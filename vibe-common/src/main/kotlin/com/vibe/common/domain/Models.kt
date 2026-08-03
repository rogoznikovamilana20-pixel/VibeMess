package com.vibe.common.domain

/**
 * Vibe Coins — internal currency.
 * 1 VC = $0.01 (fixed rate).
 * Users earn VC through activity, purchases, and achievements.
 */
data class Wallet(
    val userId: Long,
    val balance: Long = 0,
    val pendingBalance: Long = 0,
    val earnedToday: Int = 0,
    val loginStreak: Int = 0,
    val lastDailyBonus: Long = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val canClaimDailyBonus: Boolean
        get() = System.currentTimeMillis() - lastDailyBonus >= 24 * 60 * 60 * 1000

    val dailyBonusAmount: Int
        get() = 5 + minOf(loginStreak, 10)
}

enum class TransactionType {
    PURCHASE,
    SPEND,
    EARN,
    WITHDRAW,
    ESCROW_LOCK,
    ESCROW_RELEASE,
    DAILY_BONUS,
    REFERRAL,
    WELCOME_BONUS,
    GIFT,
    MARKETPLACE_PROMOTION
}

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED,
    ESCROW_HELD,
    ESCROW_RELEASED,
    REFUNDED
}

enum class EarnSource {
    DAILY_BONUS,
    REFERRAL,
    WELCOME_BONUS,
    MESSAGE_ACTIVITY,
    ACHIEVEMENT,
    PURCHASE,
    MARKETPLACE_SALE,
    PROMOTION
}

enum class SpendReason {
    AI_REQUEST_OVER_LIMIT,
    AI_IMAGE_GENERATION,
    AI_VIDEO_GENERATION,
    PREMIUM_STICKER_PACK,
    GIFT_TO_USER,
    MARKETPLACE_PROMOTION,
    VOTE
}

data class Transaction(
    val id: String,
    val userId: Long,
    val type: TransactionType,
    val amount: Long,
    val counterpartyId: Long? = null,
    val marketplaceOrderId: String? = null,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val metadata: String = ""
)

enum class SubscriptionTier {
    FREE,
    PREMIUM,
    BUSINESS,
    ENTERPRISE
}

data class Subscription(
    val userId: Long,
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val expiresAt: Long = Long.MAX_VALUE,
    val autoRenew: Boolean = false,
    val purchasedAt: Long = System.currentTimeMillis()
) {
    val isActive: Boolean
        get() = System.currentTimeMillis() < expiresAt

    val isPremium: Boolean
        get() = tier != SubscriptionTier.FREE && isActive
}

data class Achievement(
    val id: String,
    val userId: Long,
    val title: String,
    val description: String,
    val rewardVC: Int,
    val unlockedAt: Long = System.currentTimeMillis(),
    val claimed: Boolean = false
)

data class CoinPack(
    val id: String,
    val coins: Long,
    val priceUsd: Double,
    val bonusPercent: Int = 0
) {
    val effectiveCoins: Long
        get() = coins + (coins * bonusPercent / 100)

    companion object {
        val PACKS = listOf(
            CoinPack("pack_100", 100, 0.99),
            CoinPack("pack_550", 550, 4.99, bonusPercent = 10),
            CoinPack("pack_1200", 1200, 9.99, bonusPercent = 20)
        )
    }
}

object FreeTierConfig {
    const val MAX_GROUP_SIZE = 500_000
    const val MAX_FILE_SIZE_GB = 10L
    const val MAX_SIMULTANEOUS_ACCOUNTS = 5
    const val AI_REQUESTS_PER_MONTH = 200
    const val AI_STICKERS_PER_MONTH = 10
    const val UNLIMITED_FOLDERS = true
    const val UNLIMITED_PINNED_CHATS = true
    const val RECORD_CALLS = true
    const val TRANSCRIPTION = true
    const val TRANSLATOR_50_LANGUAGES = true
    const val GROUP_CALLS_MAX = 20
    const val MARKETPLACE_BUYER_FEE_PERCENT = 0
    const val MARKETPLACE_SELLER_FEE_PERCENT = 10
    const val DAILY_BONUS_VC = 5
    const val REFERRAL_BONUS_VC = 100
    const val REFERRAL_FRIEND_BONUS_VC = 50
    const val MESSAGE_ACTIVITY_THRESHOLD = 10
    const val MESSAGE_ACTIVITY_VC = 1
}

object PremiumTierConfig {
    const val PRICE_USD_MONTHLY = 4.99
    const val AI_UNLIMITED = true
    const val AI_VOICE_CLONE = true
    const val EXCLUSIVE_THEMES = 100
    const val AURION_PRO = true
    const val ANALYTICS = true
    const val AR_MASKS = true
    const val PRIORITY_UPLOAD = true
    const val MONTHLY_VIBE_COINS = 500
}

object BusinessTierConfig {
    const val PRICE_USD_MONTHLY = 19.99
    const val MAX_SEATS = 10
    const val MARKETPLACE_SELLER_FEE_PERCENT = 5
    const val CRM_FEATURES = true
    const val API_ACCESS = true
    const val AUTO_REPLIES = true
    const val WHITE_LABEL = true
}
