package com.example.rickandmorty.feature.characters.data.repository

/**
 * CharacterRepositoryImplTest: tests unitarios del repositorio.
 *
 * Objetivo: verificar que CharacterRepositoryImpl:
 * 1. Delega la llamada a RickMortyApi con el número de página correcto.
 * 2. Mapea los DTOs a modelos de dominio correctamente.
 * 3. Propaga las páginas de paginación (next URL) desde la respuesta de la API.
 * 4. Propaga excepciones al caller (ViewModel via UseCase).
 *
 * Patrón: Arrange-Act-Assert (AAA).
 * - Arrange: preparar mocks y datos de prueba.
 * - Act: ejecutar el método bajo prueba.
 * - Assert: verificar el resultado.
 */

import com.example.rickandmorty.core.network.RickMortyApi
import com.example.rickandmorty.core.network.dto.CharacterDto
import com.example.rickandmorty.core.network.dto.CharacterResponseDto
import com.example.rickandmorty.core.network.dto.InfoDto
import com.example.rickandmorty.feature.characters.domain.model.CharacterStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CharacterRepositoryImplTest {

    private lateinit var api: RickMortyApi
    private lateinit var repository: CharacterRepositoryImpl

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
        repository = CharacterRepositoryImpl(api)
    }

    @Test
    fun `getCharacters returns mapped domain models`() = runTest {
        coEvery { api.getCharacters(1) } returns fakeDto

        val (characters, _) = repository.getCharacters(1)

        assertEquals(3, characters.size)
        assertEquals(1, characters[0].id)
        assertEquals("Rick Sanchez", characters[0].name)
        assertEquals("https://example.com/rick.png", characters[0].imageUrl)
    }

    @Test
    fun `status Alive maps to ALIVE enum`() = runTest {
        coEvery { api.getCharacters(1) } returns fakeDto

        val (characters, _) = repository.getCharacters(1)

        assertEquals(CharacterStatus.ALIVE, characters[0].status)
    }

    @Test
    fun `status Dead maps to DEAD enum`() = runTest {
        coEvery { api.getCharacters(1) } returns fakeDto

        val (characters, _) = repository.getCharacters(1)

        assertEquals(CharacterStatus.DEAD, characters[1].status)
    }

    @Test
    fun `status unknown maps to UNKNOWN enum`() = runTest {
        coEvery { api.getCharacters(1) } returns fakeDto

        val (characters, _) = repository.getCharacters(1)

        assertEquals(CharacterStatus.UNKNOWN, characters[2].status)
    }

    @Test
    fun `getCharacters delegates to api with correct page`() = runTest {
        coEvery { api.getCharacters(2) } returns fakeDto

        repository.getCharacters(2)

        coVerify(exactly = 1) { api.getCharacters(2) }
    }

    @Test
    fun `getCharacters returns next page URL when available`() = runTest {
        coEvery { api.getCharacters(1) } returns fakeDto

        val (_, nextPage) = repository.getCharacters(1)

        assertEquals("https://rickandmortyapi.com/api/character?page=2", nextPage)
    }

    @Test
    fun `getCharacters returns null next page on last page`() = runTest {
        val lastPageDto = fakeDto.copy(
            info = InfoDto(count = 826, pages = 42, next = null, prev = "https://rickandmortyapi.com/api/character?page=41")
        )
        coEvery { api.getCharacters(42) } returns lastPageDto

        val (_, nextPage) = repository.getCharacters(42)

        assertNull(nextPage)
    }

    @Test
    fun `getCharacters propagates exception from api`() = runTest {
        coEvery { api.getCharacters(1) } throws RuntimeException("Network failure")

        val exception = runCatching { repository.getCharacters(1) }.exceptionOrNull()

        assertEquals("Network failure", exception?.message)
    }

    @Test
    fun `getCharacters returns empty list when api returns empty results`() = runTest {
        coEvery { api.getCharacters(1) } returns CharacterResponseDto(
            info = InfoDto(count = 0, pages = 0, next = null),
            results = emptyList()
        )

        val (characters, nextPage) = repository.getCharacters(1)

        assertEquals(0, characters.size)
        assertNull(nextPage)
    }
}
