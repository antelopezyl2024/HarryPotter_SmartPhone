package com.harrypotter.smartphone.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrypotter.smartphone.data.model.HermioneSource
import com.harrypotter.smartphone.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HermioneMessage(
    val text: String,
    val isUser: Boolean,
    val sources: List<HermioneSource> = emptyList()
)

data class HermioneUiState(
    val messages: List<HermioneMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HermioneViewModel(
    private val playerUuid: String,
    private val repo: GameRepository = GameRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(HermioneUiState())
    val state: StateFlow<HermioneUiState> = _state.asStateFlow()

    fun sendQuery(query: String, playthroughId: String?) {
        if (query.isBlank()) return
        _state.value = _state.value.copy(
            messages = _state.value.messages + HermioneMessage(query, isUser = true),
            isLoading = true,
            error = null
        )
        viewModelScope.launch {
            runCatching { repo.hermioneQuery(playerUuid, query, playthroughId) }
                .onSuccess { resp ->
                    _state.value = _state.value.copy(
                        messages = _state.value.messages + HermioneMessage(
                            resp.answer, isUser = false, sources = resp.sources
                        ),
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Hermione couldn't answer that"
                    )
                }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
