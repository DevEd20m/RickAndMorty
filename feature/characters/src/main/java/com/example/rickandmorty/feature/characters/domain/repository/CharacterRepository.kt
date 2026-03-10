package com.example.rickandmorty.feature.characters.domain.repository

import androidx.paging.PagingData
import com.example.rickandmorty.feature.characters.domain.model.Character
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun getCharactersPaged(): Flow<PagingData<Character>>
}
