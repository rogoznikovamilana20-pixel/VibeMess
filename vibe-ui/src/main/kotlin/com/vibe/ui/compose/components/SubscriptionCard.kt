package com.vibe.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.common.domain.SubscriptionTier
import com.vibe.ui.compose.theme.AccentGold
import com.vibe.ui.compose.theme.Error
import com.vibe.ui.compose.theme.Success
import com.vibe.ui.compose.theme.SurfaceDark
import com.vibe.ui.compose.theme.TextPrimaryDark
import com.vibe.ui.compose.theme.TextSecondaryDark
import com.vibe.ui.compose.theme.TextTertiaryDark
import com.vibe.ui.compose.theme.VibePrimary
import com.vibe.ui.compose.theme.Warning

@Composable
fun SubscriptionCard(
    tier: SubscriptionTier,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = when (tier) {
        SubscriptionTier.FREE -> Triple(Icons.Default.Check, Success, "Free")
        SubscriptionTier.PREMIUM -> Triple(Icons.Default.AutoAwesome, AccentGold, "Premium")
        SubscriptionTier.BUSINESS -> Triple(Icons.Default.Business, VibePrimary, "Business")
        SubscriptionTier.ENTERPRISE -> Triple(Icons.Default.Star, Warning, "Enterprise")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when (tier) {
                    SubscriptionTier.FREE -> "Everything you need, free forever"
                    SubscriptionTier.PREMIUM -> "Exclusive AI and customization"
                    SubscriptionTier.BUSINESS -> "Team features and analytics"
                    SubscriptionTier.ENTERPRISE -> "Custom deployment"
                },
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun TierComparisonCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Text(
            text = "Vibe Free includes everything:",
            color = TextPrimaryDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        val freeFeatures = listOf(
            "Groups up to 500K members",
            "Files up to 10 GB",
            "PQ E2EE encryption for ALL chats",
            "Aurion AI: 200 requests/month",
            "Call recording + transcription",
            "Real-time translator (50+ languages)",
            "Unlimited folders and pins",
            "5 simultaneous accounts",
            "All stickers + AI sticker generation",
            "Marketplace: 0% buyer fee",
            "No ads, ever"
        )

        freeFeatures.forEach { feature ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = feature,
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Premium adds exclusive extras:",
            color = AccentGold,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        val premiumFeatures = listOf(
            "Unlimited AI (image/video generation)",
            "Voice AI clone",
            "100+ exclusive themes",
            "Aurion Pro (1M context)",
            "Chat analytics + AI insights",
            "AR masks for video calls",
            "500 VC monthly bonus"
        )

        premiumFeatures.forEach { feature ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = feature,
                    color = TextTertiaryDark,
                    fontSize = 13.sp
                )
            }
        }
    }
}
