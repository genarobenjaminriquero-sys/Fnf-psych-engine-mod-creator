package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.ModStudioViewModel
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlaytestScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.FunkinCraftTheme
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class StudioRoute(val modId: String)

@Serializable
data class PlaytestRoute(val modId: String)

@Serializable
object GuideRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FunkinCraftTheme {
                FunkinCraftApp()
            }
        }
    }
}

@Composable
fun FunkinCraftApp(viewModel: ModStudioViewModel = viewModel()) {
    val navController = rememberNavController()
    val projects by viewModel.projects.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        composable<HomeRoute> {
            HomeScreen(
                projects = projects,
                onOpenMod = { modId ->
                    viewModel.openProject(modId)
                    navController.navigate(StudioRoute(modId))
                },
                onPlaytestMod = { modId ->
                    viewModel.openProject(modId)
                    navController.navigate(PlaytestRoute(modId))
                },
                onCreateMod = { name, author, engine ->
                    viewModel.createProject(name, author, engine) { createdId ->
                        navController.navigate(StudioRoute(createdId))
                    }
                },
                onDeleteMod = { modId ->
                    viewModel.deleteProject(modId)
                },
                onOpenGuide = {
                    navController.navigate(GuideRoute)
                }
            )
        }

        composable<StudioRoute> {
            StudioScreen(
                viewModel = viewModel,
                onBackHome = {
                    navController.popBackStack(HomeRoute, inclusive = false)
                },
                onOpenPlaytest = {
                    val id = viewModel.activeProject.value?.id ?: return@StudioScreen
                    navController.navigate(PlaytestRoute(id))
                },
                onOpenGuide = {
                    navController.navigate(GuideRoute)
                }
            )
        }

        composable<PlaytestRoute> {
            PlaytestScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<GuideRoute> {
            GuideScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
