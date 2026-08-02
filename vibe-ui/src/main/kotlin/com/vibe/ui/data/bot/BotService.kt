package com.vibe.ui.data.bot

import android.content.Context
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.BotEntity
import com.vibe.ui.data.db.entity.BotMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Background worker: polls server updates for owned bots and answers them
 * through the bot token (BotFather-style). Runs while the app lives.
 */
object BotService {

    private const val TAG = "BotService"
    private const val POLL_INTERVAL_MS = 5_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    fun start(context: Context) {
        if (job?.isActive == true) return
        _active.value = true

        job = scope.launch {
            while (isActive) {
                try {
                    val repo = BotRepository(context)
                    val db = VibeDatabase.getDatabase(context)
                    val owned = repo.ownedBots().filter { it.token.isNotBlank() }
                    for (bot in owned) {
                        processBot(context, repo, db, bot)
                    }
                } catch (e: Exception) {
                    VibeLogger.e(TAG, "poll cycle failed", e)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun processBot(
        context: Context,
        repo: BotRepository,
        db: VibeDatabase,
        bot: BotEntity
    ) {
        val updates = repo.pollServerUpdates(bot)
        if (updates.isEmpty()) return

        for (update in updates) {
            val history = db.botMessageDao().getByBotId(bot.id).first()
            val reply = BotEngine.reply(bot, update.text, history)
            if (reply != null) {
                repo.answerServerUpdate(bot, update.updateId, reply)
                db.botMessageDao().insert(
                    BotMessageEntity(
                        botId = bot.id,
                        text = update.text,
                        isUser = true
                    )
                )
                db.botMessageDao().insert(
                    BotMessageEntity(
                        botId = bot.id,
                        text = reply,
                        isUser = false
                    )
                )
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _active.value = false
    }
}
