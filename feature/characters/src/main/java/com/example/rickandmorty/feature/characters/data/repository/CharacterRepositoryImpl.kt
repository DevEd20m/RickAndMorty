package com.example.rickandmorty.feature.characters.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.rickandmorty.core.network.RickMortyApi
import com.example.rickandmorty.feature.characters.data.paging.CharacterPagingSource
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CharacterRepositoryImpl @Inject constructor(
    private val api: RickMortyApi
) : CharacterRepository {

    override fun getCharactersPaged(): Flow<PagingData<Character>> =
        Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            pagingSourceFactory = { CharacterPagingSource(api) }
        ).flow
}
