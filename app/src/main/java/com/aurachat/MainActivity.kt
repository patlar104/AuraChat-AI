package com.aurachat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aurachat.navigation.ChatRoute
import com.aurachat.navigation.HomeRoute
import com.aurachat.navigation.SettingsRoute
import com.aurachat.presentation.chat.ChatScreen
import com.aurachat.presentation.history.DrawerContent
import com.aurachat.presentation.home.HomeScreen
import com.aurachat.presentation.settings.SettingsScreen
import com.aurachat.ui.theme.AuraChatTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The app's single activity.
 *
 * Hosts the Compose navigation graph within a [ModalNavigationDrawer]. The drawer
 * shows the conversation history ([DrawerContent]); the main content area is a
 * [NavHost] with routes for home, chat, and settings.
 *
 * All screen-to-screen navigations use slide + fade transitions for a polished feel.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraChatTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            DrawerContent(
                                onNavigateToSession = { sessionId ->
                                    scope.launch {
                                        drawerState.close()
                                        navController.navigate(ChatRoute(sessionId)) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                            )
                        }
                    },
                ) {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        scope.launch { drawerState.open() }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = stringResource(R.string.cd_open_drawer),
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = {
                                        navController.navigate(SettingsRoute) {
                                            launchSingleTop = true
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = stringResource(R.string.cd_open_settings),
                                            tint = MaterialTheme.colorScheme.onBackground,
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                                ),
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.background,
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = HomeRoute,
                            modifier = Modifier.padding(innerPadding),
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { it }) + fadeIn()
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                            },
                        ) {
                            composable<HomeRoute> {
                                HomeScreen(
                                    onNavigateToChat = { sessionId ->
                                        navController.navigate(ChatRoute(sessionId)) {
                                            launchSingleTop = true
                                        }
                                    },
                                )
                            }
                            composable<ChatRoute> {
                                // sessionId is automatically injected into ChatViewModel
                                // via SavedStateHandle by Hilt + Navigation Compose
                                ChatScreen()
                            }
                            composable<SettingsRoute> {
                                SettingsScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}
