package com.vibe.ui.compose.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.common.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class WalletUiState {
    data object Loading : WalletUiState()
    data class Success(
        val wallet: Wallet,
        val transactions: List<Transaction> = emptyList(),
        val achievements: List<Achievement> = emptyList()
    ) : WalletUiState()
    data class Error(val message: String) : WalletUiState()
}

class WalletViewModel(
    private val gateway: PaymentGateway
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _showPurchaseDialog = MutableStateFlow(false)
    val showPurchaseDialog: StateFlow<Boolean> = _showPurchaseDialog.asStateFlow()

    fun loadWallet(userId: Long) {
        viewModelScope.launch {
            _uiState.value = WalletUiState.Loading
            try {
                val wallet = gateway.getBalance(userId).getOrThrow()
                val transactions = gateway.getTransactions(userId).getOrDefault(emptyList())
                val achievements = gateway.getAchievements(userId).getOrDefault(emptyList())
                _uiState.value = WalletUiState.Success(wallet, transactions, achievements)
            } catch (e: Exception) {
                _uiState.value = WalletUiState.Error(e.message ?: "Failed to load wallet")
            }
        }
    }

    fun claimDailyBonus(userId: Long) {
        viewModelScope.launch {
            val current = _uiState.value
            if (current is WalletUiState.Success) {
                try {
                    val bonus = ClaimDailyBonusUseCase(gateway).invoke(userId).getOrThrow()
                    val updatedWallet = current.wallet.copy(
                        balance = current.wallet.balance + bonus,
                        loginStreak = current.wallet.loginStreak + 1,
                        lastDailyBonus = System.currentTimeMillis()
                    )
                    _uiState.value = current.copy(wallet = updatedWallet)
                } catch (e: AlreadyClaimedException) {
                    // Already claimed, no-op
                }
            }
        }
    }

    fun purchaseCoins(userId: Long, pack: CoinPack) {
        viewModelScope.launch {
            try {
                PurchaseCoinsUseCase(gateway).invoke(userId, pack).getOrThrow()
                loadWallet(userId)
                _showPurchaseDialog.value = false
            } catch (e: Exception) {
                _uiState.value = WalletUiState.Error(e.message ?: "Purchase failed")
            }
        }
    }

    fun togglePurchaseDialog() {
        _showPurchaseDialog.value = !_showPurchaseDialog.value
    }
}
