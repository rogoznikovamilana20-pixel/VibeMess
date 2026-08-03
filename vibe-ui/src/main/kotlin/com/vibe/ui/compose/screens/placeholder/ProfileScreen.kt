package com.vibe.ui.compose.screens.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.ui.compose.components.BottomNavTab
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.compose.components.VibeBottomNavigation
import com.vibe.ui.compose.components.VibeTopBar
import com.vibe.ui.compose.theme.BackgroundDark
import com.vibe.ui.compose.theme.TextPrimaryDark
import com.vibe.ui.compose.theme.TextSecondaryDark
import com.vibe.ui.compose.theme.TextTertiaryDark
import com.vibe.ui.compose.theme.VibePrimary

@Composable
fun ProfileScreen(
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        VibeTopBar(title = "Profile", onMenuClick = { })

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VibeAvatar(
                name = "User",
                size = 80.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Profile",
                color = TextPrimaryDark,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Edit your profile, settings\nand privacy options",
                color = TextTertiaryDark,
                fontSize = 14.sp
            )
        }

        VibeBottomNavigation(
            selectedTab = BottomNavTab.PROFILE,
            onTabSelected = onTabSelected
        )
    }
}
