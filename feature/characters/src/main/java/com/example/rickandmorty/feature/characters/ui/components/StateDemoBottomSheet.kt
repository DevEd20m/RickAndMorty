package com.example.rickandmorty.feature.characters.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rickandmorty.core.sdui.model.DemoConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateDemoBottomSheet(
    sheetState: SheetState,
    demoConfig: DemoConfig,
    onDismiss: () -> Unit,
    onSimulateLoading: () -> Unit,
    onSimulateError: () -> Unit,
    onRestoreRealData: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = demoConfig.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = demoConfig.subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            StateDemoButton(
                label = demoConfig.loadingLabel,
                description = demoConfig.loadingDescription,
                icon = Icons.Rounded.HourglassEmpty,
                color = MaterialTheme.colorScheme.secondary,
                onClick = {
                    onSimulateLoading()
                    onDismiss()
                }
            )

            StateDemoButton(
                label = demoConfig.restoreLabel,
                description = demoConfig.restoreDescription,
                icon = Icons.Rounded.CheckCircle,
                color = MaterialTheme.colorScheme.primary,
                onClick = {
                    onRestoreRealData()
                    onDismiss()
                }
            )

            StateDemoButton(
                label = demoConfig.errorLabel,
                description = demoConfig.errorDescription,
                icon = Icons.Rounded.Warning,
                color = MaterialTheme.colorScheme.error,
                onClick = {
                    onSimulateError()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun StateDemoButton(
    label: String,
    description: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = color
            )
            Column {
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}
