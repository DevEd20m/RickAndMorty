package com.example.rickandmorty.core.sdui.repository

import com.example.rickandmorty.core.sdui.dto.ScreenConfigDto
import com.example.rickandmorty.core.sdui.mapper.toDomain
import com.example.rickandmorty.core.sdui.model.BannerConfig
import com.example.rickandmorty.core.sdui.model.CardConfig
import com.example.rickandmorty.core.sdui.model.DemoConfig
import com.example.rickandmorty.core.sdui.model.ErrorViewConfig
import com.example.rickandmorty.core.sdui.model.ImageShape
import com.example.rickandmorty.core.sdui.model.ListConfig
import com.example.rickandmorty.core.sdui.model.ScreenConfig
import com.example.rickandmorty.core.sdui.model.StatusLabels
import com.example.rickandmorty.core.sdui.model.TopBarConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ScreenConfigRepositoryImpl @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val json: Json
) : ScreenConfigRepository {

    companion object {
        private const val TAG = "ScreenConfigRepo"
        private const val KEY_SCREEN_CONFIG = "screen_config"
    }

    override suspend fun getScreenConfig(): ScreenConfig {
        return runCatching {
            remoteConfig.fetchAndActivate().await()
            val rawJson: String = remoteConfig.getString(KEY_SCREEN_CONFIG)
            json.decodeFromString<ScreenConfigDto>(rawJson).toDomain()
        }.getOrElse {
            fallbackConfig()
        }
    }

    private fun fallbackConfig(): ScreenConfig = ScreenConfig(
        topBar = TopBarConfig(title = "Rick & Morty", visible = true),
        banner = BannerConfig(visible = false, text = "", backgroundColor = "#6200EE", textColor = "#FFFFFF"),
        card = CardConfig(
            imageShape = ImageShape.CIRCLE,
            showStatusChip = true,
            statusLabels = StatusLabels(alive = "Alive", dead = "Dead", unknown = "Unknown"),
            elevationDp = 4
        ),
        errorView = ErrorViewConfig(title = "Algo salió mal", retryLabel = "Reintentar"),
        list = ListConfig(skeletonCount = 6, animationDurationMs = 300),
        demo = DemoConfig(
            title = "Demo de estados",
            subtitle = "Fuerza un estado de UI para verificar cada escenario.",
            loadingLabel = "Simular carga",
            loadingDescription = "Muestra el skeleton animado",
            restoreLabel = "Restaurar datos",
            restoreDescription = "Recarga desde la página 1",
            errorLabel = "Simular error",
            errorDescription = "Muestra la vista de error con Reintentar"
        )
    )
}
