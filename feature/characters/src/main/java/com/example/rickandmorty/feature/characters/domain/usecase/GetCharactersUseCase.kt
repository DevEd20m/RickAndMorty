package com.example.rickandmorty.feature.characters.domain.usecase

import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.repository.CharacterRepository
import javax.inject.Inject

class GetCharactersUseCase @Inject constructor(
    private val repository: CharacterRepository
) {
    suspend operator fun invoke(page: Int = 1): Result<Pair<List<Character>, String?>> =
        runCatching { repository.getCharacters(page) }
}
