package com.vibe.common.domain

/**
 * Payment gateway interface for Vibe Coins economy.
 * Implementation will handle Google Play Billing, in-app purchases, and subscriptions.
 */
interface PaymentGateway {
    suspend fun purchaseCoins(userId: Long, pack: CoinPack): Result<Transaction>
    suspend fun subscribe(userId: Long, tier: SubscriptionTier, cycle: BillingCycle): Result<Subscription>
    suspend fun cancelSubscription(userId: Long): Result<Unit>
    suspend fun getBalance(userId: Long): Result<Wallet>
    suspend fun spendVC(userId: Long, amount: Long, reason: SpendReason): Result<Transaction>
    suspend fun earnVC(userId: Long, amount: Long, source: EarnSource): Result<Transaction>
    suspend fun claimDailyBonus(userId: Long): Result<Int>
    suspend fun transferVC(from: Long, to: Long, amount: Long, giftMessage: String?): Result<Transaction>
    suspend fun getTransactions(userId: Long, limit: Int = 50): Result<List<Transaction>>
    suspend fun getAchievements(userId: Long): Result<List<Achievement>>
}

enum class BillingCycle { MONTHLY, YEARLY }

class AlreadyClaimedException : Exception("Daily bonus already claimed today")
class InsufficientBalanceException : Exception("Not enough Vibe Coins")
class SubscriptionActiveException : Exception("Subscription already active")
class InvalidAmountException : Exception("Amount must be positive")
