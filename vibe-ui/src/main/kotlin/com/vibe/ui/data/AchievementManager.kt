package com.vibe.ui.data

import android.content.Context

class AchievementManager(context: Context) {

    private val prefs = context.getSharedPreferences("vibe_achievements", Context.MODE_PRIVATE)

    enum class Id {
        FIRST_MESSAGE,
        FIRST_AI,
        TEN_MESSAGES,
        FIRST_CALL,
        PROFILE_SET,
        FIRST_POST,
        FIRST_LISTING
    }

    data class Achievement(
        val id: Id,
        val title: String,
        val description: String,
        val iconName: String
    )

    val allAchievements: List<Achievement> = listOf(
        Achievement(Id.FIRST_MESSAGE, "Первое сообщение", "Отправьте своё первое сообщение", "chat"),
        Achievement(Id.FIRST_AI, "Знакомство с AI", "Впервые обратитесь к Aurion", "auto_awesome"),
        Achievement(Id.TEN_MESSAGES, "Болтун", "Отправьте 10 сообщений", "forum"),
        Achievement(Id.FIRST_CALL, "Звонок", "Совершите первый звонок", "call"),
        Achievement(Id.PROFILE_SET, "Личность", "Заполните профиль", "person"),
        Achievement(Id.FIRST_POST, "Блогер", "Создайте первый пост в ленте", "timeline"),
        Achievement(Id.FIRST_LISTING, "Продавец", "Создайте первое объявление", "store"),
    )

    fun isUnlocked(id: Id): Boolean = prefs.getBoolean(id.name, false)

    fun unlock(id: Id) {
        prefs.edit().putBoolean(id.name, true).apply()
    }

    fun trackMessageSent() {
        val count = prefs.getInt("messages_sent", 0) + 1
        prefs.edit().putInt("messages_sent", count).apply()
        if (count == 1) unlock(Id.FIRST_MESSAGE)
        if (count >= 10) unlock(Id.TEN_MESSAGES)
    }

    fun getAll(): List<Pair<Achievement, Boolean>> = allAchievements.map { it to isUnlocked(it.id) }

    fun getProgress(): Int {
        val unlocked = allAchievements.count { isUnlocked(it.id) }
        return (unlocked * 100) / allAchievements.size
    }
}
