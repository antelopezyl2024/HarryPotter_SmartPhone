package com.harrypotter.smartphone.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.harrypotter.smartphone.ui.screens.EndingScreen
import com.harrypotter.smartphone.ui.screens.HomeScreen
import com.harrypotter.smartphone.ui.screens.HermioneScreen
import com.harrypotter.smartphone.ui.screens.SceneScreen
import com.harrypotter.smartphone.viewmodel.GameUiState
import com.harrypotter.smartphone.viewmodel.GameViewModel
import com.harrypotter.smartphone.viewmodel.GameViewModelFactory
import com.harrypotter.smartphone.viewmodel.HermioneViewModel
import com.harrypotter.smartphone.viewmodel.HermioneViewModelFactory

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Scene : Screen("scene")
    data object Ending : Screen("ending")
    data object Hermione : Screen("hermione")
}

@Composable
fun AppNavigation(playerUuid: String) {
    val navController: NavHostController = rememberNavController()
    val gameVm: GameViewModel = viewModel(factory = GameViewModelFactory(playerUuid))
    val hermioneVm: HermioneViewModel = viewModel(factory = HermioneViewModelFactory(playerUuid))
    val state by gameVm.state.collectAsStateWithLifecycle()

    // Drive navigation from state changes
    LaunchedEffect(state) {
        when (state) {
            is GameUiState.InScene, is GameUiState.Feedback -> {
                val current = navController.currentBackStackEntry?.destination?.route
                if (current == Screen.Home.route || current == null) {
                    navController.navigate(Screen.Scene.route) { launchSingleTop = true }
                }
            }
            is GameUiState.Ended -> {
                navController.navigate(Screen.Ending.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
            is GameUiState.DLCSelection -> {
                val current = navController.currentBackStackEntry?.destination?.route
                if (current != Screen.Home.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            }
            else -> Unit
        }
    }

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                state = state,
                onSelectDLC = { dlcId -> gameVm.startGame(dlcId) },
                onRetry = { gameVm.restart() }
            )
        }

        composable(Screen.Scene.route) {
            SceneScreen(
                state = state,
                onSubmitChoice = { playthroughId, sceneId, choiceId, scene ->
                    gameVm.submitChoice(playthroughId, sceneId, choiceId, scene)
                },
                onSubmitFreeText = { playthroughId, sceneId, text, scene ->
                    gameVm.submitFreeText(playthroughId, sceneId, text, scene)
                },
                onProceed = { resp, playthroughId ->
                    gameVm.proceedToNextScene(resp, playthroughId)
                },
                onHermioneClick = { navController.navigate(Screen.Hermione.route) },
                onGameEnd = {},
                onRetry = { gameVm.restart() }
            )
        }

        composable(Screen.Ending.route) {
            val endedState = state as? GameUiState.Ended
            if (endedState != null) {
                EndingScreen(
                    ending = endedState.ending,
                    onPlayAgain = { gameVm.restart() }
                )
            }
        }

        composable(Screen.Hermione.route) {
            val currentPlaythroughId = when (val s = state) {
                is GameUiState.InScene -> s.playthrough.playthroughId
                is GameUiState.Feedback -> s.playthrough.playthroughId
                else -> null
            }
            HermioneScreen(
                viewModel = hermioneVm,
                playthroughId = currentPlaythroughId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
