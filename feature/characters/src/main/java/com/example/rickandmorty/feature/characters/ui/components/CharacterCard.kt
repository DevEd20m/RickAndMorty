package com.example.rickandmorty.feature.characters.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.rickandmorty.core.sdui.model.CardConfig
import com.example.rickandmorty.core.sdui.model.StatusLabels
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.model.CharacterStatus

private val AliveColor   = Color(0xFF00D4AA)
private val DeadColor    = Color(0xFFFF4D6D)
private val UnknownColor = Color(0xFF8888AA)

@Composable
fun CharacterCard(
    character: Character,
    cardConfig: CardConfig,
    modifier: Modifier = Modifier
) {
    val statusColor: Color = when (character.status) {
        CharacterStatus.ALIVE   -> AliveColor
        CharacterStatus.DEAD    -> DeadColor
        CharacterStatus.UNKNOWN -> UnknownColor
    }

    val cardBackground = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = cardBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .background(cardBackground)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {

            Box(modifier = Modifier.width(96.dp)) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(character.imageUrl)
                        .crossfade(300)
                        .memoryCacheKey(character.imageUrl)
                        .diskCacheKey(character.imageUrl)
                        .build(),
                    contentDescription = "${character.name} image",
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(Color(0xFF1A1A2E)),
                    error = ColorPainter(Color(0xFF2A0A10)),
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "#${character.id.toString().padStart(3, '0')}",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (cardConfig.showStatusChip) {
                    StatusRow(
                        status = character.status,
                        labels = cardConfig.statusLabels,
                        color = statusColor
                    )
                }
                Text(
                    text = character.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    lineHeight = 19.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 96.dp)
                .width(2.dp)
                .fillMaxHeight(0.7f)
                .clip(RoundedCornerShape(2.dp))
                .background(statusColor.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun StatusRow(status: CharacterStatus, labels: StatusLabels, color: Color) {
    val label: String = when (status) {
        CharacterStatus.ALIVE   -> labels.alive
        CharacterStatus.DEAD    -> labels.dead
        CharacterStatus.UNKNOWN -> labels.unknown
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp
        )
    }
}
