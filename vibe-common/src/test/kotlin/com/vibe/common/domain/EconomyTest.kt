package com.vibe.common.domain

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EconomyTest {

    private lateinit var gateway: MockPaymentGateway

    @Before
    fun setup() {
        gateway = MockPaymentGateway()
    }

    @Test
    fun `daily bonus claim succeeds first time`() = kotlinx.coroutines.runBlocking {
        gateway.setBalance(1, Wallet(userId = 1, balance = 0))
        val useCase = ClaimDailyBonusUseCase(gateway)
        val result = useCase(1)
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull()) // 5 VC base bonus
    }

    @Test
    fun `daily bonus fails if already claimed`() = kotlinx.coroutines.runBlocking {
        val now = System.currentTimeMillis()
        gateway.setBalance(1, Wallet(userId = 1, balance = 5, lastDailyBonus = now))
        val useCase = ClaimDailyBonusUseCase(gateway)
        val result = useCase(1)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AlreadyClaimedException)
    }

    @Test
    fun `referral gives 100 VC to inviter and 50 to friend`() = kotlinx.coroutines.runBlocking {
        gateway.setBalance(1, Wallet(userId = 1, balance = 0))
        gateway.setBalance(2, Wallet(userId = 2, balance = 0))
        val useCase = ReferralUseCase(gateway)
        val result = useCase.inviteFriend(1, 2)
        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrNull())
    }

    @Test
    fun `spend VC fails with insufficient balance`() = kotlinx.coroutines.runBlocking {
        gateway.setBalance(1, Wallet(userId = 1, balance = 5))
        val useCase = SpendVCUseCase(gateway)
        val result = useCase(1, 10, SpendReason.AI_REQUEST_OVER_LIMIT)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InsufficientBalanceException)
    }

    @Test
    fun `spend VC succeeds with sufficient balance`() = kotlinx.coroutines.runBlocking {
        gateway.setBalance(1, Wallet(userId = 1, balance = 100))
        val useCase = SpendVCUseCase(gateway)
        val result = useCase(1, 20, SpendReason.AI_IMAGE_GENERATION)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `spend VC fails with zero amount`() = kotlinx.coroutines.runBlocking {
        gateway.setBalance(1, Wallet(userId = 1, balance = 100))
        val useCase = SpendVCUseCase(gateway)
        val result = useCase(1, 0, SpendReason.AI_REQUEST_OVER_LIMIT)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidAmountException)
    }

    @Test
    fun `wallet daily bonus amount increases with streak`() {
        val wallet1 = Wallet(userId = 1, loginStreak = 0)
        assertEquals(5, wallet1.dailyBonusAmount)

        val wallet5 = Wallet(userId = 1, loginStreak = 5)
        assertEquals(10, wallet5.dailyBonusAmount)

        val wallet10 = Wallet(userId = 1, loginStreak = 10)
        assertEquals(15, wallet10.dailyBonusAmount)

        val wallet20 = Wallet(userId = 1, loginStreak = 20)
        assertEquals(15, wallet20.dailyBonusAmount) // capped at +10
    }

    @Test
    fun `coin pack bonus calculation`() {
        val pack100 = CoinPack("p1", 100, 0.99)
        assertEquals(100, pack100.effectiveCoins)

        val pack550 = CoinPack("p2", 550, 4.99, bonusPercent = 10)
        assertEquals(605, pack550.effectiveCoins)

        val pack1200 = CoinPack("p3", 1200, 9.99, bonusPercent = 20)
        assertEquals(1440, pack1200.effectiveCoins)
    }

    @Test
    fun `free tier config values are correct`() {
        assertEquals(500_000, FreeTierConfig.MAX_GROUP_SIZE)
        assertEquals(10L, FreeTierConfig.MAX_FILE_SIZE_GB)
        assertEquals(5, FreeTierConfig.MAX_SIMULTANEOUS_ACCOUNTS)
        assertEquals(200, FreeTierConfig.AI_REQUESTS_PER_MONTH)
        assertEquals(0, FreeTierConfig.MARKETPLACE_BUYER_FEE_PERCENT)
        assertEquals(10, FreeTierConfig.MARKETPLACE_SELLER_FEE_PERCENT)
    }
}

class MockPaymentGateway : PaymentGateway {
    private val wallets = mutableMapOf<Long, Wallet>()
    private val transactions = mutableListOf<Transaction>()

    fun setBalance(userId: Long, wallet: Wallet) {
        wallets[userId] = wallet
    }

    override suspend fun purchaseCoins(userId: Long, pack: CoinPack): Result<Transaction> {
        val wallet = wallets[userId] ?: Wallet(userId = userId)
        wallets[userId] = wallet.copy(balance = wallet.balance + pack.effectiveCoins)
        return Result.success(Transaction(id = "tx_${System.currentTimeMillis()}", userId = userId, type = TransactionType.PURCHASE, amount = pack.effectiveCoins))
    }

    override suspend fun subscribe(userId: Long, tier: SubscriptionTier, cycle: BillingCycle): Result<Subscription> {
        return Result.success(Subscription(userId = userId, tier = tier))
    }

    override suspend fun cancelSubscription(userId: Long): Result<Unit> = Result.success(Unit)

    override suspend fun getBalance(userId: Long): Result<Wallet> {
        return Result.success(wallets[userId] ?: Wallet(userId = userId))
    }

    override suspend fun spendVC(userId: Long, amount: Long, reason: SpendReason): Result<Transaction> {
        val wallet = wallets[userId] ?: return Result.failure(Exception("No wallet"))
        if (wallet.balance < amount) return Result.failure(InsufficientBalanceException())
        wallets[userId] = wallet.copy(balance = wallet.balance - amount)
        return Result.success(Transaction(id = "tx_${System.currentTimeMillis()}", userId = userId, type = TransactionType.SPEND, amount = amount))
    }

    override suspend fun earnVC(userId: Long, amount: Long, source: EarnSource): Result<Transaction> {
        val wallet = wallets[userId] ?: Wallet(userId = userId)
        wallets[userId] = wallet.copy(balance = wallet.balance + amount)
        return Result.success(Transaction(id = "tx_${System.currentTimeMillis()}", userId = userId, type = TransactionType.EARN, amount = amount))
    }

    override suspend fun claimDailyBonus(userId: Long): Result<Int> {
        val wallet = wallets[userId] ?: Wallet(userId = userId)
        if (!wallet.canClaimDailyBonus) return Result.failure(AlreadyClaimedException())
        val bonus = wallet.dailyBonusAmount
        wallets[userId] = wallet.copy(
            balance = wallet.balance + bonus,
            loginStreak = wallet.loginStreak + 1,
            lastDailyBonus = System.currentTimeMillis()
        )
        return Result.success(bonus)
    }

    override suspend fun transferVC(from: Long, to: Long, amount: Long, giftMessage: String?): Result<Transaction> {
        return Result.success(Transaction(id = "tx_${System.currentTimeMillis()}", userId = to, type = TransactionType.EARN, amount = amount))
    }

    override suspend fun getTransactions(userId: Long, limit: Int): Result<List<Transaction>> {
        return Result.success(transactions.filter { it.userId == userId }.take(limit))
    }

    override suspend fun getAchievements(userId: Long): Result<List<Achievement>> {
        return Result.success(emptyList())
    }
}
