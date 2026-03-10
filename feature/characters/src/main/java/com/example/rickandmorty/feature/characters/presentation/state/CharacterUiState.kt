package com.example.rickandmorty.feature.characters.presentation.state

import com.example.rickandmorty.feature.characters.domain.model.Character
import kotlinx.collections.immutable.ImmutableList

sealed class CharacterUiState {

    object Loading : CharacterUiState()

    data class Success(
        val characters: ImmutableList<Character>,
        val hasNextPage: Boolean = false
    ) : CharacterUiState()

    data class LoadingMore(val characters: ImmutableList<Character>) : CharacterUiState()

    data class Error(val message: String) : CharacterUiState()
}
