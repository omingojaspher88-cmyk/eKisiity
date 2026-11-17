package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.KisiiPortal
import com.example.model.LectureSession
import com.example.model.LectureStatus
import com.example.model.StudentProfile
import com.example.model.UiState
import com.example.ui.theme.Gold500
import com.example.ui.theme.Sapphire950
import com.example.ui.viewmodel.AcademicSummary
import java.util.Locale

@Composable
fun DashboardScreen(
    userDisplayName: String,
    profileUi: UiState<StudentProfile?>,
    lecturesUi: UiState<List<LectureSession>>,
    summary: AcademicSummary,
    onOpenPortal: (KisiiPortal) -> Unit,
    onNavigateToPortals: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onSaveProfile: (String, String, String, String, String) -> Unit,
) {
    val profile = (profileUi as? UiState.Success)?.data
    val lectures = (lecturesUi as? UiState.Success)?.data.orEmpty()
    val sortedLectures = remember(lectures) {
        lectures.sortedWith(compareBy({ it.dayOption.ordinal }, { it.startTime }))
    }
    val nextLecture = sortedLectures.firstOrNull { it.lectureStatus != LectureStatus.CANCELLED }
    val timetableUpdates = remember(lectures) {
        lectures.filter { it.lectureStatus != LectureStatus.SCHEDULED }
    }
    var showProfileDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val isWide = maxWidth >= 640.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 980.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Campus Hero Banner
            item {
                CampusHeroCard(
                    userDisplayName = userDisplayName,
                    profile = profile,
                    onEditProfile = { showProfileDialog = true },
                )
            }

            // 2. Central Material 3 Navigation Hub (Portals, Grade Tracking, Timetable Scheduler)
            item {
                Text(
                    text = "Academic Command Center",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(10.dp))

                if (isWide) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CentralPortalsHubCard(
                            onNavigateToPortals = onNavigateToPortals,
                            onOpenPortal = onOpenPortal,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CentralGradesNavCard(
                                summary = summary,
                                onClick = onNavigateToGrades,
                                modifier = Modifier.weight(1f),
                            )
                            CentralTimetableNavCard(
                                lectureCount = lectures.size,
                                updateCount = summary.timetableUpdateCount,
                                nextLecture = nextLecture,
                                onClick = onNavigateToSchedule,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CentralPortalsHubCard(
                            onNavigateToPortals = onNavigateToPortals,
                            onOpenPortal = onOpenPortal,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CentralGradesNavCard(
                                summary = summary,
                                onClick = onNavigateToGrades,
                                modifier = Modifier.weight(1f),
                            )
                            CentralTimetableNavCard(
                                lectureCount = lectures.size,
                                updateCount = summary.timetableUpdateCount,
                                nextLecture = nextLecture,
                                onClick = onNavigateToSchedule,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // 3. Quick Launch Strip for All 6 Kisii Portals
            item {
                SectionHeader(
                    title = "Instant Portal Shortcuts",
                    actionLabel = "All 6 Portals",
                    onAction = onNavigateToPortals,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(KisiiPortal.entries, key = { it.id }) { portal ->
                        PortalQuickCard(
                            portal = portal,
                            icon = portalIcon(portal),
                            onClick = { onOpenPortal(portal) },
                        )
                    }
                }
            }

            // 4. Timetable Updates Ticker
            item {
                SectionHeader(
                    title = "Timetable Updates",
                    actionLabel = "Open Scheduler",
                    onAction = onNavigateToSchedule,
                )
                Spacer(Modifier.height(8.dp))
                if (timetableUpdates.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToSchedule),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "All lectures on normal schedule",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "Tap to view your weekly timetable or post a venue change.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        timetableUpdates.take(3).forEach { lecture ->
                            TimetableUpdateBanner(lecture = lecture, onClick = onNavigateToSchedule)
                        }
                    }
                }
            }

            // 5. Live Location Venue Attendance Action Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToAttendance),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live Venue Attendance",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "${summary.verifiedAttendanceCount} verified check-ins • Tap to verify live GPS in hall.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = onNavigateToAttendance,
                            modifier = Modifier.testTag("dashboard_checkin_button"),
                        ) {
                            Text("Check In")
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        EditProfileDialog(
            initialName = profile?.displayName?.ifBlank { userDisplayName } ?: userDisplayName,
            initialReg = profile?.regNumber.orEmpty(),
            initialProgramme = profile?.programme.orEmpty(),
            initialSemester = profile?.semesterLabel ?: "Sem 1 2026/2027",
            initialCampus = profile?.campus ?: "Main Campus",
            onDismiss = { showProfileDialog = false },
            onConfirm = { name, reg, prog, sem, campus ->
                onSaveProfile(name, reg, prog, sem, campus)
                showProfileDialog = false
            },
        )
    }
}

@Composable
private fun CampusHeroCard(
    userDisplayName: String,
    profile: StudentProfile?,
    onEditProfile: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(204.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_campus_hero_1794942497368),
                contentDescription = stringResource(R.string.cd_campus_hero),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Sapphire950.copy(alpha = 0.25f),
                                Sapphire950.copy(alpha = 0.92f),
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Gold500,
                    ) {
                        Text(
                            text = profile?.semesterLabel ?: "Sem 1 2026/2027",
                            style = MaterialTheme.typography.labelMedium,
                            color = Sapphire950,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                    IconButton(
                        onClick = onEditProfile,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .testTag("edit_profile_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit student profile",
                            tint = Color.White,
                        )
                    }
                }
                Column {
                    Text(
                        text = "Karibu, ${profile?.displayName?.ifBlank { userDisplayName } ?: userDisplayName}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append(profile?.programme?.ifBlank { "Kisii University Student" } ?: "Kisii University Student")
                            if (!profile?.regNumber.isNullOrBlank()) {
                                append(" • ${profile?.regNumber}")
                            }
                            append(" • ${profile?.campus ?: "Main Campus"}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CentralPortalsHubCard(
    onNavigateToPortals: () -> Unit,
    onOpenPortal: (KisiiPortal) -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToPortals)
            .testTag("dashboard_nav_portals"),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(46.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "University Portals Hub",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Student Portal, eLearning, Exams, Repository & Huduma Bora",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Open University Portals Hub",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    KisiiPortal.STUDENT_PORTAL,
                    KisiiPortal.ELEARNING,
                    KisiiPortal.EXAM_PORTAL,
                    KisiiPortal.REPOSITORY,
                    KisiiPortal.HUDUMA_BORA,
                ).forEach { portal ->
                    AssistChip(
                        onClick = { onOpenPortal(portal) },
                        label = {
                            Text(
                                text = portal.shortTitle,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = portalIcon(portal),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CentralGradesNavCard(
    summary: AcademicSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("dashboard_nav_grades"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Grade,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Open Grade Tracking",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Grade Tracking",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = String.format(Locale.US, "%.2f GPA", summary.cumulativeGpa),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${summary.unitCount} units • ${String.format(Locale.US, "%.0f", summary.averagePercentage)}% avg",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (summary.cumulativeGpa / 4.0).toFloat().coerceIn(0f, 1f) },
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
private fun CentralTimetableNavCard(
    lectureCount: Int,
    updateCount: Int,
    nextLecture: LectureSession?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("dashboard_nav_schedule"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Open Timetable Scheduler",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Timetable",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$lectureCount Lectures",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (nextLecture != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${nextLecture.courseCode} • ${nextLecture.dayOption.shortLabel} ${nextLecture.startTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text(
                    text = "Plan lectures & alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (updateCount > 0) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Text(
                    text = if (updateCount > 0) "$updateCount active updates" else "On schedule",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        TextButton(onClick = onAction) {
            Text(actionLabel)
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun PortalQuickCard(
    portal: KisiiPortal,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .width(164.dp)
            .clickable(onClick = onClick)
            .testTag("quick_portal_${portal.id}"),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = portal.shortTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = portal.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TimetableUpdateBanner(
    lecture: LectureSession,
    onClick: () -> Unit,
) {
    val containerColor = when (lecture.lectureStatus) {
        LectureStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
        LectureStatus.VENUE_CHANGED, LectureStatus.RESCHEDULED -> MaterialTheme.colorScheme.secondaryContainer
        LectureStatus.SCHEDULED -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${lecture.courseCode} • ${lecture.lectureStatus.label.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${lecture.dayOption.shortLabel} ${lecture.startTime}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${lecture.courseTitle} — ${lecture.venueName}",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (lecture.updateNote.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = lecture.updateNote,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialReg: String,
    initialProgramme: String,
    initialSemester: String,
    initialCampus: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var reg by remember { mutableStateOf(initialReg) }
    var programme by remember { mutableStateOf(initialProgramme) }
    var semester by remember { mutableStateOf(initialSemester) }
    var campus by remember { mutableStateOf(initialCampus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Student Academic Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = reg,
                    onValueChange = { reg = it },
                    label = { Text("Registration No. (e.g. IN13/00142/23)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = programme,
                    onValueChange = { programme = it },
                    label = { Text("Programme (e.g. BSc. Computer Science)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = semester,
                    onValueChange = { semester = it },
                    label = { Text("Active Semester") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = campus,
                    onValueChange = { campus = it },
                    label = { Text("Campus") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, reg, programme, semester, campus) },
                enabled = name.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

fun portalIcon(portal: KisiiPortal): ImageVector = when (portal) {
    KisiiPortal.STUDENT_PORTAL -> Icons.Rounded.AccountBalance
    KisiiPortal.ELEARNING -> Icons.Rounded.School
    KisiiPortal.EXAM_PORTAL -> Icons.AutoMirrored.Rounded.Assignment
    KisiiPortal.REPOSITORY -> Icons.Rounded.AutoStories
    KisiiPortal.HUDUMA_BORA -> Icons.Rounded.SupportAgent
    KisiiPortal.LIBRARY -> Icons.Rounded.Verified
}
