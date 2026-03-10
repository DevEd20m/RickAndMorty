package com.example.rickandmorty.core.sdui.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScreenConfigDto(
    @SerialName("topBar") val topBar: TopBarConfigDto = TopBarConfigDto(),
    @SerialName("banner") val banner: BannerConfigDto = BannerConfigDto(),
    @SerialName("card") val card: CardConfigDto = CardConfigDto(),
    @SerialName("errorView") val errorView: ErrorViewConfigDto = ErrorViewConfigDto(),
    @SerialName("list") val list: ListConfigDto = ListConfigDto(),
    @SerialName("demo") val demo: DemoConfigDto = DemoConfigDto()
)

@Serializable
data class TopBarConfigDto(
    @SerialName("title") val title: String = "Rick & Morty",
    @SerialName("visible") val visible: Boolean = true
)

@Serializable
data class BannerConfigDto(
    @SerialName("visible") val visible: Boolean = false,
    @SerialName("text") val text: String = "",
    @SerialName("backgroundColor") val backgroundColor: String = "#6200EE",
    @SerialName("textColor") val textColor: String = "#FFFFFF"
)

@Serializable
data class CardConfigDto(
    @SerialName("imageShape") val imageShape: String = "circle",
    @SerialName("showStatusChip") val showStatusChip: Boolean = true,
    @SerialName("statusLabels") val statusLabels: StatusLabelsDto = StatusLabelsDto(),
    @SerialName("elevationDp") val elevationDp: Int = 4
)

@Serializable
data class StatusLabelsDto(
    @SerialName("alive") val alive: String = "Alive",
    @SerialName("dead") val dead: String = "Dead",
    @SerialName("unknown") val unknown: String = "Unknown"
)

@Serializable
data class ErrorViewConfigDto(
    @SerialName("title") val title: String = "Something went wrong",
    @SerialName("retryLabel") val retryLabel: String = "Try again"
)

@Serializable
data class ListConfigDto(
    @SerialName("skeletonCount") val skeletonCount: Int = 6,
    @SerialName("animationDurationMs") val animationDurationMs: Int = 300
)

@Serializable
data class DemoConfigDto(
    @SerialName("title") val title: String = "Demo de estados",
    @SerialName("subtitle") val subtitle: String = "Fuerza un estado de UI para verificar cada escenario.",
    @SerialName("loadingLabel") val loadingLabel: String = "Simular carga",
    @SerialName("loadingDescription") val loadingDescription: String = "Muestra el skeleton animado",
    @SerialName("restoreLabel") val restoreLabel: String = "Restaurar datos",
    @SerialName("restoreDescription") val restoreDescription: String = "Recarga desde la página 1",
    @SerialName("errorLabel") val errorLabel: String = "Simular error",
    @SerialName("errorDescription") val errorDescription: String = "Muestra la vista de error con Reintentar"
)
