package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.CalibrationScreen
import com.example.ui.screens.GesturesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrackingScreen
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EyeCyan
import com.example.ui.theme.EyeEmerald
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.EyeGestureViewModel

object NavRoutes {
    const val HOME = "home"
    const val TRACK = "track"
    const val GESTURES = "gestures"
    const val APPS = "apps"
    const val PROFILES = "profiles"
    const val HISTORY = "history"
    const val CALIBRATION = "calibration"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                EyeGestureApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EyeGestureApp(
    viewModel: EyeGestureViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoutes.HOME

    val activeProfile by viewModel.activeProfile.collectAsState()
    val isGestureLocked by viewModel.isGestureLocked.collectAsState()
    val trackingState by viewModel.liveTrackingState.collectAsState()

    val bottomNavItems = listOf(
        BottomNavItem("Home", NavRoutes.HOME, Icons.Default.Home, "nav_home"),
        BottomNavItem("Track", NavRoutes.TRACK, Icons.Default.Visibility, "nav_track"),
        BottomNavItem("Gestures", NavRoutes.GESTURES, Icons.Default.Bolt, "nav_gestures"),
        BottomNavItem("Apps", NavRoutes.APPS, Icons.Default.Apps, "nav_apps"),
        BottomNavItem("Settings", NavRoutes.SETTINGS, Icons.Default.Settings, "nav_settings")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("app_scaffold"),
        topBar = {
            if (currentRoute != NavRoutes.CALIBRATION) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (isGestureLocked) Color(0xFFFF5252)
                                        else if (trackingState.isFaceDetected) EyeEmerald
                                        else Color.Gray,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EyeGesture",
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp,
                                color = Color.White
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.navigate(NavRoutes.PROFILES) },
                            modifier = Modifier.testTag("top_profiles_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profiles",
                                tint = if (currentRoute == NavRoutes.PROFILES) EyeEmerald else EyeCyan
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { navController.navigate(NavRoutes.HISTORY) },
                            modifier = Modifier.testTag("top_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = if (currentRoute == NavRoutes.HISTORY) EyeEmerald else Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DarkSurface
                    )
                )
            }
        },
        bottomBar = {
            if (currentRoute != NavRoutes.CALIBRATION) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = EyeEmerald,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = EyeEmerald,
                                indicatorColor = EyeEmerald,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(NavRoutes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTrack = { navController.navigate(NavRoutes.TRACK) },
                    onNavigateToCalibration = { navController.navigate(NavRoutes.CALIBRATION) },
                    onNavigateToGestures = { navController.navigate(NavRoutes.GESTURES) },
                    onNavigateToApps = { navController.navigate(NavRoutes.APPS) },
                    onNavigateToProfiles = { navController.navigate(NavRoutes.PROFILES) }
                )
            }

            composable(NavRoutes.TRACK) {
                TrackingScreen(
                    viewModel = viewModel,
                    onNavigateToCalibration = { navController.navigate(NavRoutes.CALIBRATION) }
                )
            }

            composable(NavRoutes.GESTURES) {
                GesturesScreen(
                    viewModel = viewModel,
                    onNavigateToApps = { navController.navigate(NavRoutes.APPS) }
                )
            }

            composable(NavRoutes.APPS) {
                AppsScreen(
                    viewModel = viewModel
                )
            }

            composable(NavRoutes.PROFILES) {
                ProfilesScreen(
                    viewModel = viewModel
                )
            }

            composable(NavRoutes.HISTORY) {
                HistoryScreen(
                    viewModel = viewModel
                )
            }

            composable(NavRoutes.CALIBRATION) {
                CalibrationScreen(
                    viewModel = viewModel,
                    onFinish = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToCalibration = { navController.navigate(NavRoutes.CALIBRATION) }
                )
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)
