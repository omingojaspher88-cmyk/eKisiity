package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AttendanceRecord
import com.example.model.CourseGrade
import com.example.model.DayOfWeekOption
import com.example.model.KisiiGradingScale
import com.example.model.LectureSession
import com.example.model.LectureStatus
import com.example.model.StudentProfile
import com.example.model.VerificationStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class EkisiityRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.logFirestoreErrors(
        uid: String,
        operationType: OperationType,
        path: String,
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // --- STUDENT PROFILE ---

    fun observeStudentProfile(userId: String): Flow<StudentProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            StudentProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .logFirestoreErrors(uid, OperationType.GET, docRef.path)
        )
    }

    suspend fun saveStudentProfile(
        displayName: String,
        regNumber: String,
        programme: String,
        semesterLabel: String,
        campus: String,
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(80)
            val cleanReg = regNumber.trim().take(30)
            val cleanProg = programme.trim().take(80)
            val cleanSem = semesterLabel.trim().ifBlank { "Sem 1 2026/2027" }.take(40)
            val cleanCampus = campus.trim().ifBlank { "Main Campus" }.take(60)
            require(cleanName.isNotEmpty()) { "Display name cannot be empty" }

            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "regNumber" to cleanReg,
                        "programme" to cleanProg,
                        "semesterLabel" to cleanSem,
                        "campus" to cleanCampus,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "regNumber" to cleanReg,
                        "programme" to cleanProg,
                        "semesterLabel" to cleanSem,
                        "campus" to cleanCampus,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- LECTURES & TIMETABLE UPDATES ---

    fun observeLectures(userId: String): Flow<List<LectureSession>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val colRef = db.collection("users").document(userId).collection("lectures")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LectureSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(uid, OperationType.LIST, colRef.path)
        )
    }

    suspend fun addLecture(lecture: LectureSession): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/lectures"
        return try {
            val uid = requireUserId()
            val courseCode = lecture.courseCode.trim().uppercase().take(20)
            val courseTitle = lecture.courseTitle.trim().take(100)
            val lecturer = lecture.lecturer.trim().take(80)
            val startTime = lecture.startTime.trim().take(10)
            val endTime = lecture.endTime.trim().take(10)
            val venueName = lecture.venueName.trim().take(80)
            val updateNote = lecture.updateNote.trim().take(200)
            val radius = lecture.geofenceRadiusMeters.coerceIn(20L, 500L)

            require(courseCode.length >= 2) { "Course code must be at least 2 characters" }
            require(courseTitle.length >= 2) { "Course title must be at least 2 characters" }
            require(venueName.length >= 2) { "Venue name must be at least 2 characters" }
            require(startTime.length >= 4 && endTime.length >= 4) { "Invalid lecture time format" }

            val dayStr = DayOfWeekOption.fromString(lecture.dayOfWeek).name
            val statusStr = LectureStatus.fromString(lecture.status).name

            val docRef = db.collection("users").document(uid).collection("lectures").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "courseCode" to courseCode,
                "courseTitle" to courseTitle,
                "lecturer" to lecturer,
                "dayOfWeek" to dayStr,
                "startTime" to startTime,
                "endTime" to endTime,
                "venueName" to venueName,
                "venueLat" to lecture.venueLat.coerceIn(-90.0, 90.0),
                "venueLng" to lecture.venueLng.coerceIn(-180.0, 180.0),
                "geofenceRadiusMeters" to radius,
                "status" to statusStr,
                "updateNote" to updateNote,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTimetableStatus(
        lectureId: String,
        status: LectureStatus,
        venueName: String,
        venueLat: Double,
        venueLng: Double,
        startTime: String,
        endTime: String,
        updateNote: String,
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/lectures/$lectureId"
        return try {
            val uid = requireUserId()
            require(lectureId.isNotBlank()) { "Lecture ID cannot be blank" }
            val cleanVenue = venueName.trim().take(80)
            val cleanStart = startTime.trim().take(10)
            val cleanEnd = endTime.trim().take(10)
            val cleanNote = updateNote.trim().take(200)
            require(cleanVenue.length >= 2) { "Venue name must be at least 2 characters" }
            require(cleanStart.length >= 4 && cleanEnd.length >= 4) { "Time cannot be blank" }

            val docRef = db.collection("users").document(uid).collection("lectures").document(lectureId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to status.name,
                    "venueName" to cleanVenue,
                    "venueLat" to venueLat.coerceIn(-90.0, 90.0),
                    "venueLng" to venueLng.coerceIn(-180.0, 180.0),
                    "startTime" to cleanStart,
                    "endTime" to cleanEnd,
                    "updateNote" to cleanNote,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteLecture(lectureId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/lectures/$lectureId"
        return try {
            val uid = requireUserId()
            require(lectureId.isNotBlank()) { "Lecture ID cannot be blank" }
            val docRef = db.collection("users").document(uid).collection("lectures").document(lectureId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- GRADE TRACKING ---

    fun observeGrades(userId: String): Flow<List<CourseGrade>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val colRef = db.collection("users").document(userId).collection("grades")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CourseGrade::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(uid, OperationType.LIST, colRef.path)
        )
    }

    suspend fun saveGrade(grade: CourseGrade, existingId: String? = null): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/grades"
        return try {
            val uid = requireUserId()
            val code = grade.courseCode.trim().uppercase().take(20)
            val title = grade.courseTitle.trim().take(100)
            val semester = grade.semester.trim().ifBlank { "Sem 1 2026/2027" }.take(40)
            val credits = grade.credits.coerceIn(1L, 12L)
            val cat = grade.catScore.coerceIn(0.0, 30.0)
            val assign = grade.assignmentScore.coerceIn(0.0, 20.0)
            val exam = grade.examScore.coerceIn(0.0, 70.0)
            val total = (cat + assign + exam).coerceIn(0.0, 100.0)
            val letter = KisiiGradingScale.letterForScore(total)

            require(code.length >= 2) { "Course code must be at least 2 characters" }
            require(title.length >= 2) { "Course title must be at least 2 characters" }

            val colRef = db.collection("users").document(uid).collection("grades")
            if (existingId.isNullOrBlank()) {
                val docRef = colRef.document()
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "courseCode" to code,
                        "courseTitle" to title,
                        "semester" to semester,
                        "credits" to credits,
                        "catScore" to cat,
                        "assignmentScore" to assign,
                        "examScore" to exam,
                        "totalScore" to total,
                        "gradeLetter" to letter,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                val docRef = colRef.document(existingId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "courseCode" to code,
                        "courseTitle" to title,
                        "semester" to semester,
                        "credits" to credits,
                        "catScore" to cat,
                        "assignmentScore" to assign,
                        "examScore" to exam,
                        "totalScore" to total,
                        "gradeLetter" to letter,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteGrade(gradeId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/grades/$gradeId"
        return try {
            val uid = requireUserId()
            require(gradeId.isNotBlank()) { "Grade ID cannot be blank" }
            val docRef = db.collection("users").document(uid).collection("grades").document(gradeId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- LIVE VENUE ATTENDANCE ---

    fun observeAttendance(userId: String): Flow<List<AttendanceRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val colRef = db.collection("users").document(userId).collection("attendance")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AttendanceRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(uid, OperationType.LIST, colRef.path)
        )
    }

    suspend fun recordAttendance(record: AttendanceRecord): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/attendance"
        return try {
            val uid = requireUserId()
            require(record.lectureId.isNotBlank()) { "Lecture ID cannot be blank" }
            val code = record.courseCode.trim().uppercase().take(20)
            val title = record.courseTitle.trim().take(100)
            val venue = record.venueName.trim().take(80)
            val sessionDate = record.sessionDate.trim().take(20)
            require(code.length >= 2 && title.length >= 2 && venue.length >= 2) {
                "Invalid attendance lecture details"
            }
            require(sessionDate.length >= 6) { "Session date invalid" }

            val status = VerificationStatus.fromString(record.verificationStatus).name
            val docRef = db.collection("users").document(uid).collection("attendance").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "lectureId" to record.lectureId.take(128),
                    "courseCode" to code,
                    "courseTitle" to title,
                    "venueName" to venue,
                    "recordedLat" to record.recordedLat.coerceIn(-90.0, 90.0),
                    "recordedLng" to record.recordedLng.coerceIn(-180.0, 180.0),
                    "accuracyMeters" to record.accuracyMeters.coerceIn(0.0, 5000.0),
                    "distanceMeters" to record.distanceMeters.coerceIn(0.0, 5000000.0),
                    "verificationStatus" to status,
                    "sessionDate" to sessionDate,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAttendance(attendanceId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/attendance/$attendanceId"
        return try {
            val uid = requireUserId()
            require(attendanceId.isNotBlank()) { "Attendance ID cannot be blank" }
            val docRef = db.collection("users").document(uid).collection("attendance").document(attendanceId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
