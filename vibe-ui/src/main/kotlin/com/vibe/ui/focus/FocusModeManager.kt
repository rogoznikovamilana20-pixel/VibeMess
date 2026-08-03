package com.vibe.ui.focus

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FocusSpace(val label: String, val accent: Long) {
    PERSONAL("Личное", 0xFF8D2BFA),
    WORK("Работа", 0xFF10B6FA)
}

object FocusModeManager {

    private var prefs: SharedPreferences? = null
    private val _currentSpace = MutableStateFlow(FocusSpace.PERSONAL)
    val currentSpace: StateFlow<FocusSpace> = _currentSpace.asStateFlow()

    private val _personalUnread = MutableStateFlow(0)
    val personalUnread: StateFlow<Int> = _personalUnread.asStateFlow()

    private val _workUnread = MutableStateFlow(0)
    val workUnread: StateFlow<Int> = _workUnread.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("vibe_focus_mode", Context.MODE_PRIVATE)
        val saved = prefs?.getString("current_space", FocusSpace.PERSONAL.name)
        _currentSpace.value = try {
            FocusSpace.valueOf(saved ?: FocusSpace.PERSONAL.name)
        } catch (_: Exception) {
            FocusSpace.PERSONAL
        }
    }

    fun switchSpace(space: FocusSpace) {
        _currentSpace.value = space
        prefs?.edit()?.putString("current_space", space.name)?.apply()
    }

    fun toggleSpace() {
        val next = when (_currentSpace.value) {
            FocusSpace.PERSONAL -> FocusSpace.WORK
            FocusSpace.WORK -> FocusSpace.PERSONAL
        }
        switchSpace(next)
    }

    fun updateUnreadCounts(personal: Int, work: Int) {
        _personalUnread.value = personal
        _workUnread.value = work
    }

    fun getAutoReply(): String {
        return when (_currentSpace.value) {
            FocusSpace.PERSONAL -> "Сейчас не на работе, отвечу завтра."
            FocusSpace.WORK -> "Сейчас на работе, отвечу позже."
        }
    }

    fun getSpaceForChat(chatName: String, lastMessage: String): FocusSpace {
        val workKeywords = listOf(
            "встреча", "дедлайн", "проект", "задача", "отчёт", "созвон",
            "согласование", "бюджет", "клиент", "договор", "спринт", "таск",
            "deploy", "mr", "pull request", "jira", "sprint", "standup"
        )
        val combined = "$chatName $lastMessage".lowercase()
        return if (workKeywords.any { combined.contains(it) }) {
            FocusSpace.WORK
        } else {
            FocusSpace.PERSONAL
        }
    }
}
