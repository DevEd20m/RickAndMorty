package com.example.rickandmorty.core.sdui.usecase

import com.example.rickandmorty.core.sdui.model.ScreenConfig
import com.example.rickandmorty.core.sdui.repository.ScreenConfigRepository
import javax.inject.Inject

class GetScreenConfigUseCase @Inject constructor(
    private val repository: ScreenConfigRepository
) {
    suspend operator fun invoke(): ScreenConfig = repository.getScreenConfig()
}
