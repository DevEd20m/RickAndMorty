package com.example.rickandmorty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.rickandmorty.core.ui.theme.RickAndMortyTheme
import com.example.rickandmorty.feature.characters.ui.screen.CharacterListScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var isDarkTheme: Boolean by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RickAndMortyTheme(darkTheme = isDarkTheme) {
                CharacterListScreen(
                    onThemeToggle = { isDark -> isDarkTheme = isDark }
                )
            }
        }
    }
}