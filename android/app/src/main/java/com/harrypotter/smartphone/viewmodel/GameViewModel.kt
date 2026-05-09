package com.harrypotter.smartphone.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrypotter.smartphone.data.model.*
import com.harrypotter.smartphone.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GameUiState {
    data object Loading : GameUiState()
    data class DLCSelection(val dlcs: List<DLCSummary>) : GameUiState()
    data class InScene(val playthrough: Playthrough, val scene: Scene) : GameUiState()
    data class Feedback(
        val playthrough: Playthrough,
        val response: SubmitDecisionResponse,
        val prevScene: Scene
    ) : GameUiState()
    data class Ended(val ending: GetEndingResponse) : GameUiState()
    data class Error(val message: String) : GameUiState()
}

class GameViewModel(
    private val playerUuid: String,
    private val repo: GameRepository = GameRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var currentDlcId: String = ""

    init { loadDLCs() }

    private fun loadDLCs() {
        viewModelScope.launch {
            _state.value = GameUiState.Loading
            runCatching { repo.listDLCs() }
                .onSuccess { _state.value = GameUiState.DLCSelection(it.dlcs) }
                .onFailure { _state.value = GameUiState.Error(it.message ?: "Failed to load DLCs") }
        }
    }

    fun startGame(dlcId: String) {
        viewModelScope.launch {
            currentDlcId = dlcId
            _state.value = GameUiState.Loading
            runCatching { repo.startPlaythrough(playerUuid, dlcId) }
                .onSuccess { _state.value = GameUiState.InScene(it.playthrough, it.scene) }
                .onFailure { _state.value = GameUiState.Error(it.message ?: "Failed to start game") }
        }
    }

    fun submitChoice(playthroughId: String, sceneId: String, choiceId: String, currentScene: Scene) {
        viewModelScope.launch {
            _state.value = GameUiState.Loading
            runCatching { repo.submitChoice(playthroughId, sceneId, choiceId) }
                .onSuccess { resp ->
                    if (resp.playthroughStatus == "COMPLETED") {
                        loadEnding(playthroughId)
                    } else {
                        _state.value = GameUiState.Feedback(
                            playthrough = Playthrough(
                                playthroughId = playthroughId,
                                playerUuid = playerUuid,
                                dlcId = currentDlcId,
                                currentSceneId = resp.nextScene?.sceneId,
                                totalScore = resp.totalScore,
                                status = resp.playthroughStatus
                            ),
                            response = resp, prevScene = currentScene
                        )
                    }
                }
                .onFailure { _state.value = GameUiState.Error(it.message ?: "Submission failed") }
        }
    }

    fun submitFreeText(playthroughId: String, sceneId: String, text: String, currentScene: Scene) {
        viewModelScope.launch {
            _state.value = GameUiState.Loading
            runCatching { repo.submitFreeText(playthroughId, sceneId, text) }
                .onSuccess { resp ->
                    if (resp.playthroughStatus == "COMPLETED") {
                        loadEnding(playthroughId)
                    } else {
                        _state.value = GameUiState.Feedback(
                            playthrough = Playthrough(
                                playthroughId = playthroughId,
                                playerUuid = playerUuid,
                                dlcId = currentDlcId,
                                currentSceneId = resp.nextScene?.sceneId,
                                totalScore = resp.totalScore,
                                status = resp.playthroughStatus
                            ),
                            response = resp, prevScene = currentScene
                        )
                    }
                }
                .onFailure { _state.value = GameUiState.Error(it.message ?: "Submission failed") }
        }
    }

    fun proceedToNextScene(response: SubmitDecisionResponse, playthroughId: String) {
        val next = response.nextScene
        if (next == null) {
            loadEnding(playthroughId)
        } else {
            _state.value = GameUiState.InScene(
                Playthrough(
                    playthroughId = playthroughId,
                    playerUuid = playerUuid,
                    dlcId = currentDlcId,
                    currentSceneId = next.sceneId,
                    totalScore = response.totalScore,
                    status = "IN_PROGRESS"
                ),
                next
            )
        }
    }

    private fun loadEnding(playthroughId: String) {
        viewModelScope.launch {
            runCatching { repo.getEnding(playthroughId) }
                .onSuccess { _state.value = GameUiState.Ended(it) }
                .onFailure { _state.value = GameUiState.Error(it.message ?: "Failed to load ending") }
        }
    }

    fun restart() { loadDLCs() }
}
