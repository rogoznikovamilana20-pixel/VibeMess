package com.vibe.common.domain

class ClaimDailyBonusUseCase(private val gateway: PaymentGateway) {
    suspend operator fun invoke(userId: Long): Result<Int> {
        val wallet = gateway.getBalance(userId).getOrNull()
            ?: return Result.failure(Exception("Failed to load wallet"))
        
        if (!wallet.canClaimDailyBonus) {
            return Result.failure(AlreadyClaimedException())
        }
        
        return gateway.claimDailyBonus(userId)
    }
}

class ReferralUseCase(private val gateway: PaymentGateway) {
    suspend fun inviteFriend(inviterId: Long, friendId: Long): Result<Int> {
        val result = gateway.transferVC(
            from = 0, // system
            to = inviterId,
            amount = FreeTierConfig.REFERRAL_BONUS_VC.toLong(),
            giftMessage = null
        )
        if (result.isSuccess) {
            gateway.earnVC(friendId, FreeTierConfig.REFERRAL_FRIEND_BONUS_VC.toLong(), EarnSource.WELCOME_BONUS)
        }
        return result.map { FreeTierConfig.REFERRAL_BONUS_VC }
    }
}

class SpendVCUseCase(private val gateway: PaymentGateway) {
    suspend operator fun invoke(userId: Long, amount: Long, reason: SpendReason): Result<Transaction> {
        if (amount <= 0) return Result.failure(InvalidAmountException())
        
        val wallet = gateway.getBalance(userId).getOrNull()
            ?: return Result.failure(Exception("Failed to load wallet"))
        
        if (wallet.balance < amount) {
            return Result.failure(InsufficientBalanceException())
        }
        
        return gateway.spendVC(userId, amount, reason)
    }
}

class EarnActivityUseCase(private val gateway: PaymentGateway) {
    private val messageCount = java.util.concurrent.atomic.AtomicInteger(0)
    
    suspend fun onMessageSent(userId: Long): Result<Transaction?> {
        val count = messageCount.incrementAndGet()
        if (count >= FreeTierConfig.MESSAGE_ACTIVITY_THRESHOLD) {
            messageCount.set(0)
            return gateway.earnVC(
                userId,
                FreeTierConfig.MESSAGE_ACTIVITY_VC.toLong(),
                EarnSource.MESSAGE_ACTIVITY
            ).map { it }
        }
        return Result.success(null)
    }
}

class PurchaseCoinsUseCase(private val gateway: PaymentGateway) {
    suspend operator fun invoke(userId: Long, pack: CoinPack): Result<Transaction> {
        return gateway.purchaseCoins(userId, pack)
    }
}

class SubscribeUseCase(private val gateway: PaymentGateway) {
    suspend operator fun invoke(userId: Long, tier: SubscriptionTier, cycle: BillingCycle): Result<Subscription> {
        val wallet = gateway.getBalance(userId).getOrNull()
        if (wallet != null) {
            // Check if already subscribed at same or higher tier
            // In production, check subscription status
        }
        return gateway.subscribe(userId, tier, cycle)
    }
}
