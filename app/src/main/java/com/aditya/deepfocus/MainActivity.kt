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
import com.aditya.deepfocus.ui.screens.FocusScreen
import com.aditya.deepfocus.ui.screens.HomeScreen
import com.aditya.deepfocus.ui.theme.DeepFocusTheme
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
    object Home : Screen("home")
    object Focus : Screen("focus/{youtubeUrl}/{durationMinutes}") {
        fun createRoute(youtubeUrl: String, durationMinutes: Int): String {
            val encoded = URLEncoder.encode(youtubeUrl, StandardCharsets.UTF_8.toString())
            return "focus/$encoded/$durationMinutes"
        }
    }
}

@Composable
fun DeepFocusNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(onStartSession = { url, minutes ->
                navController.navigate(Screen.Focus.createRoute(url, minutes))
            })
        }
        composable(
            route = Screen.Focus.route,
            arguments = listOf(
                navArgument("youtubeUrl") { type = NavType.StringType },
                navArgument("durationMinutes") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("youtubeUrl") ?: ""
            val youtubeUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
            val durationMinutes = backStackEntry.arguments?.getInt("durationMinutes") ?: 25
            FocusScreen(youtubeUrl = youtubeUrl, durationMinutes = durationMinutes, onSessionComplete = { navController.popBackStack() })
        }
    }
}
