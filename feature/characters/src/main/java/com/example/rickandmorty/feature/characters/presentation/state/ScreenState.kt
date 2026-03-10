package com.example.rickandmorty.feature.characters.presentation.state

import com.example.rickandmorty.core.sdui.model.ScreenConfig

data class ScreenState(
    val uiState: CharacterUiState,
    val isRefreshing: Boolean,
    val screenConfig: ScreenConfig
) {
    companion object {
        fun default(): ScreenState = ScreenState(
            uiState = CharacterUiState.Loading,
            isRefreshing = false,
            screenConfig = ScreenConfigDefaults.screenConfig()
        )
    }
}
