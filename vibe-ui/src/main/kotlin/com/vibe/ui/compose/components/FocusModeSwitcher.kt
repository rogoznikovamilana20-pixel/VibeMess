package com.vibe.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibe.ui.focus.FocusModeManager
import com.vibe.ui.focus.FocusSpace

@Composable
fun FocusModeSwitcher(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val currentSpace by FocusModeManager.currentSpace.collectAsState()
    var showPanel by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        // Focus Mode Panel (slides down from top, below the top app bar)
        AnimatedVisibility(
            visible = showPanel,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
                    .clickable { showPanel = false }
            ) {
                Text(
                    "Пространство",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FocusSpaceCard(
                        space = FocusSpace.PERSONAL,
                        isSelected = currentSpace == FocusSpace.PERSONAL,
                        onClick = {
                            FocusModeManager.switchSpace(FocusSpace.PERSONAL)
                            showPanel = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FocusSpaceCard(
                        space = FocusSpace.WORK,
                        isSelected = currentSpace == FocusSpace.WORK,
                        onClick = {
                            FocusModeManager.switchSpace(FocusSpace.WORK)
                            showPanel = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    if (currentSpace == FocusSpace.PERSONAL)
                        "Личные чаты · Фиолетовый режим"
                    else
                        "Рабочие чаты · Синий режим",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Current space indicator — only visible as a small badge, does NOT intercept touches
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        if (currentSpace == FocusSpace.PERSONAL)
                            listOf(Color(0xFF8D2BFA), Color(0xFFB06BFF))
                        else
                            listOf(Color(0xFF10B6FA), Color(0xFF0EA5E9))
                    )
                )
                .clickable { showPanel = !showPanel }
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (currentSpace == FocusSpace.PERSONAL) Icons.Default.Person
                    else Icons.Default.BusinessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    currentSpace.label,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FocusSpaceCard(
    space: FocusSpace,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = when (space) {
        FocusSpace.PERSONAL -> listOf(Color(0xFF8D2BFA), Color(0xFFB06BFF))
        FocusSpace.WORK -> listOf(Color(0xFF10B6FA), Color(0xFF0EA5E9))
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Brush.linearGradient(gradient)
                else Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.1f), Color.Gray.copy(alpha = 0.05f)))
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (space == FocusSpace.PERSONAL) Icons.Default.Person
            else Icons.Default.BusinessCenter,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            space.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}
