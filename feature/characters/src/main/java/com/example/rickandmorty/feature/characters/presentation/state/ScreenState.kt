package com.example.rickandmorty.feature.characters.presentation.state

import com.example.rickandmorty.core.sdui.model.ScreenConfig

data class ScreenState(
    val isRefreshing: Boolean,
    val screenConfig: ScreenConfig,
    val isDemoLoading: Boolean,
    val isDemoError: Boolean
) {
    companion object {
        fun default(): ScreenState = ScreenState(
            isRefreshing = false,
            screenConfig = ScreenConfigDefaults.screenConfig(),
            isDemoLoading = false,
            isDemoError = false
        )
    }
}
