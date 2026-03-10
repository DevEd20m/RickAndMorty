package com.example.rickandmorty.feature.characters.domain.usecase

import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.model.CharacterStatus
import com.example.rickandmorty.feature.characters.domain.repository.CharacterRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetCharactersUseCaseTest {

    private lateinit var repository: CharacterRepository
    private lateinit var useCase: GetCharactersUseCase

    private val fakeCharacters = listOf(
        Character(id = 1, name = "Rick Sanchez", status = CharacterStatus.ALIVE, imageUrl = "https://example.com/rick.png"),
        Character(id = 2, name = "Morty Smith", status = CharacterStatus.ALIVE, imageUrl = "https://example.com/morty.png")
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetCharactersUseCase(repository)
    }

    @Test
    fun `invoke returns characters from repository`() = runTest {
        coEvery { repository.getCharacters(1) } returns Pair(fakeCharacters, null)

        val result = useCase(1)
        val (characters, _) = result.getOrThrow()

        assertEquals(fakeCharacters, characters)
    }

    @Test
    fun `invoke delegates to repository with correct page`() = runTest {
        coEvery { repository.getCharacters(2) } returns Pair(fakeCharacters, null)

        useCase(2)

        coVerify(exactly = 1) { repository.getCharacters(2) }
    }

    @Test
    fun `invoke returns next page URL when available`() = runTest {
        val nextUrl = "https://rickandmortyapi.com/api/character?page=2"
        coEvery { repository.getCharacters(1) } returns Pair(fakeCharacters, nextUrl)

        val (_, nextPage) = useCase(1).getOrThrow()

        assertEquals(nextUrl, nextPage)
    }

    @Test
    fun `invoke returns null next page on last page`() = runTest {
        coEvery { repository.getCharacters(42) } returns Pair(fakeCharacters, null)

        val (_, nextPage) = useCase(42).getOrThrow()

        assertNull(nextPage)
    }

    @Test
    fun `invoke wraps exception from repository in Result failure`() = runTest {
        coEvery { repository.getCharacters(1) } throws RuntimeException("API error")

        val result = useCase(1)

        assertTrue(result.isFailure)
        assertEquals("API error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke returns empty list when repository is empty`() = runTest {
        coEvery { repository.getCharacters(1) } returns Pair(emptyList(), null)

        val (characters, _) = useCase(1).getOrThrow()

        assertEquals(0, characters.size)
    }
}
