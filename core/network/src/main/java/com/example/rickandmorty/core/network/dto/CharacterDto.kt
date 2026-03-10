package com.example.rickandmorty.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CharacterResponseDto(
    @SerialName("info") val info: InfoDto,
    @SerialName("results") val results: List<CharacterDto>
)

@Serializable
data class InfoDto(
    @SerialName("count") val count: Int,
    @SerialName("pages") val pages: Int,
    @SerialName("next") val next: String? = null,
    @SerialName("prev") val prev: String? = null
)

@Serializable
data class CharacterDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("status") val status: String,
    @SerialName("image") val image: String
)
