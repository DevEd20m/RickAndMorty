package com.example.rickandmorty.feature.characters.data.repository

import androidx.paging.PagingSource
import com.example.rickandmorty.core.network.RickMortyApi
import com.example.rickandmorty.core.network.dto.CharacterDto
import com.example.rickandmorty.core.network.dto.CharacterResponseDto
import com.example.rickandmorty.core.network.dto.InfoDto
import com.example.rickandmorty.feature.characters.data.paging.CharacterPagingSource
import com.example.rickandmorty.feature.characters.domain.model.CharacterStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CharacterRepositoryImplTest {

    private lateinit var api: RickMortyApi
    private lateinit var pagingSource: CharacterPagingSource

    private val fakeDto = CharacterResponseDto(
        info = InfoDto(count = 826, pages = 42, next = "https://rickandmortyapi.com/api/character?page=2", prev = null),
        results = listOf(
            CharacterDto(id = 1, name = "Rick Sanchez", status = "Alive", image = "https://example.com/rick.png"),
            CharacterDto(id = 2, name = "Morty Smith", status = "Dead", image = "https://example.com/morty.png"),
            CharacterDto(id = 3, name = "Beth Smith", status = "unknown", image = "https://example.com/beth.png")
        )
    )

    @Before
    fun setUp() {
        api = mockk()
        pagingSource = CharacterPagingSource(api)
    }

    @Test
    fun `load returns mapped domain models on page 1`() = runTest {
        coEvery { api.getCharacters(1) } returns fakeDto

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = 1, loadSize = 20, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        assertEquals(3, result.data.size)
        assertEquals(1, result.data[0].id)
        assertEquals("Rick Sanchez", result.data[0].name)
        assertEquals("https://example.com/rick.png", result.data[0].imageUrl)
    }

    @Test
    fun `load maps status Alive to ALIVE`() = runTest {
        coEvery { api.getCharacters(1) } returns fakeDto

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = 1, loadSize = 20, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        assertEquals(CharacterStatus.ALIVE, result.data[0].status)
    }

    @Test
    fun `load maps status Dead to DEAD`() = runTest {
        coEvery { api.getCharacters(1) } returns fakeDto

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = 1, loadSize = 20, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        assertEquals(CharacterStatus.DEAD, result.data[1].status)
    }

    @Test
    fun `load sets nextKey to page 2 when next URL is present`() = runTest {
        coEvery { api.getCharacters(1) } returns fakeDto

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = 1, loadSize = 20, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        assertEquals(2, result.nextKey)
        assertNull(result.prevKey)
    }

    @Test
    fun `load sets nextKey to null on last page`() = runTest {
        val lastPageDto = fakeDto.copy(
            info = InfoDto(count = 826, pages = 42, next = null, prev = "https://rickandmortyapi.com/api/character?page=41")
        )
        coEvery { api.getCharacters(42) } returns lastPageDto

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = 42, loadSize = 20, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        assertNull(result.nextKey)
    }

    @Test
    fun `load returns Error when api throws`() = runTest {
        coEvery { api.getCharacters(1) } throws RuntimeException("Network failure")

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = 1, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Error)
        assertEquals("Network failure", (result as PagingSource.LoadResult.Error).throwable.message)
    }

    @Test
    fun `load returns empty page when api returns empty results`() = runTest {
        coEvery { api.getCharacters(1) } returns CharacterResponseDto(
            info = InfoDto(count = 0, pages = 0, next = null),
            results = emptyList()
        )

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = 1, loadSize = 20, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        assertEquals(0, result.data.size)
        assertNull(result.nextKey)
    }
}
