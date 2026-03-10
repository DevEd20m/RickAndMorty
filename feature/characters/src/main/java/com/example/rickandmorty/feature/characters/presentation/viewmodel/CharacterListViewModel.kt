package com.example.rickandmorty.feature.characters.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.core.presentation.BaseViewModel
import com.example.rickandmorty.core.sdui.model.ScreenConfig
import com.example.rickandmorty.core.sdui.usecase.GetScreenConfigUseCase
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.usecase.GetCharactersUseCase
import com.example.rickandmorty.feature.characters.presentation.state.CharacterUiState
import com.example.rickandmorty.feature.characters.presentation.state.ScreenState
import com.example.rickandmorty.feature.characters.presentation.state.ScreenConfigDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class CharacterListViewModel @Inject constructor(
    private val getCharactersUseCase: GetCharactersUseCase,
    private val getScreenConfigUseCase: GetScreenConfigUseCase
) : BaseViewModel() {

    private val _uiState: MutableStateFlow<CharacterUiState> = MutableStateFlow(CharacterUiState.Loading)
    private val _isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _screenConfig = MutableStateFlow(ScreenConfigDefaults.screenConfig())

    private var currentPage: Int = 1
    private var hasNextPage: Boolean = false

    val screenState: StateFlow<ScreenState> = combine(
        _uiState,
        _isRefreshing,
        _screenConfig
    ) { uiState, isRefreshing, screenConfig ->
        ScreenState(uiState, isRefreshing, screenConfig)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScreenState.default()
    )

    init {
        viewModelScope.launch {
            supervisorScope {
                launch { loadCharacters() }
                launch { loadScreenConfig() }
            }
        }
    }

    fun retry() {
        currentPage = 1
        hasNextPage = false
        loadCharacters()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            currentPage = 1
            fetchAndUpdateCharacters(page = 1, append = false)
            _isRefreshing.value = false
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (!hasNextPage || current is CharacterUiState.LoadingMore) return

        viewModelScope.launch {
            if (current is CharacterUiState.Success) {
                _uiState.value = CharacterUiState.LoadingMore(current.characters)
            }
            fetchAndUpdateCharacters(page = currentPage + 1, append = true)
        }
    }

    fun simulateLoading() {
        _uiState.value = CharacterUiState.Loading
    }

    fun simulateError() {
        _uiState.value = CharacterUiState.Error("Simulated error for demo")
    }

    fun restoreRealData() {
        currentPage = 1
        hasNextPage = false
        loadCharacters()
    }

    private fun loadCharacters() {
        launchWithErrorHandling(
            onError = { throwable ->
                _uiState.value = CharacterUiState.Error(throwable.message ?: "An unexpected error occurred")
            }
        ) {
            _uiState.value = CharacterUiState.Loading
            fetchAndUpdateCharacters(page = 1, append = false)
        }
    }

    private fun loadScreenConfig() {
        launchWithErrorHandling {
            val config: ScreenConfig = getScreenConfigUseCase()
            _screenConfig.value = config
        }
    }

    private suspend fun fetchAndUpdateCharacters(page: Int, append: Boolean) {
        val result: Result<Pair<List<Character>, String?>> = getCharactersUseCase(page)
        result.fold(
            onSuccess = { (characters, nextPage) ->
                hasNextPage = nextPage != null
                currentPage = page

                val existingList: ImmutableList<Character> =
                    if (append && _uiState.value is CharacterUiState.LoadingMore) {
                        (_uiState.value as CharacterUiState.LoadingMore).characters
                    } else {
                        persistentListOf()
                    }

                _uiState.value = CharacterUiState.Success(
                    characters = (existingList + characters).toImmutableList(),
                    hasNextPage = hasNextPage
                )
            },
            onFailure = { throwable ->
                val current: CharacterUiState = _uiState.value
                if (append && current is CharacterUiState.LoadingMore) {
                    _uiState.value = CharacterUiState.Success(
                        characters = current.characters,
                        hasNextPage = hasNextPage
                    )
                } else {
                    _uiState.value = CharacterUiState.Error(
                        throwable.message ?: "An unexpected error occurred"
                    )
                }
            }
        )
    }
}
