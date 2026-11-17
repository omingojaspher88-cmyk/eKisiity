package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.location.LiveLocationReading
import com.example.location.VenueLocationManager
import com.example.model.AttendanceRecord
import com.example.model.LectureSession
import com.example.model.UiState
import com.example.model.VerificationStatus
import com.example.ui.viewmodel.LocationCheckState
import java.util.Locale

@Composable
fun AttendanceScreen(
    lecturesUi: UiState<List<LectureSession>>,
    attendanceUi: UiState<List<AttendanceRecord>>,
    locationState: LocationCheckState,
    onRefreshLocation: (((LiveLocationReading) -> Unit)?) -> Unit,
    onCheckInLecture: (LectureSession) -> Unit,
    onDeleteAttendance: (String) -> Unit,
    onGoToSchedule: () -> Unit,
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            onRefreshLocation(null)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && locationState is LocationCheckState.Idle) {
            onRefreshLocation(null)
        }
    }

    val lectures = (lecturesUi as? UiState.Success)?.data.orEmpty()
    var selectedLectureId by remember(lectures) {
        mutableStateOf(lectures.firstOrNull()?.id)
    }
    val selectedLecture = lectures.firstOrNull { it.id == selectedLectureId } ?: lectures.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LiveVenueCheckInCard(
                hasPermission = hasLocationPermission,
                locationState = locationState,
                lectures = lectures,
                selectedLecture = selectedLecture,
                onSelectLecture = { selectedLectureId = it.id },
                onRequestPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                },
                onRefreshGps = { onRefreshLocation(null) },
                onCheckIn = {
                    if (!hasLocationPermission) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )
                    } else if (selectedLecture != null) {
                        onCheckInLecture(selectedLecture)
                    }
                },
                onGoToSchedule = onGoToSchedule,
            )
        }

        item {
            Text(
                text = "Attendance Verification Log",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        when (attendanceUi) {
            UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = attendanceUi.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            is UiState.Success -> {
                val sorted = attendanceUi.data.sortedByDescending { it.createdAt?.seconds ?: 0L }
                if (sorted.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "No attendance check-ins recorded yet",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "Select a scheduled lecture above and tap Record Live Attendance when in the lecture hall.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    items(sorted, key = { it.id }) { record ->
                        AttendanceLogCard(
                            record = record,
                            onDelete = { onDeleteAttendance(record.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveVenueCheckInCard(
    hasPermission: Boolean,
    locationState: LocationCheckState,
    lectures: List<LectureSession>,
    selectedLecture: LectureSession?,
    onSelectLecture: (LectureSession) -> Unit,
    onRequestPermission: () -> Unit,
    onRefreshGps: () -> Unit,
    onCheckIn: () -> Unit,
    onGoToSchedule: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.GpsFixed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Live Venue GPS Verification",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(Modifier.height(8.dp))

            if (!hasPermission) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Location access needed to verify lecture hall presence",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "eKisiity checks your live GPS coordinates against the lecture hall radius only when you check in.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.testTag("grant_location_button"),
                        ) {
                            Text("Enable Location Access")
                        }
                    }
                }
            } else {
                when (locationState) {
                    LocationCheckState.Idle, LocationCheckState.Locating -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Acquiring high-accuracy GPS fix...",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    is LocationCheckState.Error -> {
                        Text(
                            text = locationState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    is LocationCheckState.Fixed -> {
                        val r = locationState.reading
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = "GPS: ${String.format(Locale.US, "%.5f, %.5f", r.latitude, r.longitude)}",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    Text(
                                        text = "Accuracy ±${r.accuracyMeters.toInt()}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                OutlinedButton(onClick = onRefreshGps) {
                                    Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Refresh")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            if (lectures.isEmpty()) {
                OutlinedButton(
                    onClick = onGoToSchedule,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add a Lecture in Schedule First")
                }
            } else {
                Text(
                    text = "Select Lecture Session",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lectures, key = { it.id }) { lecture ->
                        FilterChip(
                            selected = selectedLecture?.id == lecture.id,
                            onClick = { onSelectLecture(lecture) },
                            label = { Text("${lecture.courseCode} • ${lecture.dayOption.shortLabel}") },
                        )
                    }
                }

                if (selectedLecture != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "${selectedLecture.courseTitle} @ ${selectedLecture.venueName}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    val fixed = (locationState as? LocationCheckState.Fixed)?.reading
                    if (fixed != null) {
                        val distance = VenueLocationManager.distanceBetweenMeters(
                            fixed.latitude,
                            fixed.longitude,
                            selectedLecture.venueLat,
                            selectedLecture.venueLng,
                        )
                        val status = VenueLocationManager.classifyVenueProximity(
                            distance,
                            selectedLecture.geofenceRadiusMeters,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Distance to hall: ${distance.toInt()}m (allowed ≤${selectedLecture.geofenceRadiusMeters}m) → ${status.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status == VerificationStatus.VERIFIED_IN_VENUE) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onCheckIn,
                        enabled = locationState !is LocationCheckState.Locating,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("record_attendance_button"),
                    ) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Record Live Venue Attendance")
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceLogCard(
    record: AttendanceRecord,
    onDelete: () -> Unit,
) {
    val status = record.statusEnum
    val statusColor = when (status) {
        VerificationStatus.VERIFIED_IN_VENUE -> MaterialTheme.colorScheme.tertiary
        VerificationStatus.NEAR_CAMPUS -> MaterialTheme.colorScheme.secondary
        VerificationStatus.REMOTE_LOGGED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (status == VerificationStatus.VERIFIED_IN_VENUE) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Rounded.WarningAmber
                },
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${record.courseCode} • ${record.sessionDate}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = record.venueName,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${status.label} • ${record.distanceMeters.toInt()}m away (±${record.accuracyMeters.toInt()}m GPS)",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete attendance record",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
