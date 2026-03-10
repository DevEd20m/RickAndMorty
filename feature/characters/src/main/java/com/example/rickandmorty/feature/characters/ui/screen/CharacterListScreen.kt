package com.example.rickandmorty.feature.characters.ui.screen

import android.os.Bundle
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.background
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
import com.example.rickandmorty.core.sdui.model.CardConfig
import com.example.rickandmorty.core.sdui.model.ScreenConfig
import com.example.rickandmorty.core.sdui.renderer.Consume
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.presentation.state.CharacterUiState
import com.example.rickandmorty.feature.characters.presentation.state.ScreenState
import com.example.rickandmorty.feature.characters.presentation.viewmodel.CharacterListViewModel
import com.example.rickandmorty.feature.characters.ui.components.CharacterCard
import com.example.rickandmorty.feature.characters.ui.components.ErrorView
import com.example.rickandmorty.feature.characters.ui.components.LoadingSkeletonList
import com.example.rickandmorty.feature.characters.ui.components.StateDemoBottomSheet
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    viewModel: CharacterListViewModel = hiltViewModel()
) {
    val screenState: ScreenState by viewModel.screenState.collectAsState()
    val screenConfig: ScreenConfig = screenState.screenConfig
    val showBanner: Boolean = screenConfig.banner.visible

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
            onRestoreRealData = viewModel::restoreRealData
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
        val listState: LazyListState = rememberLazyListState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showBanner) {
                screenConfig.banner.Consume()
            }

            PullToRefreshBox(
                isRefreshing = screenState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = screenState.uiState) {
                    is CharacterUiState.Loading ->
                        LoadingSkeletonList(count = screenConfig.list.skeletonCount)

                    is CharacterUiState.Success ->
                        CharacterList(
                            listState = listState,
                            characters = state.characters,
                            hasNextPage = state.hasNextPage,
                            cardConfig = screenConfig.card,
                            animationDurationMs = screenConfig.list.animationDurationMs,
                            onLoadMore = viewModel::loadMore
                        )

                    is CharacterUiState.LoadingMore ->
                        CharacterList(
                            listState = listState,
                            characters = state.characters,
                            hasNextPage = true,
                            cardConfig = screenConfig.card,
                            animationDurationMs = screenConfig.list.animationDurationMs,
                            onLoadMore = {},
                            showLoadingFooter = true
                        )

                    is CharacterUiState.Error ->
                        ErrorView(
                            message = state.message,
                            onRetry = viewModel::retry,
                            title = screenConfig.errorView.title,
                            retryLabel = screenConfig.errorView.retryLabel
                        )
                }
            }
        }
    }
}

@Composable
private fun CharacterList(
    listState: LazyListState,
    characters: List<Character>,
    hasNextPage: Boolean,
    cardConfig: CardConfig,
    animationDurationMs: Int,
    onLoadMore: () -> Unit,
    showLoadingFooter: Boolean = false
) {
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                val lastVisible: Int = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalItems: Int = layoutInfo.totalItemsCount
                hasNextPage && totalItems > 0 && lastVisible >= totalItems - 3
            }
            .distinctUntilChanged()
            .collect { shouldLoad: Boolean -> if (shouldLoad) onLoadMore() }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(items = characters, key = { _, character -> character.id }) { _, character ->
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

        if (showLoadingFooter) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050510),
                        Color(0x00050510)
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
                                Color(0xFF00D4AA),
                                Color(0x3300D4AA),
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
                                Color(0xFFFFFFFF),
                                Color(0xFFA0C8FF),
                                Color(0xFF00D4AA)
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
