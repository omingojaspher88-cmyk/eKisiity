package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNotifications
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.model.DayOfWeekOption
import com.example.model.KisiiCampusVenues
import com.example.model.LectureSession
import com.example.model.LectureStatus
import com.example.model.UiState
import com.example.ui.viewmodel.LocationCheckState
import java.util.Locale

private enum class ScheduleFilter(val label: String) {
    ALL("All"),
    UPDATES("Updates"),
    MON("Mon"),
    TUE("Tue"),
    WED("Wed"),
    THU("Thu"),
    FRI("Fri"),
    SAT("Sat"),
}

@Composable
fun ScheduleScreen(
    lecturesUi: UiState<List<LectureSession>>,
    locationState: LocationCheckState,
    onRefreshLocation: (((LiveLocationReading) -> Unit)?) -> Unit,
    onAddLecture: (
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
    ) -> Unit,
    onUpdateTimetable: (
        lectureId: String,
        status: LectureStatus,
        venueName: String,
        venueLat: Double,
        venueLng: Double,
        startTime: String,
        endTime: String,
        updateNote: String,
    ) -> Unit,
    onDeleteLecture: (String) -> Unit,
    onLoadSampleSchedule: () -> Unit,
) {
    var selectedFilter by remember { mutableStateOf(ScheduleFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingUpdateFor by remember { mutableStateOf<LectureSession?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_lecture_fab"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add lecture")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ScheduleFilter.entries, key = { it.name }) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                    )
                }
            }

            when (lecturesUi) {
                UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = lecturesUi.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                is UiState.Success -> {
                    val filtered = remember(lecturesUi.data, selectedFilter) {
                        lecturesUi.data.filter { lec ->
                            when (selectedFilter) {
                                ScheduleFilter.ALL -> true
                                ScheduleFilter.UPDATES -> lec.lectureStatus != LectureStatus.SCHEDULED
                                ScheduleFilter.MON -> lec.dayOption == DayOfWeekOption.MONDAY
                                ScheduleFilter.TUE -> lec.dayOption == DayOfWeekOption.TUESDAY
                                ScheduleFilter.WED -> lec.dayOption == DayOfWeekOption.WEDNESDAY
                                ScheduleFilter.THU -> lec.dayOption == DayOfWeekOption.THURSDAY
                                ScheduleFilter.FRI -> lec.dayOption == DayOfWeekOption.FRIDAY
                                ScheduleFilter.SAT -> lec.dayOption == DayOfWeekOption.SATURDAY
                            }
                        }.sortedWith(compareBy({ it.dayOption.ordinal }, { it.startTime }))
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = if (lecturesUi.data.isEmpty()) "No lectures scheduled yet" else "No lectures match this filter",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Add your Kisii University units and lecture hall venues to track timetable updates and verify attendance.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (lecturesUi.data.isEmpty()) {
                                    Spacer(Modifier.height(16.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(onClick = { showAddDialog = true }) {
                                            Text("Add Lecture")
                                        }
                                        OutlinedButton(
                                            onClick = onLoadSampleSchedule,
                                            modifier = Modifier.testTag("load_sample_schedule_button"),
                                        ) {
                                            Text("Load Kisii Sample")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 88.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(filtered, key = { it.id }) { lecture ->
                                LectureCard(
                                    lecture = lecture,
                                    onPostUpdate = { editingUpdateFor = lecture },
                                    onDelete = { onDeleteLecture(lecture.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLectureDialog(
            locationState = locationState,
            onRefreshLocation = onRefreshLocation,
            onDismiss = { showAddDialog = false },
            onConfirm = { code, title, lecturer, day, start, end, venue, lat, lng, radius ->
                onAddLecture(
                    code,
                    title,
                    lecturer,
                    day,
                    start,
                    end,
                    venue,
                    lat,
                    lng,
                    radius,
                    LectureStatus.SCHEDULED,
                    "",
                )
                showAddDialog = false
            },
        )
    }

    editingUpdateFor?.let { lecture ->
        TimetableUpdateDialog(
            lecture = lecture,
            onDismiss = { editingUpdateFor = null },
            onConfirm = { status, venue, lat, lng, start, end, note ->
                onUpdateTimetable(
                    lecture.id,
                    status,
                    venue,
                    lat,
                    lng,
                    start,
                    end,
                    note,
                )
                editingUpdateFor = null
            },
        )
    }
}

@Composable
private fun LectureCard(
    lecture: LectureSession,
    onPostUpdate: () -> Unit,
    onDelete: () -> Unit,
) {
    val badgeBg = when (lecture.lectureStatus) {
        LectureStatus.SCHEDULED -> MaterialTheme.colorScheme.tertiaryContainer
        LectureStatus.VENUE_CHANGED, LectureStatus.RESCHEDULED -> MaterialTheme.colorScheme.secondaryContainer
        LectureStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
    }
    val badgeFg = when (lecture.lectureStatus) {
        LectureStatus.SCHEDULED -> MaterialTheme.colorScheme.onTertiaryContainer
        LectureStatus.VENUE_CHANGED, LectureStatus.RESCHEDULED -> MaterialTheme.colorScheme.onSecondaryContainer
        LectureStatus.CANCELLED -> MaterialTheme.colorScheme.onErrorContainer
    }

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = lecture.courseCode,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeBg,
                    ) {
                        Text(
                            text = lecture.lectureStatus.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Row {
                    IconButton(onClick = onPostUpdate) {
                        Icon(
                            imageVector = Icons.Rounded.EditNotifications,
                            contentDescription = "Update timetable status",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove lecture",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                text = lecture.courseTitle,
                style = MaterialTheme.typography.titleMedium,
            )
            if (lecture.lecturer.isNotBlank()) {
                Text(
                    text = lecture.lecturer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${lecture.dayOption.shortLabel} • ${lecture.startTime}–${lecture.endTime}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${lecture.venueName} (${lecture.geofenceRadiusMeters}m zone)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (lecture.updateNote.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBg.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = lecture.updateNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = badgeFg,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddLectureDialog(
    locationState: LocationCheckState,
    onRefreshLocation: (((LiveLocationReading) -> Unit)?) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (
        code: String,
        title: String,
        lecturer: String,
        day: DayOfWeekOption,
        start: String,
        end: String,
        venue: String,
        lat: Double,
        lng: Double,
        radius: Long,
    ) -> Unit,
) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var lecturer by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf(DayOfWeekOption.MONDAY) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var selectedPreset by remember { mutableStateOf(KisiiCampusVenues.presets.first()) }
    var venueName by remember { mutableStateOf(KisiiCampusVenues.presets.first().name) }
    var venueLat by remember { mutableStateOf(KisiiCampusVenues.presets.first().latitude) }
    var venueLng by remember { mutableStateOf(KisiiCampusVenues.presets.first().longitude) }
    var radius by remember { mutableStateOf(KisiiCampusVenues.presets.first().radiusMeters) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onRefreshLocation { reading ->
                venueLat = reading.latitude
                venueLng = reading.longitude
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Lecture Session") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course Code (e.g. COMP 314)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = lecturer,
                    onValueChange = { lecturer = it },
                    label = { Text("Lecturer Name (optional)") },
                    singleLine = true,
                )
                Text("Day of Week", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeekOption.entries.forEach { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(day.shortLabel) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text("Campus Hall Preset", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KisiiCampusVenues.presets.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = {
                                selectedPreset = preset
                                venueName = preset.name
                                venueLat = preset.latitude
                                venueLng = preset.longitude
                                radius = preset.radiusMeters
                            },
                            label = { Text(preset.buildingCode) },
                        )
                    }
                }
                OutlinedTextField(
                    value = venueName,
                    onValueChange = { venueName = it },
                    label = { Text("Lecture Hall Name") },
                    singleLine = true,
                )
                OutlinedButton(
                    onClick = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            onRefreshLocation { reading ->
                                venueLat = reading.latitude
                                venueLng = reading.longitude
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (locationState is LocationCheckState.Locating) "Capturing GPS..."
                        else "Pin My Current GPS (${String.format(Locale.US, "%.4f, %.4f", venueLat, venueLng)})"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        code,
                        title,
                        lecturer,
                        selectedDay,
                        startTime,
                        endTime,
                        venueName,
                        venueLat,
                        venueLng,
                        radius,
                    )
                },
                enabled = code.trim().length >= 2 && title.trim().length >= 2 && venueName.trim().length >= 2,
            ) {
                Text("Save Lecture")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimetableUpdateDialog(
    lecture: LectureSession,
    onDismiss: () -> Unit,
    onConfirm: (
        status: LectureStatus,
        venueName: String,
        venueLat: Double,
        venueLng: Double,
        startTime: String,
        endTime: String,
        note: String,
    ) -> Unit,
) {
    var status by remember { mutableStateOf(lecture.lectureStatus) }
    var venueName by remember { mutableStateOf(lecture.venueName) }
    var venueLat by remember { mutableStateOf(lecture.venueLat) }
    var venueLng by remember { mutableStateOf(lecture.venueLng) }
    var startTime by remember { mutableStateOf(lecture.startTime) }
    var endTime by remember { mutableStateOf(lecture.endTime) }
    var note by remember { mutableStateOf(lecture.updateNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timetable Update: ${lecture.courseCode}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Status", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LectureStatus.entries.forEach { option ->
                        FilterChip(
                            selected = status == option,
                            onClick = { status = option },
                            label = { Text(option.label) },
                        )
                    }
                }
                Text("Quick Venue Switch", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KisiiCampusVenues.presets.forEach { preset ->
                        FilterChip(
                            selected = venueName == preset.name,
                            onClick = {
                                venueName = preset.name
                                venueLat = preset.latitude
                                venueLng = preset.longitude
                            },
                            label = { Text(preset.buildingCode) },
                        )
                    }
                }
                OutlinedTextField(
                    value = venueName,
                    onValueChange = { venueName = it },
                    label = { Text("Venue") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Update Note (e.g. Moved from SC 101)") },
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(status, venueName, venueLat, venueLng, startTime, endTime, note) },
                enabled = venueName.trim().length >= 2 && startTime.trim().length >= 4 && endTime.trim().length >= 4,
            ) {
                Text("Publish Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
