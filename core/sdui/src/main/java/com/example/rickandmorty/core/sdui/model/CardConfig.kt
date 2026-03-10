package com.example.rickandmorty.core.sdui.model

data class CardConfig(
    val imageShape: ImageShape,
    val showStatusChip: Boolean,
    val statusLabels: StatusLabels,
    val elevationDp: Int
)

enum class ImageShape {
    CIRCLE, ROUNDED;

    companion object {
        fun fromString(value: String): ImageShape = when (value.lowercase()) {
            "circle" -> CIRCLE
            else -> ROUNDED
        }
    }
}

data class StatusLabels(val alive: String, val dead: String, val unknown: String)
