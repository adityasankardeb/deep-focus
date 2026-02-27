package com.aditya.deepfocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aditya.deepfocus.ui.screens.*
import com.aditya.deepfocus.ui.theme.DeepFocusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeepFocusTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DeepFocusNavGraph()
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object History : Screen("history")
    object Focus : Screen("focus/{videoId}/{startSeconds}/{endSeconds}") {
        fun createRoute(videoId: String, startSeconds: Int, endSeconds: Int) =
            "focus/$videoId/$startSeconds/$endSeconds"
    }
}

@Composable
fun DeepFocusNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(onSplashComplete = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onStartSession = { videoId, start, end ->
                    navController.navigate(Screen.Focus.createRoute(videoId, start, end))
                },
                onViewHistory = { navController.navigate(Screen.History.route) }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Focus.route,
            arguments = listOf(
                navArgument("videoId") { type = NavType.StringType },
                navArgument("startSeconds") { type = NavType.IntType },
                navArgument("endSeconds") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            val startSeconds = backStackEntry.arguments?.getInt("startSeconds") ?: 0
            val endSeconds = backStackEntry.arguments?.getInt("endSeconds") ?: 0
            FocusScreen(
                videoId = videoId,
                startSeconds = startSeconds,
                endSeconds = endSeconds,
                onSessionComplete = { navController.popBackStack() }
            )
        }
    }
}
