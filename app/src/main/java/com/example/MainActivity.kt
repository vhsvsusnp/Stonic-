package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.example.voice.StonicWakeWordService

object StonicRoutes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val NOTES = "notes"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {

    private val viewModel: StonicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleWakeIntent(intent)

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
                    StonicAppNavigation(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWakeIntent(intent)
    }

    private fun handleWakeIntent(intent: Intent?) {
        if (intent?.hasExtra(StonicWakeWordService.EXTRA_WAKE_PROMPT) == true) {
            val prompt = intent.getStringExtra(StonicWakeWordService.EXTRA_WAKE_PROMPT).orEmpty()
            if (prompt.isNotBlank()) {
                viewModel.submitQuery(prompt, isVoice = true)
            } else {
                viewModel.submitQuery("Stonic", isVoice = true)
            }
        }
    }
}

@Composable
fun StonicAppNavigation(viewModel: StonicViewModel) {
    val navController = rememberNavController()

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
