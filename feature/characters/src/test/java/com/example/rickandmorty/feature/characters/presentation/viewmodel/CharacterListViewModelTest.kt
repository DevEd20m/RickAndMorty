package com.example.rickandmorty.feature.characters.presentation.viewmodel

import androidx.paging.PagingData
import app.cash.turbine.test
import com.example.rickandmorty.core.sdui.usecase.GetScreenConfigUseCase
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.model.CharacterStatus
import com.example.rickandmorty.feature.characters.domain.usecase.GetCharactersUseCase
import com.example.rickandmorty.feature.characters.presentation.state.ScreenConfigDefaults
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class CharacterListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var getCharactersUseCase: GetCharactersUseCase
    private lateinit var getScreenConfigUseCase: GetScreenConfigUseCase
    private lateinit var viewModel: CharacterListViewModel

    private val fakeCharacters = listOf(
        Character(id = 1, name = "Rick Sanchez", status = CharacterStatus.ALIVE, imageUrl = "https://example.com/rick.png"),
        Character(id = 2, name = "Morty Smith", status = CharacterStatus.ALIVE, imageUrl = "https://example.com/morty.png")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getCharactersUseCase = mockk()
        getScreenConfigUseCase = mockk()
        every { getCharactersUseCase() } returns flowOf(PagingData.from(fakeCharacters))
        coEvery { getScreenConfigUseCase() } returns ScreenConfigDefaults.screenConfig()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial screenState has isDemoLoading false`() = runTest(testDispatcher) {
        viewModel = CharacterListViewModel(getCharactersUseCase, getScreenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            val state = awaitItem()
            assertFalse(state.isDemoLoading)
            assertFalse(state.isDemoError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `simulateLoading sets isDemoLoading true`() = runTest(testDispatcher) {
        viewModel = CharacterListViewModel(getCharactersUseCase, getScreenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            awaitItem() // initial
            viewModel.simulateLoading()
            val state = awaitItem()
            assertTrue(state.isDemoLoading)
            assertFalse(state.isDemoError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `simulateError sets isDemoError true`() = runTest(testDispatcher) {
        viewModel = CharacterListViewModel(getCharactersUseCase, getScreenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            awaitItem() // initial
            viewModel.simulateError()
            val state = awaitItem()
            assertTrue(state.isDemoError)
            assertFalse(state.isDemoLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restoreRealData clears demo flags`() = runTest(testDispatcher) {
        viewModel = CharacterListViewModel(getCharactersUseCase, getScreenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            awaitItem() // initial
            viewModel.simulateError()
            awaitItem() // isDemoError = true
            viewModel.restoreRealData()
            val state = awaitItem()
            assertFalse(state.isDemoLoading)
            assertFalse(state.isDemoError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `screenConfig is loaded from getScreenConfigUseCase`() = runTest(testDispatcher) {
        val expectedConfig = ScreenConfigDefaults.screenConfig()
        viewModel = CharacterListViewModel(getCharactersUseCase, getScreenConfigUseCase)
        backgroundScope.launch { viewModel.screenState.collect {} }

        viewModel.screenState.test {
            val state = awaitItem()
            assertTrue(state.screenConfig.topBar.visible == expectedConfig.topBar.visible)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
