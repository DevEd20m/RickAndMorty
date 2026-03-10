package com.example.rickandmorty.feature.characters.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.rickandmorty.core.network.RickMortyApi
import com.example.rickandmorty.feature.characters.data.mapper.toDomain
import com.example.rickandmorty.feature.characters.domain.model.Character

class CharacterPagingSource(
    private val api: RickMortyApi
) : PagingSource<Int, Character>() {

    override fun getRefreshKey(state: PagingState<Int, Character>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Character> {
        val page: Int = params.key ?: 1
        return try {
            val response = api.getCharacters(page)
            val characters: List<Character> = response.results.map { it.toDomain() }
            val nextPage: Int? = if (response.info.next != null) page + 1 else null
            LoadResult.Page(
                data = characters,
                prevKey = if (page == 1) null else page - 1,
                nextKey = nextPage
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
