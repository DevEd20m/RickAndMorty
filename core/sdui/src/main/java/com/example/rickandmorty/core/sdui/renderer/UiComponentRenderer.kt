package com.example.rickandmorty.core.sdui.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rickandmorty.core.sdui.model.BannerConfig
import androidx.core.graphics.toColorInt

@Composable
fun BannerConfig.Consume(modifier: Modifier = Modifier) {
    if (!visible) return
    BannerV1(config = this, modifier = modifier)
}

@Composable
fun BannerV1(config: BannerConfig, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(parseHexColor(config.backgroundColor, fallback = Color(0xFF6200EE)))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = config.text,
            color = parseHexColor(config.textColor, fallback = Color.White),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun parseHexColor(hex: String, fallback: Color): Color =
    runCatching { Color(hex.toColorInt()) }.getOrElse { fallback }
