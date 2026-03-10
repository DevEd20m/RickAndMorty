package com.example.rickandmorty.feature.characters.domain.model

data class Character(
    val id: Int,
    val name: String,
    val status: CharacterStatus,
    val imageUrl: String
)

enum class CharacterStatus {
    ALIVE, DEAD, UNKNOWN;

    companion object {
        fun fromString(value: String): CharacterStatus = when (value.lowercase()) {
            "alive" -> ALIVE
            "dead" -> DEAD
            else -> UNKNOWN
        }
    }
}
