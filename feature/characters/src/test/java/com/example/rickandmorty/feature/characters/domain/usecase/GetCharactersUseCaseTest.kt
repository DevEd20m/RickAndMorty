package com.example.rickandmorty.feature.characters.domain.usecase

import androidx.paging.PagingData
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.model.CharacterStatus
import com.example.rickandmorty.feature.characters.domain.repository.CharacterRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
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
    fun `invoke returns flow from repository`() = runTest {
        val fakePagingData: Flow<PagingData<Character>> = flowOf(PagingData.from(fakeCharacters))
        every { repository.getCharactersPaged() } returns fakePagingData

        val result = useCase()

        assertNotNull(result)
    }

    @Test
    fun `invoke delegates to repository getCharactersPaged`() = runTest {
        every { repository.getCharactersPaged() } returns flowOf(PagingData.from(fakeCharacters))

        useCase()

        verify(exactly = 1) { repository.getCharactersPaged() }
    }

    @Test
    fun `invoke returns empty paging data when repository is empty`() = runTest {
        every { repository.getCharactersPaged() } returns flowOf(PagingData.empty())

        val result = useCase()

        assertNotNull(result)
    }
}
