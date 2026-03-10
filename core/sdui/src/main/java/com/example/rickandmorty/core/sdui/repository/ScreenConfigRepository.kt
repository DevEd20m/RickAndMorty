package com.example.rickandmorty.core.sdui.repository

import com.example.rickandmorty.core.sdui.model.ScreenConfig

interface ScreenConfigRepository {
    suspend fun getScreenConfig(): ScreenConfig
}
