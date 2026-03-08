package com.aurachat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aurachat.presentation.chat.ChatScreen
import com.aurachat.presentation.history.DrawerContent
import com.aurachat.presentation.home.HomeScreen
import com.aurachat.ui.theme.AuraChatTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// Route constants — will be moved to a dedicated NavRoutes file in a later phase
object NavRoutes {
    const val HOME = "home"
    const val CHAT = "chat/{sessionId}"
    const val SETTINGS = "settings"

    fun chat(sessionId: Long) = "chat/$sessionId"
}

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
                                        navController.navigate(NavRoutes.chat(sessionId))
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
                                        text = "AuraChat",
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        scope.launch { drawerState.open() }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Open navigation drawer",
                                        )
                                    }
                                },
                                actions = {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                    ) {
                                        IconButton(onClick = {}) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "User avatar",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                        }
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
                            startDestination = NavRoutes.HOME,
                            modifier = Modifier.padding(innerPadding),
                        ) {
                            composable(NavRoutes.HOME) {
                                HomeScreen(
                                    onNavigateToChat = { sessionId ->
                                        navController.navigate(NavRoutes.chat(sessionId))
                                    },
                                )
                            }
                            composable(
                                route = NavRoutes.CHAT,
                                arguments = listOf(
                                    navArgument("sessionId") { type = NavType.LongType }
                                ),
                            ) {
                                // sessionId is automatically injected into ChatViewModel
                                // via SavedStateHandle by Hilt + Navigation Compose
                                ChatScreen()
                            }
                            composable(NavRoutes.SETTINGS) { SettingsPlaceholder() }
                        }
                    }
                }
            }
        }
    }
}

// ── Phase placeholders — replaced when each feature phase is implemented ──────

@Composable
private fun SettingsPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Settings — Phase 7", color = androidx.compose.ui.graphics.Color.White)
    }
}
