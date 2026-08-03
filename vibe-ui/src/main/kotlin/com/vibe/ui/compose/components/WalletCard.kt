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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.ui.compose.theme.AccentGold
import com.vibe.ui.compose.theme.SurfaceDark
import com.vibe.ui.compose.theme.TextPrimaryDark
import com.vibe.ui.compose.theme.TextSecondaryDark
import com.vibe.ui.compose.theme.VibePrimary

@Composable
fun WalletCard(
    balance: Long,
    streak: Int,
    canClaimBonus: Boolean,
    onClaimBonus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF7C3AED),
                        Color(0xFF8B5CF6),
                        Color(0xFFA78BFA)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "Wallet",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Vibe Coins",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "$balance",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "VC ($${"%.2f".format(balance * 0.01)})",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Streak: $streak days",
                    color = AccentGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = onClaimBonus,
                enabled = canClaimBonus,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (canClaimBonus) "Claim +5 VC" else "Claimed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CoinPackItem(
    coins: Long,
    priceUsd: Double,
    bonusPercent: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) VibePrimary.copy(alpha = 0.15f) else SurfaceDark)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$coins",
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "VC",
            color = TextSecondaryDark,
            fontSize = 11.sp
        )
        if (bonusPercent > 0) {
            Text(
                text = "+$bonusPercent%",
                color = AccentGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = "$${"%.2f".format(priceUsd)}",
            color = TextSecondaryDark,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
