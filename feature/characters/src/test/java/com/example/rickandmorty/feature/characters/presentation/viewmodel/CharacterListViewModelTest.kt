package com.example.rickandmorty.feature.characters.presentation.viewmodel

import app.cash.turbine.test
import com.example.rickandmorty.core.sdui.usecase.GetScreenConfigUseCase
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.model.CharacterStatus
import com.example.rickandmorty.feature.characters.domain.usecase.GetCharactersUseCase
import com.example.rickandmorty.feature.characters.presentation.state.CharacterUiState
import com.example.rickandmorty.feature.characters.presentation.state.ScreenConfigDefaults
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
class CharacterListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var getCharactersUseCase: GetCharactersUseCase
    private lateinit var getScreenConfigUseCase: GetScreenConfigUseCase
    private lateinit var viewModel: CharacterListViewModel

    private val fakeCharacters = listOf(
        Character(id = 1, name = "Rick Sanchez", status = CharacterStatus.ALIVE, imageUrl = "https://example.com/rick.png"),
        Character(id = 2, name = "Morty Smith", status = CharacterStatus.ALIVE, imageUrl = "https://example.com/morty.png")
    )

    private val fakeResult: Result<Pair<List<Character>, String?>> = Result.success(Pair(fakeCharacters, null))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getCharactersUseCase = mockk()
        getScreenConfigUseCase = mockk()
        coEvery { getScreenConfigUseCase() } returns ScreenConfigDefaults.screenConfig()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Result<Pair<List<Character>, String?>>>()
        coEvery { getCharactersUseCase(any()) } coAnswers { gate.await() }
        viewModel = CharacterListViewModel(getCharactersUseCase, getScreenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            val first = awaitItem()
            assertTrue("Expected Loading but got ${first.uiState}", first.uiState is CharacterUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `screenState emits Success when repository returns data`() = runTest(testDispatcher) {
        coEvery { getCharactersUseCase(any()) } returns fakeResult
        viewModel = CharacterListViewModel(getCharactersUseCase, getScreenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            val result = awaitItem()
            assertTrue("Expected Success but got ${result.uiState}", result.uiState is CharacterUiState.Success)
            assertEquals(fakeCharacters, (result.uiState as CharacterUiState.Success).characters.toList())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `screenState emits Error when use case throws exception`() = runTest(testDispatcher) {
        val errorMessage = "Network error"
        coEvery { getCharactersUseCase(any()) } returns Result.failure(RuntimeException(errorMessage))
        viewModel = CharacterListViewModel(getCharactersUseCase, getScreenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            val result = awaitItem()
            assertTrue("Expected Error but got ${result.uiState}", result.uiState is CharacterUiState.Error)
            assertEquals(errorMessage, (result.uiState as CharacterUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry triggers reload and emits Loading then Success`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Result<Pair<List<Character>, String?>>>()
        coEvery { getCharactersUseCase(any()) } returns fakeResult
        viewModel = CharacterListViewModel(getCharactersUseCase, getScreenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            awaitItem() // Success from init

            coEvery { getCharactersUseCase(any()) } coAnswers { gate.await() }
            viewModel.retry()
            assertTrue("Expected Loading on retry", awaitItem().uiState is CharacterUiState.Loading)

            gate.complete(Result.success(Pair(fakeCharacters, null as String?)))
            assertTrue(awaitItem().uiState is CharacterUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry after error recovers to Success`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Result<Pair<List<Character>, String?>>>()
        coEvery { getCharactersUseCase(any()) } returns Result.failure(RuntimeException("Network error"))
        viewModel = CharacterListViewModel(getCharactersUseCase, getScreenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            assertTrue(awaitItem().uiState is CharacterUiState.Error)

            coEvery { getCharactersUseCase(any()) } coAnswers { gate.await() }
            viewModel.retry()
            assertTrue(awaitItem().uiState is CharacterUiState.Loading)

            gate.complete(Result.success(Pair(fakeCharacters, null as String?)))
            assertTrue(awaitItem().uiState is CharacterUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh updates isRefreshing flag`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Result<Pair<List<Character>, String?>>>()
        coEvery { getCharactersUseCase(any()) } returns fakeResult
        viewModel = CharacterListViewModel(getCharactersUseCase, getScreenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            awaitItem() // Success from init

            coEvery { getCharactersUseCase(any()) } coAnswers { gate.await() }
            viewModel.refresh()
            assertEquals(true, awaitItem().isRefreshing)

            gate.complete(Result.success(Pair(fakeCharacters, null as String?)))
            assertEquals(false, awaitItem().isRefreshing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getCharactersUseCase is called on init`() = runTest(testDispatcher) {
        coEvery { getCharactersUseCase(any()) } returns fakeResult
        viewModel = CharacterListViewModel(getCharactersUseCase, getScreenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        coVerify(exactly = 1) { getCharactersUseCase(1) }
    }

    @Test
    fun `loadMore appends next page to existing list`() = runTest(testDispatcher) {
        val page2Characters = listOf(
            Character(id = 3, name = "Summer Smith", status = CharacterStatus.ALIVE, imageUrl = "https://example.com/summer.png")
        )
        coEvery { getCharactersUseCase(1) } returns Result.success(Pair(fakeCharacters, "https://rickandmortyapi.com/api/character?page=2"))
        coEvery { getCharactersUseCase(2) } returns Result.success(Pair(page2Characters, null as String?))
        viewModel = CharacterListViewModel(getCharactersUseCase, getScreenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            val success = awaitItem()
            assertTrue(success.uiState is CharacterUiState.Success)
            assertTrue((success.uiState as CharacterUiState.Success).hasNextPage)

            viewModel.loadMore()
            val afterLoad = awaitItem()
            val finalState = if (afterLoad.uiState is CharacterUiState.LoadingMore) awaitItem() else afterLoad
            assertTrue(finalState.uiState is CharacterUiState.Success)
            assertEquals(3, (finalState.uiState as CharacterUiState.Success).characters.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
