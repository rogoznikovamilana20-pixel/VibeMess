package com.vibe.ui.compose.screens.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.ui.compose.components.BottomNavTab
import com.vibe.ui.compose.components.VibeBottomNavigation
import com.vibe.ui.compose.components.VibeTopBar
import com.vibe.ui.compose.theme.BackgroundDark
import com.vibe.ui.compose.theme.TextPrimaryDark
import com.vibe.ui.compose.theme.TextSecondaryDark
import com.vibe.ui.compose.theme.TextTertiaryDark
import com.vibe.ui.compose.theme.VibePrimary
import com.vibe.ui.compose.theme.VibePurple

@Composable
fun AurionScreen(
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        VibeTopBar(title = "Aurion AI", onMenuClick = { })

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = VibePurple,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aurion",
                color = TextPrimaryDark,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your AI assistant",
                color = VibePurple,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "AI-powered conversations\nand smart replies",
                color = TextTertiaryDark,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        VibeBottomNavigation(
            selectedTab = BottomNavTab.AURION,
            onTabSelected = onTabSelected
        )
    }
}
