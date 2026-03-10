package com.example.rickandmorty.feature.characters.data.repository

import com.example.rickandmorty.core.network.RickMortyApi
import com.example.rickandmorty.core.network.dto.CharacterResponseDto
import com.example.rickandmorty.feature.characters.data.mapper.toDomain
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.repository.CharacterRepository
import javax.inject.Inject

class CharacterRepositoryImpl @Inject constructor(
    private val api: RickMortyApi
) : CharacterRepository {

    override suspend fun getCharacters(page: Int): Pair<List<Character>, String?> {
        val response: CharacterResponseDto = api.getCharacters(page)
        val characters: List<Character> = response.results.map { it.toDomain() }
        return Pair(characters, response.info.next)
    }
}
