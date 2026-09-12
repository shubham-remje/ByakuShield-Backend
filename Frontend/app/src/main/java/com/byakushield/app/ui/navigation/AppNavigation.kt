package com.byakushield.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.byakushield.app.data.security.TokenManager
import com.byakushield.app.ui.screens.AuditHistoryScreen
import com.byakushield.app.ui.screens.DashboardScreen
import com.byakushield.app.ui.screens.DataScrubScreen
import com.byakushield.app.ui.screens.LoginScreen
import com.byakushield.app.ui.screens.QrScannerScreen
import com.byakushield.app.ui.screens.RegisterScreen
import com.byakushield.app.ui.screens.ScanScreen
import com.byakushield.app.ui.screens.TextArmorScreen
import com.byakushield.app.ui.screens.VoiceShieldScreen

object Routes {

    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val SCAN = "scan"
    const val HISTORY = "history"

    const val TEXT_ARMOR = "text_armor"
    const val QUISH_GUARD = "quish_guard"
    const val VOICE_SHIELD = "voice_shield"
    const val DATA_SCRUB = "data_scrub"
}

@Composable
fun AppNavigation(
    tokenManager: TokenManager,
    startDestination: String
) {
    val navController = rememberNavController()

    val navBackStackEntry by
    navController.currentBackStackEntryAsState()

    val currentRoute =
        navBackStackEntry?.destination?.route

    val showBottomBar =
        currentRoute == Routes.HOME ||
                currentRoute == Routes.SCAN ||
                currentRoute == Routes.HISTORY

    Scaffold(
        bottomBar = {

            if (showBottomBar) {

                NavigationBar {

                    NavigationBarItem(
                        selected =
                            currentRoute == Routes.HOME,
                        onClick = {

                            navController.navigate(
                                Routes.HOME
                            ) {

                                popUpTo(Routes.HOME) {
                                    inclusive = false
                                }

                                launchSingleTop = true
                            }
                        },
                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Default.Home,
                                contentDescription =
                                    "Home"
                            )
                        },
                        label = {
                            Text("Home")
                        }
                    )

                    NavigationBarItem(
                        selected =
                            currentRoute == Routes.SCAN,
                        onClick = {

                            navController.navigate(
                                Routes.SCAN
                            ) {
                                launchSingleTop = true
                            }
                        },
                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Default.Security,
                                contentDescription =
                                    "Scan"
                            )
                        },
                        label = {
                            Text("Scan")
                        }
                    )

                    NavigationBarItem(
                        selected =
                            currentRoute == Routes.HISTORY,
                        onClick = {

                            navController.navigate(
                                Routes.HISTORY
                            ) {
                                launchSingleTop = true
                            }
                        },
                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Default.History,
                                contentDescription =
                                    "History"
                            )
                        },
                        label = {
                            Text("History")
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {

                composable(Routes.LOGIN) {

                    LoginScreen(
                        tokenManager = tokenManager,
                        onLoginSuccess = {

                            navController.navigate(
                                Routes.HOME
                            ) {

                                popUpTo(Routes.LOGIN) {
                                    inclusive = true
                                }
                            }
                        },
                        onRegisterClick = {

                            navController.navigate(
                                Routes.REGISTER
                            )
                        }
                    )
                }

                composable(Routes.REGISTER) {

                    RegisterScreen(
                        tokenManager = tokenManager,
                        onRegisterSuccess = {

                            navController.navigate(
                                Routes.LOGIN
                            ) {

                                popUpTo(Routes.REGISTER) {
                                    inclusive = true
                                }
                            }
                        },
                        onBackToLogin = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Routes.HOME) {

                    DashboardScreen(
                        tokenManager = tokenManager,
                        onLogout = {

                            tokenManager.clearToken()

                            navController.navigate(
                                Routes.LOGIN
                            ) {

                                popUpTo(Routes.HOME) {
                                    inclusive = true
                                }

                                launchSingleTop = true
                            }
                        },
                        onStartScan = {

                            navController.navigate(
                                Routes.SCAN
                            )
                        }
                    )
                }

                composable(Routes.SCAN) {

                    ScanScreen(
                        onTextArmorClick = {

                            navController.navigate(
                                Routes.TEXT_ARMOR
                            )
                        },
                        onQuishGuardClick = {

                            navController.navigate(
                                Routes.QUISH_GUARD
                            )
                        },
                        onVoiceShieldClick = {

                            navController.navigate(
                                Routes.VOICE_SHIELD
                            )
                        },
                        onDataScrubClick = {

                            navController.navigate(
                                Routes.DATA_SCRUB
                            )
                        }
                    )
                }

                composable(Routes.HISTORY) {

                    AuditHistoryScreen(
                        tokenManager = tokenManager
                    )
                }

                composable(Routes.TEXT_ARMOR) {

                    TextArmorScreen(
                        tokenManager = tokenManager,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Routes.QUISH_GUARD) {

                    QrScannerScreen(
                        tokenManager = tokenManager,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Routes.VOICE_SHIELD) {

                    VoiceShieldScreen(
                        tokenManager = tokenManager,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Routes.DATA_SCRUB) {

                    DataScrubScreen(
                        tokenManager = tokenManager,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}