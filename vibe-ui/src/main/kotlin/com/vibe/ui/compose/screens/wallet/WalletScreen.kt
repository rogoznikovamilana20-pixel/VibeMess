package com.vibe.ui.compose.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.common.domain.CoinPack
import com.vibe.ui.compose.components.CoinPackItem
import com.vibe.ui.compose.components.WalletCard
import com.vibe.ui.compose.theme.AccentGold
import com.vibe.ui.compose.theme.BackgroundDark
import com.vibe.ui.compose.theme.Error
import com.vibe.ui.compose.theme.Success
import com.vibe.ui.compose.theme.TextPrimaryDark
import com.vibe.ui.compose.theme.TextSecondaryDark
import com.vibe.ui.compose.theme.TextTertiaryDark
import com.vibe.ui.compose.theme.VibePrimary

@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    userId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val showPurchase by viewModel.showPurchaseDialog.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimaryDark,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Vibe Coins",
                color = TextPrimaryDark,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        when (val state = uiState) {
            is WalletUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 100.dp),
                    color = VibePrimary
                )
            }
            is WalletUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Error", color = Error, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = state.message, color = TextSecondaryDark, fontSize = 14.sp)
                }
            }
            is WalletUiState.Success -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Wallet card
                    item {
                        WalletCard(
                            balance = state.wallet.balance,
                            streak = state.wallet.loginStreak,
                            canClaimBonus = state.wallet.canClaimDailyBonus,
                            onClaimBonus = { viewModel.claimDailyBonus(userId) }
                        )
                    }

                    // Earn VC section
                    item {
                        Text(
                            text = "Earn Vibe Coins",
                            color = TextPrimaryDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    item {
                        EarnOption(
                            title = "Daily login bonus",
                            subtitle = "+${state.wallet.dailyBonusAmount} VC",
                            icon = Icons.Default.CardGiftcard,
                            color = AccentGold
                        )
                    }

                    item {
                        EarnOption(
                            title = "Send messages",
                            subtitle = "+1 VC per 10 messages",
                            icon = Icons.Default.TrendingUp,
                            color = Success
                        )
                    }

                    // Purchase packs
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Buy Vibe Coins",
                            color = TextPrimaryDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CoinPack.PACKS.forEach { pack ->
                                CoinPackItem(
                                    coins = pack.effectiveCoins,
                                    priceUsd = pack.priceUsd,
                                    bonusPercent = pack.bonusPercent,
                                    isSelected = false,
                                    onClick = { viewModel.togglePurchaseDialog() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Purchase dialog
    if (showPurchase) {
        AlertDialog(
            onDismissRequest = { viewModel.togglePurchaseDialog() },
            title = { Text("Confirm Purchase") },
            text = { Text("Buy Vibe Coins? Payment will be processed via Google Play.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.purchaseCoins(userId, CoinPack.PACKS.first()) },
                    colors = ButtonDefaults.buttonColors(containerColor = VibePrimary)
                ) {
                    Text("Buy")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.togglePurchaseDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EarnOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1730))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = TextTertiaryDark, fontSize = 12.sp)
        }
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Success,
            modifier = Modifier.size(16.dp)
        )
    }
}
