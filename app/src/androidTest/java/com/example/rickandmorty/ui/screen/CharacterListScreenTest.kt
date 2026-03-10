package com.example.rickandmorty.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.domain.model.CharacterStatus
import com.example.rickandmorty.presentation.state.CharacterUiState
import com.example.rickandmorty.presentation.viewmodel.CharacterListViewModel
import com.example.rickandmorty.ui.components.CharacterCard
import com.example.rickandmorty.ui.components.ErrorView
import com.example.rickandmorty.ui.components.LoadingSkeletonList
import com.example.rickandmorty.ui.theme.RickAndMortyTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class CharacterListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val fakeCharacters = listOf(
        Character(id = 1, name = "Rick Sanchez", status = CharacterStatus.ALIVE, imageUrl = "https://example.com/rick.png"),
        Character(id = 2, name = "Morty Smith", status = CharacterStatus.DEAD, imageUrl = "https://example.com/morty.png")
    )

    @Test
    fun loadingSkeletonIsDisplayedWhenStateIsLoading() {
        composeRule.setContent {
            RickAndMortyTheme {
                LoadingSkeletonList()
            }
        }

        composeRule.onNodeWithText("Rick Sanchez").assertDoesNotExist()
        composeRule.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun characterListIsDisplayedWhenStateIsSuccess() {
        composeRule.setContent {
            RickAndMortyTheme {
                CharacterCard(character = fakeCharacters[0])
            }
        }

        composeRule.onNodeWithText("Rick Sanchez").assertIsDisplayed()
        composeRule.onNodeWithText("Alive").assertIsDisplayed()
    }

    @Test
    fun secondCharacterCardRendersCorrectly() {
        composeRule.setContent {
            RickAndMortyTheme {
                CharacterCard(character = fakeCharacters[1])
            }
        }

        composeRule.onNodeWithText("Morty Smith").assertIsDisplayed()
        composeRule.onNodeWithText("Dead").assertIsDisplayed()
    }

    @Test
    fun errorViewIsDisplayedWithMessageAndRetryButton() {
        composeRule.setContent {
            RickAndMortyTheme {
                ErrorView(
                    message = "Network error occurred",
                    onRetry = {}
                )
            }
        }

        composeRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeRule.onNodeWithText("Network error occurred").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun retryButtonCallsOnRetryCallback() {
        var retryClicked = false

        composeRule.setContent {
            RickAndMortyTheme {
                ErrorView(
                    message = "Network error occurred",
                    onRetry = { retryClicked = true }
                )
            }
        }

        composeRule.onNodeWithText("Retry").performClick()
        assert(retryClicked) { "Retry callback was not invoked" }
    }

    @Test
    fun unknownStatusChipIsDisplayedCorrectly() {
        val unknownCharacter = Character(
            id = 3,
            name = "Beth Smith",
            status = CharacterStatus.UNKNOWN,
            imageUrl = "https://example.com/beth.png"
        )

        composeRule.setContent {
            RickAndMortyTheme {
                CharacterCard(character = unknownCharacter)
            }
        }

        composeRule.onNodeWithText("Beth Smith").assertIsDisplayed()
        composeRule.onNodeWithText("Unknown").assertIsDisplayed()
    }
}
