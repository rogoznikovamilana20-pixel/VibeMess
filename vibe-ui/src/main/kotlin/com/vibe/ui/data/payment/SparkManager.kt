package com.vibe.ui.data.payment

import android.content.Context
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.SparkBalanceEntity
import com.vibe.ui.data.db.entity.VibePlusStatusEntity
import com.vibe.ui.network.ServerConfig
import com.vibe.ui.network.VibeHttpClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SparkManager {

    data class SparkState(
        val balance: Long = 0,
        val isVibePlus: Boolean = false,
        val vibePlusExpiresAt: Long = 0
    )

    private const val SPARKS_PER_RUBLE = 100L
    const val COMMISSION_PERCENT = 5

    private val tag = "SparkManager"
    private lateinit var db: VibeDatabase
    private lateinit var serverConfig: ServerConfig
    private lateinit var httpClient: VibeHttpClient
    private val crashHandler = CoroutineExceptionHandler { _, e ->
        VibeLogger.e(tag, "background coroutine crashed", e)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + crashHandler)

    private val _state = MutableStateFlow(SparkState())
    val state: StateFlow<SparkState> = _state.asStateFlow()

    fun initialize(context: Context) {
        if (::db.isInitialized) return
        db = VibeDatabase.getDatabase(context.applicationContext)
        serverConfig = ServerConfig(context)
        httpClient = VibeHttpClient(serverConfig)
        loadFromDb()
    }

    /**
     * Гарантирует, что база инициализирована, даже если initialize() ещё не вызывали.
     * Использует контекст приложения Telegram-ядра как fallback.
     */
    private fun ensureInitialized() {
        if (::db.isInitialized) return
        val ctx = org.telegram.messenger.ApplicationLoader.applicationContext ?: return
        initialize(ctx)
    }

    private fun loadFromDb() {
        if (!::db.isInitialized) return
        scope.launch {
            val balance = db.sparkBalanceDao().get()?.balance ?: 0L
            val plus = db.vibePlusStatusDao().get()
            val active = plus?.isActive == true && (plus.expiresAt == 0L || plus.expiresAt > System.currentTimeMillis())
            _state.value = SparkState(
                balance = balance,
                isVibePlus = active,
                vibePlusExpiresAt = plus?.expiresAt ?: 0L
            )
        }
    }

    fun refreshFromServer() {
        ensureInitialized()
        if (!::db.isInitialized) return
        scope.launch {
            try {
                val server = httpClient.rustPaymentBalance() ?: return@launch
                db.sparkBalanceDao().let { dao ->
                    dao.get()?.let { dao.update(it.copy(balance = server.balance)) }
                        ?: dao.insert(SparkBalanceEntity(balance = server.balance))
                }
                if (server.subscriptionPlan != null) {
                    val expiresAt = try {
                        java.time.Instant.parse(server.subscriptionExpiresAt!!).toEpochMilli()
                    } catch (e: Exception) {
                        System.currentTimeMillis() + 30L * 24 * 3600 * 1000
                    }
                    val dao = db.vibePlusStatusDao()
                    dao.get()?.let { dao.update(it.copy(isActive = true, expiresAt = expiresAt)) }
                        ?: dao.insert(VibePlusStatusEntity(isActive = true, expiresAt = expiresAt))
                }
                loadFromDb()
            } catch (e: Exception) {
                VibeLogger.e(tag, "refreshFromServer failed", e)
            }
        }
    }

    suspend fun addSparks(amount: Long) {
        ensureInitialized()
        if (!::db.isInitialized) return
        withContext(Dispatchers.IO) {
            val dao = db.sparkBalanceDao()
            val updated = dao.add(amount)
            if (updated == 0) {
                dao.insert(SparkBalanceEntity(balance = amount))
            }
            loadFromDb()
        }
    }

    suspend fun spendSparks(amount: Long): Boolean {
        ensureInitialized()
        if (!::db.isInitialized) return false
        return withContext(Dispatchers.IO) {
            val dao = db.sparkBalanceDao()
            val updated = dao.spend(amount)
            if (updated > 0) {
                loadFromDb()
                true
            } else {
                false
            }
        }
    }

    fun activateVibePlus(durationDays: Long) {
        ensureInitialized()
        if (!::db.isInitialized) return
        scope.launch {
            val now = System.currentTimeMillis()
            val expiresAt = now + durationDays * 24 * 3600 * 1000
            val dao = db.vibePlusStatusDao()
            dao.get()?.let { dao.update(it.copy(isActive = true, expiresAt = expiresAt)) }
                ?: dao.insert(VibePlusStatusEntity(isActive = true, expiresAt = expiresAt))
            loadFromDb()
        }
    }

    fun sparksToRubles(sparks: Long): Long = sparks * SPARKS_PER_RUBLE

    fun commissionFor(sparks: Long): Long = sparks * COMMISSION_PERCENT / 100
}
