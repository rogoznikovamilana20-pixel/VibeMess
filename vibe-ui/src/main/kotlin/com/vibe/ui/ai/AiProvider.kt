package com.vibe.ui.ai

import kotlinx.coroutines.flow.Flow

/**
 * Interface for AI providers (OpenAI, Anthropic, etc.)
 */
interface AiProvider {
    val name: String
    val isAvailable: Boolean

    suspend fun chat(
        messages: List<AiMessage>,
        model: String = getDefaultModel(),
        temperature: Double = 0.7,
        maxTokens: Int = 2048
    ): AiResponse

    fun stream(
        messages: List<AiMessage>,
        model: String = getDefaultModel(),
        temperature: Double = 0.7,
        maxTokens: Int = 2048
    ): Flow<AiStreamChunk>

    fun getDefaultModel(): String
    fun getAvailableModels(): List<String>
}

data class AiMessage(
    val role: AiRole,
    val content: String
)

enum class AiRole {
    SYSTEM,
    USER,
    ASSISTANT
}

data class AiResponse(
    val content: String,
    val model: String,
    val usage: AiUsage?
)

data class AiUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

data class AiStreamChunk(
    val delta: String,
    val isComplete: Boolean,
    val usage: AiUsage?
)
