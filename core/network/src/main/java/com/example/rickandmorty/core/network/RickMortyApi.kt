package com.example.rickandmorty.core.network

import com.example.rickandmorty.core.network.dto.CharacterResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface RickMortyApi {

    @GET("character")
    suspend fun getCharacters(@Query("page") page: Int = 1): CharacterResponseDto
}
