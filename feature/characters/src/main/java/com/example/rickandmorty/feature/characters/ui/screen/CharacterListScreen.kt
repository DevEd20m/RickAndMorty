package com.example.rickandmorty.feature.characters.ui.screen

import android.os.Bundle
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.rickandmorty.core.sdui.model.CardConfig
import com.example.rickandmorty.core.sdui.model.ScreenConfig
import com.example.rickandmorty.core.sdui.renderer.Consume
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.presentation.state.ScreenState
import com.example.rickandmorty.feature.characters.presentation.viewmodel.CharacterListViewModel
import com.example.rickandmorty.feature.characters.ui.components.CharacterCard
import com.example.rickandmorty.feature.characters.ui.components.ErrorView
import com.example.rickandmorty.feature.characters.ui.components.LoadingSkeletonList
import com.example.rickandmorty.feature.characters.ui.components.StateDemoBottomSheet
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    viewModel: CharacterListViewModel = hiltViewModel(),
    onThemeToggle: (isDark: Boolean) -> Unit = {}
) {
    val screenState: ScreenState by viewModel.screenState.collectAsState()
    val screenConfig: ScreenConfig = screenState.screenConfig
    val characters: LazyPagingItems<Character> = viewModel.characters.collectAsLazyPagingItems()

    androidx.compose.runtime.LaunchedEffect(screenState.isDarkTheme) {
        onThemeToggle(screenState.isDarkTheme)
    }

    var showDemoSheet: Boolean by remember { mutableStateOf(false) }
    val demoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val analytics: FirebaseAnalytics = Firebase.analytics
    SideEffect {
        val bundle = Bundle().apply {
            putString("banner_visible", screenConfig.banner.visible.toString())
            putString("image_shape", screenConfig.card.imageShape.name.lowercase())
            putString("top_bar_title", screenConfig.topBar.title)
        }
        analytics.logEvent("sdui_config_applied", bundle)
    }

    if (showDemoSheet) {
        StateDemoBottomSheet(
            sheetState = demoSheetState,
            demoConfig = screenConfig.demo,
            onDismiss = { showDemoSheet = false },
            onSimulateLoading = viewModel::simulateLoading,
            onSimulateError = viewModel::simulateError,
            onRestoreRealData = viewModel::restoreRealData,
            isDarkTheme = screenState.isDarkTheme,
            onToggleTheme = viewModel::toggleTheme
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDemoSheet = true },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "State demo"
                )
            }
        },
        topBar = {
            if (screenConfig.topBar.visible) {
                TopBar(title = screenConfig.topBar.title)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (screenConfig.banner.visible) {
                screenConfig.banner.Consume()
            }

            PullToRefreshBox(
                isRefreshing = screenState.isRefreshing,
                onRefresh = { characters.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    screenState.isDemoLoading ->
                        LoadingSkeletonList(count = screenConfig.list.skeletonCount)

                    screenState.isDemoError ->
                        ErrorView(
                            message = "Error simulado",
                            onRetry = viewModel::restoreRealData,
                            title = screenConfig.errorView.title,
                            retryLabel = screenConfig.errorView.retryLabel
                        )

                    characters.loadState.refresh is LoadState.Loading ->
                        LoadingSkeletonList(count = screenConfig.list.skeletonCount)

                    characters.loadState.refresh is LoadState.Error ->
                        ErrorView(
                            message = (characters.loadState.refresh as LoadState.Error).error.message
                                ?: "Error desconocido",
                            onRetry = { characters.retry() },
                            title = screenConfig.errorView.title,
                            retryLabel = screenConfig.errorView.retryLabel
                        )

                    else ->
                        CharacterList(
                            characters = characters,
                            cardConfig = screenConfig.card,
                            animationDurationMs = screenConfig.list.animationDurationMs
                        )
                }
            }
        }
    }
}

@Composable
private fun CharacterList(
    characters: LazyPagingItems<Character>,
    cardConfig: CardConfig,
    animationDurationMs: Int
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = characters.itemCount,
            key = characters.itemKey { it.id }
        ) { index ->
            val character: Character? = characters[index]
            if (character != null) {
                CharacterCard(
                    character = character,
                    cardConfig = cardConfig,
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(durationMillis = animationDurationMs),
                        placementSpec = tween(durationMillis = animationDurationMs),
                        fadeOutSpec = tween(durationMillis = animationDurationMs)
                    )
                )
            }
        }

        when (val appendState: LoadState = characters.loadState.append) {
            is LoadState.Loading -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is LoadState.Error -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = appendState.error.message ?: "Error cargando más",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun TopBar(title: String) {
    val bgColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBgColor = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        bgColor,
                        bgColor.copy(alpha = 0f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor,
                                primaryColor.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                letterSpacing = (-0.5).sp,
                modifier = Modifier
                    .graphicsLayer(alpha = 0.99f)
                    .drawWithCache {
                        val gradient: Brush = Brush.linearGradient(
                            colors = listOf(
                                onBgColor,
                                onBgColor.copy(alpha = 0.7f),
                                primaryColor
                            )
                        )
                        onDrawWithContent {
                            drawContent()
                            drawRect(gradient, blendMode = BlendMode.SrcAtop)
                        }
                    }
            )
        }
    }
}
