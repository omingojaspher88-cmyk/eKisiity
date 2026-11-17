package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EkisiityRepository
import com.example.location.LiveLocationReading
import com.example.location.VenueLocationManager
import com.example.model.AttendanceRecord
import com.example.model.CourseGrade
import com.example.model.DayOfWeekOption
import com.example.model.KisiiCampusVenues
import com.example.model.KisiiGradingScale
import com.example.model.LectureSession
import com.example.model.LectureStatus
import com.example.model.StudentProfile
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AcademicSummary(
    val cumulativeGpa: Double = 0.0,
    val averagePercentage: Double = 0.0,
    val totalCredits: Long = 0L,
    val unitCount: Int = 0,
    val timetableUpdateCount: Int = 0,
    val verifiedAttendanceCount: Int = 0,
    val totalAttendanceCount: Int = 0,
)

sealed interface LocationCheckState {
    data object Idle : LocationCheckState
    data object Locating : LocationCheckState
    data class Fixed(val reading: LiveLocationReading) : LocationCheckState
    data class Error(val message: String) : LocationCheckState
}

class EkisiityViewModel(
    private val repository: EkisiityRepository,
    private val locationManager: VenueLocationManager,
    private val currentUserId: String,
    private val defaultDisplayName: String,
) : ViewModel() {

    private fun <T> Flow<T>.toUiStateFlow(
        tagName: String,
        fallbackError: String,
    ): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "$tagName stream error", rootError)
                emit(UiState.Error(rootError.message ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val profileState: StateFlow<UiState<StudentProfile?>> =
        repository.observeStudentProfile(currentUserId)
            .toUiStateFlow("Profile", "Failed to load student profile")

    val lecturesState: StateFlow<UiState<List<LectureSession>>> =
        repository.observeLectures(currentUserId)
            .toUiStateFlow("Lectures", "Failed to load timetable")

    val gradesState: StateFlow<UiState<List<CourseGrade>>> =
        repository.observeGrades(currentUserId)
            .toUiStateFlow("Grades", "Failed to load course grades")

    val attendanceState: StateFlow<UiState<List<AttendanceRecord>>> =
        repository.observeAttendance(currentUserId)
            .toUiStateFlow("Attendance", "Failed to load attendance records")

    val academicSummary: StateFlow<AcademicSummary> = combine(
        lecturesState,
        gradesState,
        attendanceState,
    ) { lecUi, gradeUi, attUi ->
        val lectures = (lecUi as? UiState.Success)?.data.orEmpty()
        val grades = (gradeUi as? UiState.Success)?.data.orEmpty()
        val attendance = (attUi as? UiState.Success)?.data.orEmpty()

        val totalCredits = grades.sumOf { it.credits }
        val weightedPoints = grades.sumOf {
            KisiiGradingScale.gradePointsForLetter(it.gradeLetter) * it.credits.toDouble()
        }
        val gpa = if (totalCredits > 0L) weightedPoints / totalCredits.toDouble() else 0.0
        val avgPct = if (grades.isNotEmpty()) grades.map { it.totalScore }.average() else 0.0
        val updatesCount = lectures.count { it.lectureStatus != LectureStatus.SCHEDULED }
        val verifiedCount = attendance.count { it.verificationStatus == "VERIFIED_IN_VENUE" }

        AcademicSummary(
            cumulativeGpa = gpa,
            averagePercentage = avgPct,
            totalCredits = totalCredits,
            unitCount = grades.size,
            timetableUpdateCount = updatesCount,
            verifiedAttendanceCount = verifiedCount,
            totalAttendanceCount = attendance.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AcademicSummary(),
    )

    private val _locationState = MutableStateFlow<LocationCheckState>(LocationCheckState.Idle)
    val locationState: StateFlow<LocationCheckState> = _locationState.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun saveStudentProfile(
        displayName: String,
        regNumber: String,
        programme: String,
        semesterLabel: String,
        campus: String,
    ) {
        viewModelScope.launch {
            val result = repository.saveStudentProfile(
                displayName = displayName.ifBlank { defaultDisplayName },
                regNumber = regNumber,
                programme = programme,
                semesterLabel = semesterLabel,
                campus = campus,
            )
            _feedbackMessage.value = if (result.isSuccess) {
                "Student profile synced to cloud"
            } else {
                result.exceptionOrNull()?.message ?: "Could not save profile"
            }
        }
    }

    fun addLecture(
        courseCode: String,
        courseTitle: String,
        lecturer: String,
        dayOfWeek: DayOfWeekOption,
        startTime: String,
        endTime: String,
        venueName: String,
        venueLat: Double,
        venueLng: Double,
        geofenceRadiusMeters: Long,
        status: LectureStatus,
        updateNote: String,
    ) {
        viewModelScope.launch {
            val result = repository.addLecture(
                LectureSession(
                    userId = currentUserId,
                    courseCode = courseCode,
                    courseTitle = courseTitle,
                    lecturer = lecturer,
                    dayOfWeek = dayOfWeek.name,
                    startTime = startTime,
                    endTime = endTime,
                    venueName = venueName,
                    venueLat = venueLat,
                    venueLng = venueLng,
                    geofenceRadiusMeters = geofenceRadiusMeters,
                    status = status.name,
                    updateNote = updateNote,
                )
            )
            _feedbackMessage.value = if (result.isSuccess) {
                "Lecture added to schedule"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to add lecture"
            }
        }
    }

    fun updateTimetableStatus(
        lectureId: String,
        status: LectureStatus,
        venueName: String,
        venueLat: Double,
        venueLng: Double,
        startTime: String,
        endTime: String,
        updateNote: String,
    ) {
        viewModelScope.launch {
            val result = repository.updateTimetableStatus(
                lectureId = lectureId,
                status = status,
                venueName = venueName,
                venueLat = venueLat,
                venueLng = venueLng,
                startTime = startTime,
                endTime = endTime,
                updateNote = updateNote,
            )
            _feedbackMessage.value = if (result.isSuccess) {
                "Timetable update published"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to update timetable"
            }
        }
    }

    fun deleteLecture(lectureId: String) {
        viewModelScope.launch {
            val result = repository.deleteLecture(lectureId)
            if (result.isFailure) {
                _feedbackMessage.value = result.exceptionOrNull()?.message ?: "Failed to delete lecture"
            }
        }
    }

    fun saveGrade(
        existingId: String?,
        courseCode: String,
        courseTitle: String,
        semester: String,
        credits: Long,
        catScore: Double,
        assignmentScore: Double,
        examScore: Double,
    ) {
        viewModelScope.launch {
            val total = (catScore + assignmentScore + examScore).coerceIn(0.0, 100.0)
            val result = repository.saveGrade(
                grade = CourseGrade(
                    userId = currentUserId,
                    courseCode = courseCode,
                    courseTitle = courseTitle,
                    semester = semester,
                    credits = credits,
                    catScore = catScore,
                    assignmentScore = assignmentScore,
                    examScore = examScore,
                    totalScore = total,
                    gradeLetter = KisiiGradingScale.letterForScore(total),
                ),
                existingId = existingId,
            )
            _feedbackMessage.value = if (result.isSuccess) {
                "Grade saved (${KisiiGradingScale.letterForScore(total)})"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to save grade"
            }
        }
    }

    fun deleteGrade(gradeId: String) {
        viewModelScope.launch {
            val result = repository.deleteGrade(gradeId)
            if (result.isFailure) {
                _feedbackMessage.value = result.exceptionOrNull()?.message ?: "Failed to delete grade"
            }
        }
    }

    fun refreshLiveLocation(onSuccess: ((LiveLocationReading) -> Unit)? = null) {
        viewModelScope.launch {
            _locationState.value = LocationCheckState.Locating
            val result = locationManager.fetchCurrentLocation()
            if (result.isSuccess) {
                val reading = result.getOrThrow()
                _locationState.value = LocationCheckState.Fixed(reading)
                onSuccess?.invoke(reading)
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Could not retrieve GPS location"
                _locationState.value = LocationCheckState.Error(msg)
                _feedbackMessage.value = msg
            }
        }
    }

    fun checkInAttendanceWithLiveGps(lecture: LectureSession) {
        viewModelScope.launch {
            val currentReading = (_locationState.value as? LocationCheckState.Fixed)?.reading
                ?: run {
                    _locationState.value = LocationCheckState.Locating
                    val fetched = locationManager.fetchCurrentLocation()
                    if (fetched.isFailure) {
                        val err = fetched.exceptionOrNull()?.message ?: "Location fix unavailable"
                        _locationState.value = LocationCheckState.Error(err)
                        _feedbackMessage.value = err
                        return@launch
                    }
                    val reading = fetched.getOrThrow()
                    _locationState.value = LocationCheckState.Fixed(reading)
                    reading
                }

            val distance = VenueLocationManager.distanceBetweenMeters(
                currentReading.latitude,
                currentReading.longitude,
                lecture.venueLat,
                lecture.venueLng,
            )
            val status = VenueLocationManager.classifyVenueProximity(
                distanceMeters = distance,
                geofenceRadiusMeters = lecture.geofenceRadiusMeters,
            )
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val result = repository.recordAttendance(
                AttendanceRecord(
                    userId = currentUserId,
                    lectureId = lecture.id,
                    courseCode = lecture.courseCode,
                    courseTitle = lecture.courseTitle,
                    venueName = lecture.venueName,
                    recordedLat = currentReading.latitude,
                    recordedLng = currentReading.longitude,
                    accuracyMeters = currentReading.accuracyMeters,
                    distanceMeters = distance,
                    verificationStatus = status.name,
                    sessionDate = dateStr,
                )
            )

            _feedbackMessage.value = if (result.isSuccess) {
                "${lecture.courseCode} logged: ${status.label} (${distance.toInt()}m from venue)"
            } else {
                result.exceptionOrNull()?.message ?: "Could not save attendance record"
            }
        }
    }

    fun deleteAttendance(attendanceId: String) {
        viewModelScope.launch {
            val result = repository.deleteAttendance(attendanceId)
            if (result.isFailure) {
                _feedbackMessage.value = result.exceptionOrNull()?.message ?: "Failed to remove record"
            }
        }
    }

    fun loadKisiiStarterSchedule() {
        viewModelScope.launch {
            val tba = KisiiCampusVenues.presets[0]
            val sc = KisiiCampusVenues.presets[1]
            val ict = KisiiCampusVenues.presets[3]
            val mla = KisiiCampusVenues.presets[2]

            val starters = listOf(
                LectureSession(
                    userId = currentUserId,
                    courseCode = "COMP 314",
                    courseTitle = "Distributed Systems",
                    lecturer = "Dr. M. Nyabuto",
                    dayOfWeek = DayOfWeekOption.MONDAY.name,
                    startTime = "08:00",
                    endTime = "10:00",
                    venueName = tba.name,
                    venueLat = tba.latitude,
                    venueLng = tba.longitude,
                    geofenceRadiusMeters = tba.radiusMeters,
                    status = LectureStatus.VENUE_CHANGED.name,
                    updateNote = "Shifted from SC 101 to TBA 204 for projector maintenance",
                ),
                LectureSession(
                    userId = currentUserId,
                    courseCode = "COMP 321",
                    courseTitle = "Compiler Construction",
                    lecturer = "Prof. K. Moraa",
                    dayOfWeek = DayOfWeekOption.TUESDAY.name,
                    startTime = "11:00",
                    endTime = "13:00",
                    venueName = ict.name,
                    venueLat = ict.latitude,
                    venueLng = ict.longitude,
                    geofenceRadiusMeters = ict.radiusMeters,
                    status = LectureStatus.SCHEDULED.name,
                    updateNote = "",
                ),
                LectureSession(
                    userId = currentUserId,
                    courseCode = "SOEN 306",
                    courseTitle = "Software Quality Assurance",
                    lecturer = "Dr. D. Ochieng",
                    dayOfWeek = DayOfWeekOption.WEDNESDAY.name,
                    startTime = "14:00",
                    endTime = "16:00",
                    venueName = sc.name,
                    venueLat = sc.latitude,
                    venueLng = sc.longitude,
                    geofenceRadiusMeters = sc.radiusMeters,
                    status = LectureStatus.RESCHEDULED.name,
                    updateNote = "Moved from 08:00 morning slot to 14:00 afternoon",
                ),
                LectureSession(
                    userId = currentUserId,
                    courseCode = "MATH 212",
                    courseTitle = "Probability & Statistics II",
                    lecturer = "Mr. E. Mogaka",
                    dayOfWeek = DayOfWeekOption.THURSDAY.name,
                    startTime = "09:00",
                    endTime = "11:00",
                    venueName = mla.name,
                    venueLat = mla.latitude,
                    venueLng = mla.longitude,
                    geofenceRadiusMeters = mla.radiusMeters,
                    status = LectureStatus.SCHEDULED.name,
                    updateNote = "",
                ),
            )
            var successCount = 0
            starters.forEach { if (repository.addLecture(it).isSuccess) successCount++ }
            _feedbackMessage.value = "Loaded $successCount Kisii University sample lectures"
        }
    }

    private companion object {
        const val TAG = "EkisiityVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
