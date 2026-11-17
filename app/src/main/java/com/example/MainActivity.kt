package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.EkisiityRepository
import com.example.location.VenueLocationManager
import com.example.model.KisiiPortal
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GradesScreen
import com.example.ui.screens.PortalWebViewScreen
import com.example.ui.screens.PortalsHubScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.theme.EkisiityTheme
import com.example.ui.viewmodel.EkisiityViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object Routes {
    const val DASHBOARD = "dashboard"
    const val PORTALS = "portals"
    const val SCHEDULE = "schedule"
    const val GRADES = "grades"
    const val ATTENDANCE = "attendance"
    const val PORTAL_VIEWER = "portal_viewer/{portalId}"

    fun portalViewer(portal: KisiiPortal): String = "portal_viewer/${portal.id}"
}

private enum class TopTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    DASHBOARD(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    PORTALS(Routes.PORTALS, R.string.nav_portals, Icons.Rounded.AccountBalance, Icons.Outlined.AccountBalance),
    SCHEDULE(Routes.SCHEDULE, R.string.nav_schedule, Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth),
    GRADES(Routes.GRADES, R.string.nav_grades, Icons.Rounded.Grade, Icons.Outlined.Grade),
    ATTENDANCE(Routes.ATTENDANCE, R.string.nav_attend, Icons.Rounded.LocationOn, Icons.Outlined.LocationOn),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EkisiityTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            EkisiityShell(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: user.email ?: "Kisii Student",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EkisiityShell(
    currentUserId: String,
    userDisplayName: String,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: EkisiityViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                EkisiityViewModel(
                    repository = EkisiityRepository(db),
                    locationManager = VenueLocationManager(app),
                    currentUserId = currentUserId,
                    defaultDisplayName = userDisplayName,
                )
            }
        }
    )

    val profileUi by viewModel.profileState.collectAsStateWithLifecycle()
    val lecturesUi by viewModel.lecturesState.collectAsStateWithLifecycle()
    val gradesUi by viewModel.gradesState.collectAsStateWithLifecycle()
    val attendanceUi by viewModel.attendanceState.collectAsStateWithLifecycle()
    val summary by viewModel.academicSummary.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isPortalViewer = currentRoute?.startsWith("portal_viewer") == true

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp && !isPortalViewer

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!isPortalViewer) {
                    CenterAlignedTopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        actions = {
                            IconButton(
                                onClick = {
                                    signOut(credentialManager = credentialManager, scope = scope)
                                },
                                modifier = Modifier.testTag("sign_out_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = stringResource(R.string.cd_sign_out),
                                )
                            }
                        },
                    )
                }
            },
            bottomBar = {
                if (!useRail && !isPortalViewer) {
                    NavigationBar {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
                                    if (tab == TopTab.SCHEDULE && summary.timetableUpdateCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(summary.timetableUpdateCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(iconVector, contentDescription = null)
                                        }
                                    } else {
                                        Icon(iconVector, contentDescription = null)
                                    }
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("nav_${tab.route}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
                                    if (tab == TopTab.SCHEDULE && summary.timetableUpdateCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(summary.timetableUpdateCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(iconVector, contentDescription = null)
                                        }
                                    } else {
                                        Icon(iconVector, contentDescription = null)
                                    }
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.DASHBOARD,
                    ) {
                        composable(Routes.DASHBOARD) {
                            DashboardScreen(
                                userDisplayName = userDisplayName,
                                profileUi = profileUi,
                                lecturesUi = lecturesUi,
                                summary = summary,
                                onOpenPortal = { portal ->
                                    navController.navigate(Routes.portalViewer(portal))
                                },
                                onNavigateToPortals = { navigateTab(Routes.PORTALS) },
                                onNavigateToSchedule = { navigateTab(Routes.SCHEDULE) },
                                onNavigateToAttendance = { navigateTab(Routes.ATTENDANCE) },
                                onNavigateToGrades = { navigateTab(Routes.GRADES) },
                                onSaveProfile = viewModel::saveStudentProfile,
                            )
                        }
                        composable(Routes.PORTALS) {
                            PortalsHubScreen(
                                onOpenPortal = { portal ->
                                    navController.navigate(Routes.portalViewer(portal))
                                }
                            )
                        }
                        composable(Routes.SCHEDULE) {
                            ScheduleScreen(
                                lecturesUi = lecturesUi,
                                locationState = locationState,
                                onRefreshLocation = viewModel::refreshLiveLocation,
                                onAddLecture = viewModel::addLecture,
                                onUpdateTimetable = viewModel::updateTimetableStatus,
                                onDeleteLecture = viewModel::deleteLecture,
                                onLoadSampleSchedule = viewModel::loadKisiiStarterSchedule,
                            )
                        }
                        composable(Routes.GRADES) {
                            GradesScreen(
                                gradesUi = gradesUi,
                                summary = summary,
                                onSaveGrade = viewModel::saveGrade,
                                onDeleteGrade = viewModel::deleteGrade,
                            )
                        }
                        composable(Routes.ATTENDANCE) {
                            AttendanceScreen(
                                lecturesUi = lecturesUi,
                                attendanceUi = attendanceUi,
                                locationState = locationState,
                                onRefreshLocation = viewModel::refreshLiveLocation,
                                onCheckInLecture = viewModel::checkInAttendanceWithLiveGps,
                                onDeleteAttendance = viewModel::deleteAttendance,
                                onGoToSchedule = { navigateTab(Routes.SCHEDULE) },
                            )
                        }
                        composable(
                            route = Routes.PORTAL_VIEWER,
                            arguments = listOf(
                                navArgument("portalId") { type = NavType.StringType }
                            ),
                        ) { backStackEntry ->
                            val portalId = backStackEntry.arguments?.getString("portalId")
                            val portal = KisiiPortal.entries.firstOrNull { it.id == portalId }
                                ?: KisiiPortal.STUDENT_PORTAL
                            PortalWebViewScreen(
                                initialPortal = portal,
                                onClose = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
