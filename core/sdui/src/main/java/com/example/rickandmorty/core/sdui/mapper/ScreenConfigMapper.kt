package com.example.rickandmorty.core.sdui.mapper

import com.example.rickandmorty.core.sdui.dto.ScreenConfigDto
import com.example.rickandmorty.core.sdui.model.BannerConfig
import com.example.rickandmorty.core.sdui.model.CardConfig
import com.example.rickandmorty.core.sdui.model.DemoConfig
import com.example.rickandmorty.core.sdui.model.ErrorViewConfig
import com.example.rickandmorty.core.sdui.model.ImageShape
import com.example.rickandmorty.core.sdui.model.ListConfig
import com.example.rickandmorty.core.sdui.model.ScreenConfig
import com.example.rickandmorty.core.sdui.model.StatusLabels
import com.example.rickandmorty.core.sdui.model.TopBarConfig

fun ScreenConfigDto.toDomain(): ScreenConfig = ScreenConfig(
    topBar = TopBarConfig(title = topBar.title, visible = topBar.visible),
    banner = BannerConfig(
        visible = banner.visible,
        text = banner.text,
        backgroundColor = banner.backgroundColor,
        textColor = banner.textColor
    ),
    card = CardConfig(
        imageShape = ImageShape.fromString(card.imageShape),
        showStatusChip = card.showStatusChip,
        statusLabels = StatusLabels(
            alive = card.statusLabels.alive,
            dead = card.statusLabels.dead,
            unknown = card.statusLabels.unknown
        ),
        elevationDp = card.elevationDp
    ),
    errorView = ErrorViewConfig(title = errorView.title, retryLabel = errorView.retryLabel),
    list = ListConfig(skeletonCount = list.skeletonCount, animationDurationMs = list.animationDurationMs),
    demo = DemoConfig(
        title = demo.title,
        subtitle = demo.subtitle,
        loadingLabel = demo.loadingLabel,
        loadingDescription = demo.loadingDescription,
        restoreLabel = demo.restoreLabel,
        restoreDescription = demo.restoreDescription,
        errorLabel = demo.errorLabel,
        errorDescription = demo.errorDescription
    )
)
