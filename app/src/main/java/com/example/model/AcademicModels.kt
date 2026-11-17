package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class KisiiPortal(
    val id: String,
    val title: String,
    val shortTitle: String,
    val url: String,
    val category: String,
    val description: String,
) {
    STUDENT_PORTAL(
        id = "student_portal",
        title = "Kisii Student Portal",
        shortTitle = "Student Portal",
        url = "https://portal.kisiiuniversity.ac.ke",
        category = "Academics & Finance",
        description = "Course registration, fee statements, transcripts, and hostel booking.",
    ),
    ELEARNING(
        id = "elearning",
        title = "eLearning (ODeL)",
        shortTitle = "eLearning",
        url = "https://elearning.kisiiuniversity.ac.ke",
        category = "Virtual Classes",
        description = "Lecture notes, course modules, discussion forums, and assignments.",
    ),
    EXAM_PORTAL(
        id = "exam_portal",
        title = "Examination Portal",
        shortTitle = "Exams",
        url = "https://exams.kisiiuniversity.ac.ke",
        category = "Assessments",
        description = "Online CATs, semester examinations, exam cards, and timetables.",
    ),
    REPOSITORY(
        id = "repository",
        title = "Digital Repository",
        shortTitle = "Repository",
        url = "https://repository.kisiiuniversity.ac.ke",
        category = "Research Archive",
        description = "Past papers, theses, dissertations, and Kisii University publications.",
    ),
    HUDUMA_BORA(
        id = "huduma_bora",
        title = "Huduma Bora Platform",
        shortTitle = "Huduma Bora",
        url = "https://digital.kisiiuniversity.ac.ke",
        category = "Student Services",
        description = "Unified service desk for clearances, receipts, admissions, and support.",
    ),
    LIBRARY(
        id = "library",
        title = "University Library",
        shortTitle = "Library",
        url = "https://library.kisiiuniversity.ac.ke",
        category = "OPAC & e-Books",
        description = "Koha library catalogue, e-journals, and digital reading resources.",
    ),
}

enum class DayOfWeekOption(val label: String, val shortLabel: String) {
    MONDAY("Monday", "Mon"),
    TUESDAY("Tuesday", "Tue"),
    WEDNESDAY("Wednesday", "Wed"),
    THURSDAY("Thursday", "Thu"),
    FRIDAY("Friday", "Fri"),
    SATURDAY("Saturday", "Sat");

    companion object {
        fun fromString(raw: String?): DayOfWeekOption =
            entries.firstOrNull { it.name == raw } ?: MONDAY
    }
}

enum class LectureStatus(val label: String) {
    SCHEDULED("Scheduled"),
    RESCHEDULED("Rescheduled"),
    VENUE_CHANGED("Venue Changed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromString(raw: String?): LectureStatus =
            entries.firstOrNull { it.name == raw } ?: SCHEDULED
    }
}

enum class VerificationStatus(val label: String) {
    VERIFIED_IN_VENUE("Verified in Venue"),
    NEAR_CAMPUS("Near Campus"),
    REMOTE_LOGGED("Remote Logged");

    companion object {
        fun fromString(raw: String?): VerificationStatus =
            entries.firstOrNull { it.name == raw } ?: REMOTE_LOGGED
    }
}

data class CampusHallPreset(
    val name: String,
    val buildingCode: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Long = 120L,
)

object KisiiCampusVenues {
    const val MAIN_CAMPUS_LAT = -0.69235
    const val MAIN_CAMPUS_LNG = 34.78470

    val presets = listOf(
        CampusHallPreset("Tuition Block A - TBA 204", "TBA", -0.69235, 34.78470, 120L),
        CampusHallPreset("Science Complex - SC 102", "SC", -0.69192, 34.78512, 120L),
        CampusHallPreset("Main Library Auditorium", "MLA", -0.69268, 34.78425, 140L),
        CampusHallPreset("ICT Center - Lab 1", "ICT", -0.69210, 34.78440, 100L),
        CampusHallPreset("Business & Economics - BEH 301", "BEH", -0.69295, 34.78505, 120L),
        CampusHallPreset("Graduates Amphitheatre", "GA", -0.69170, 34.78455, 150L),
    )
}

object KisiiGradingScale {
    fun letterForScore(totalScore: Double): String = when {
        totalScore >= 70.0 -> "A"
        totalScore >= 60.0 -> "B"
        totalScore >= 50.0 -> "C"
        totalScore >= 40.0 -> "D"
        else -> "E"
    }

    fun gradePointsForLetter(letter: String): Double = when (letter) {
        "A" -> 4.0
        "B" -> 3.0
        "C" -> 2.0
        "D" -> 1.0
        else -> 0.0
    }
}

@Keep
@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val regNumber: String = "",
    val programme: String = "",
    val semesterLabel: String = "Sem 1 2026/2027",
    val campus: String = "Main Campus",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class LectureSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val courseCode: String = "",
    val courseTitle: String = "",
    val lecturer: String = "",
    val dayOfWeek: String = DayOfWeekOption.MONDAY.name,
    val startTime: String = "08:00",
    val endTime: String = "10:00",
    val venueName: String = "",
    val venueLat: Double = KisiiCampusVenues.MAIN_CAMPUS_LAT,
    val venueLng: Double = KisiiCampusVenues.MAIN_CAMPUS_LNG,
    val geofenceRadiusMeters: Long = 120L,
    val status: String = LectureStatus.SCHEDULED.name,
    val updateNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val dayOption: DayOfWeekOption get() = DayOfWeekOption.fromString(dayOfWeek)
    val lectureStatus: LectureStatus get() = LectureStatus.fromString(status)
}

@Keep
@IgnoreExtraProperties
data class CourseGrade(
    @DocumentId val id: String = "",
    val userId: String = "",
    val courseCode: String = "",
    val courseTitle: String = "",
    val semester: String = "Sem 1 2026/2027",
    val credits: Long = 3L,
    val catScore: Double = 0.0,
    val assignmentScore: Double = 0.0,
    val examScore: Double = 0.0,
    val totalScore: Double = 0.0,
    val gradeLetter: String = "A",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val lectureId: String = "",
    val courseCode: String = "",
    val courseTitle: String = "",
    val venueName: String = "",
    val recordedLat: Double = 0.0,
    val recordedLng: Double = 0.0,
    val accuracyMeters: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val verificationStatus: String = VerificationStatus.REMOTE_LOGGED.name,
    val sessionDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val statusEnum: VerificationStatus get() = VerificationStatus.fromString(verificationStatus)
}
