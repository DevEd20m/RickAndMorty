package com.example.rickandmorty.feature.characters.domain.usecase

import androidx.paging.PagingData
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCharactersUseCase @Inject constructor(
    private val repository: CharacterRepository
) {
    operator fun invoke(): Flow<PagingData<Character>> = repository.getCharactersPaged()
}
