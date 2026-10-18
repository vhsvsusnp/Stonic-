package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JarvisToolsScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StonicBackground
import com.example.ui.viewmodel.StonicViewModel

object StonicRoutes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val NOTES = "notes"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding(),
                    containerColor = StonicBackground
                ) { _ ->
                    StonicAppNavigation()
                }
            }
        }
    }
}

@Composable
fun StonicAppNavigation() {
    val navController = rememberNavController()
    val viewModel: StonicViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = StonicRoutes.HOME
    ) {
        composable(StonicRoutes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToChat = { navController.navigate(StonicRoutes.CHAT) },
                onNavigateToNotes = { navController.navigate(StonicRoutes.NOTES) },
                onNavigateToTools = { navController.navigate(StonicRoutes.TOOLS) },
                onNavigateToSettings = { navController.navigate(StonicRoutes.SETTINGS) }
            )
        }

        composable(StonicRoutes.CHAT) {
            ChatScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(StonicRoutes.NOTES) {
            NotesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(StonicRoutes.TOOLS) {
            JarvisToolsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(StonicRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
