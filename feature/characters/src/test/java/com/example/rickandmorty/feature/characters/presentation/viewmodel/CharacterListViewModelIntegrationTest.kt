package com.example.rickandmorty.feature.characters.presentation.viewmodel

import androidx.paging.PagingData
import app.cash.turbine.test
import com.example.rickandmorty.core.sdui.model.ScreenConfig
import com.example.rickandmorty.core.sdui.repository.ScreenConfigRepository
import com.example.rickandmorty.core.sdui.usecase.GetScreenConfigUseCase
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.model.CharacterStatus
import com.example.rickandmorty.feature.characters.domain.repository.CharacterRepository
import com.example.rickandmorty.feature.characters.domain.usecase.GetCharactersUseCase
import com.example.rickandmorty.feature.characters.presentation.state.ScreenConfigDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
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
    fun `initial screenState has demo flags false`() = runTest(testDispatcher) {
        val fakeRepository = FakeCharacterRepository(fakeCharacters)
        val useCase = GetCharactersUseCase(fakeRepository)
        val screenConfigUseCase = GetScreenConfigUseCase(fakeScreenConfigRepository)
        val viewModel = CharacterListViewModel(useCase, screenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            val state = awaitItem()
            assertFalse(state.isDemoLoading)
            assertFalse(state.isDemoError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `simulateLoading sets isDemoLoading true end-to-end`() = runTest(testDispatcher) {
        val useCase = GetCharactersUseCase(FakeCharacterRepository(fakeCharacters))
        val screenConfigUseCase = GetScreenConfigUseCase(fakeScreenConfigRepository)
        val viewModel = CharacterListViewModel(useCase, screenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            awaitItem()
            viewModel.simulateLoading()
            assertTrue(awaitItem().isDemoLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `simulateError then restoreRealData clears flags end-to-end`() = runTest(testDispatcher) {
        val useCase = GetCharactersUseCase(FakeCharacterRepository(fakeCharacters))
        val screenConfigUseCase = GetScreenConfigUseCase(fakeScreenConfigRepository)
        val viewModel = CharacterListViewModel(useCase, screenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            awaitItem()
            viewModel.simulateError()
            assertTrue(awaitItem().isDemoError)
            viewModel.restoreRealData()
            val restored = awaitItem()
            assertFalse(restored.isDemoError)
            assertFalse(restored.isDemoLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `screenConfig is populated from repository end-to-end`() = runTest(testDispatcher) {
        val useCase = GetCharactersUseCase(FakeCharacterRepository(fakeCharacters))
        val screenConfigUseCase = GetScreenConfigUseCase(fakeScreenConfigRepository)
        val viewModel = CharacterListViewModel(useCase, screenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            val state = awaitItem()
            assertTrue(state.screenConfig.topBar.visible)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeCharacterRepository(
    private val characters: List<Character>
) : CharacterRepository {
    override fun getCharactersPaged(): Flow<PagingData<Character>> =
        flowOf(PagingData.from(characters))
}
