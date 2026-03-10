package com.example.rickandmorty.feature.characters.presentation.viewmodel

import app.cash.turbine.test
import com.example.rickandmorty.core.sdui.model.ScreenConfig
import com.example.rickandmorty.core.sdui.repository.ScreenConfigRepository
import com.example.rickandmorty.core.sdui.usecase.GetScreenConfigUseCase
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.model.CharacterStatus
import com.example.rickandmorty.feature.characters.domain.repository.CharacterRepository
import com.example.rickandmorty.feature.characters.domain.usecase.GetCharactersUseCase
import com.example.rickandmorty.feature.characters.presentation.state.CharacterUiState
import com.example.rickandmorty.feature.characters.presentation.state.ScreenConfigDefaults
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterListViewModelIntegrationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val fakeScreenConfigRepository = object : ScreenConfigRepository {
        override suspend fun getScreenConfig(): ScreenConfig = ScreenConfigDefaults.screenConfig()
    }

    private val fakeCharacters = listOf(
        Character(id = 1, name = "Rick Sanchez", status = CharacterStatus.ALIVE, imageUrl = "https://example.com/rick.png"),
        Character(id = 2, name = "Morty Smith", status = CharacterStatus.ALIVE, imageUrl = "https://example.com/morty.png"),
        Character(id = 3, name = "Summer Smith", status = CharacterStatus.DEAD, imageUrl = "https://example.com/summer.png")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `end-to-end flow with fake repository emits Success`() = runTest(testDispatcher) {
        val fakeRepository = FakeCharacterRepository(fakeCharacters)
        val useCase = GetCharactersUseCase(fakeRepository)
        val screenConfigUseCase = GetScreenConfigUseCase(fakeScreenConfigRepository)
        val viewModel = CharacterListViewModel(useCase, screenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            val result = awaitItem()
            assertTrue(result.uiState is CharacterUiState.Success)
            assertEquals(3, (result.uiState as CharacterUiState.Success).characters.size)
            assertEquals("Rick Sanchez", (result.uiState as CharacterUiState.Success).characters[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `end-to-end flow with failing fake repository emits Error`() = runTest(testDispatcher) {
        val fakeRepository = FailingFakeCharacterRepository()
        val useCase = GetCharactersUseCase(fakeRepository)
        val screenConfigUseCase = GetScreenConfigUseCase(fakeScreenConfigRepository)
        val viewModel = CharacterListViewModel(useCase, screenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            val result = awaitItem()
            assertTrue(result.uiState is CharacterUiState.Error)
            assertEquals("Service unavailable", (result.uiState as CharacterUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry with fake repository transitions from Error to Success`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Pair<List<Character>, String?>>()
        val fakeRepository = object : CharacterRepository {
            var callCount = 0
            override suspend fun getCharacters(page: Int): Pair<List<Character>, String?> {
                callCount++
                return if (callCount == 1) throw RuntimeException("Service unavailable")
                else gate.await()
            }
        }
        val useCase = GetCharactersUseCase(fakeRepository)
        val screenConfigUseCase = GetScreenConfigUseCase(fakeScreenConfigRepository)
        val viewModel = CharacterListViewModel(useCase, screenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            assertTrue(awaitItem().uiState is CharacterUiState.Error)

            viewModel.retry()
            assertTrue(awaitItem().uiState is CharacterUiState.Loading)

            gate.complete(Pair(fakeCharacters, null))
            assertTrue(awaitItem().uiState is CharacterUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh with fake repository updates isRefreshing flag`() = runTest(testDispatcher) {
        val slowRepo = object : CharacterRepository {
            var callCount = 0
            override suspend fun getCharacters(page: Int): Pair<List<Character>, String?> {
                callCount++
                if (callCount > 1) kotlinx.coroutines.delay(Long.MAX_VALUE)
                return Pair(fakeCharacters, null)
            }
        }
        val useCase = GetCharactersUseCase(slowRepo)
        val screenConfigUseCase = GetScreenConfigUseCase(fakeScreenConfigRepository)
        val viewModel = CharacterListViewModel(useCase, screenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            awaitItem() // Success from init

            viewModel.refresh()
            assertEquals(true, awaitItem().isRefreshing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pagination loads next page and appends to list`() = runTest(testDispatcher) {
        val page2Characters = listOf(
            Character(id = 4, name = "Beth Smith", status = CharacterStatus.ALIVE, imageUrl = "https://example.com/beth.png")
        )
        val paginatedRepo = object : CharacterRepository {
            override suspend fun getCharacters(page: Int): Pair<List<Character>, String?> {
                return if (page == 1) Pair(fakeCharacters, "https://rickandmortyapi.com/api/character?page=2")
                else Pair(page2Characters, null)
            }
        }
        val useCase = GetCharactersUseCase(paginatedRepo)
        val screenConfigUseCase = GetScreenConfigUseCase(fakeScreenConfigRepository)
        val viewModel = CharacterListViewModel(useCase, screenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            val page1 = awaitItem()
            assertTrue(page1.uiState is CharacterUiState.Success)
            assertEquals(3, (page1.uiState as CharacterUiState.Success).characters.size)
            assertTrue((page1.uiState as CharacterUiState.Success).hasNextPage)

            viewModel.loadMore()
            val afterLoad = awaitItem()
            val finalState = if (afterLoad.uiState is CharacterUiState.LoadingMore) awaitItem() else afterLoad
            assertTrue(finalState.uiState is CharacterUiState.Success)
            assertEquals(4, (finalState.uiState as CharacterUiState.Success).characters.size)
            assertEquals("Beth Smith", (finalState.uiState as CharacterUiState.Success).characters[3].name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeCharacterRepository(
    private val characters: List<Character>
) : CharacterRepository {
    override suspend fun getCharacters(page: Int): Pair<List<Character>, String?> =
        Pair(characters, null)
}

private class FailingFakeCharacterRepository : CharacterRepository {
    override suspend fun getCharacters(page: Int): Pair<List<Character>, String?> =
        throw RuntimeException("Service unavailable")
}
