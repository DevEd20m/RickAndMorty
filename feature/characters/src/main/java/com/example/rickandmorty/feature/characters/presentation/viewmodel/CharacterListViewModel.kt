package com.example.rickandmorty.feature.characters.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.rickandmorty.core.presentation.BaseViewModel
import com.example.rickandmorty.core.sdui.model.ScreenConfig
import com.example.rickandmorty.core.sdui.usecase.GetScreenConfigUseCase
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.usecase.GetCharactersUseCase
import com.example.rickandmorty.feature.characters.presentation.state.ScreenConfigDefaults
import com.example.rickandmorty.feature.characters.presentation.state.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
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

    val characters: Flow<PagingData<Character>> = getCharactersUseCase().cachedIn(viewModelScope)

    private val _isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _screenConfig: MutableStateFlow<ScreenConfig> = MutableStateFlow(ScreenConfigDefaults.screenConfig())
    private val _isDemoLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _isDemoError: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val screenState: StateFlow<ScreenState> = combine(
        _isRefreshing,
        _screenConfig,
        _isDemoLoading,
        _isDemoError
    ) { isRefreshing, screenConfig, isDemoLoading, isDemoError ->
        ScreenState(
            isRefreshing = isRefreshing,
            screenConfig = screenConfig,
            isDemoLoading = isDemoLoading,
            isDemoError = isDemoError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScreenState.default()
    )

    init {
        viewModelScope.launch {
            supervisorScope {
                launch { loadScreenConfig() }
            }
        }
    }

    fun simulateLoading() {
        _isDemoLoading.value = true
        _isDemoError.value = false
    }

    fun simulateError() {
        _isDemoError.value = true
        _isDemoLoading.value = false
    }

    fun restoreRealData() {
        _isDemoLoading.value = false
        _isDemoError.value = false
    }

    private fun loadScreenConfig() {
        launchWithErrorHandling {
            val config: ScreenConfig = getScreenConfigUseCase()
            _screenConfig.value = config
        }
    }
}
