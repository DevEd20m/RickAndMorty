package com.example.rickandmorty.feature.characters.domain.repository

import com.example.rickandmorty.feature.characters.domain.model.Character

interface CharacterRepository {
    suspend fun getCharacters(page: Int = 1): Pair<List<Character>, String?>
}
