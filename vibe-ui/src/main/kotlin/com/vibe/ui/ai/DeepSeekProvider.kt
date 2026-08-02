package com.vibe.ui.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class DeepSeekProvider(
    private val apiKey: String = "",
    private val baseUrl: String = "https://api.deepseek.com/v1"
) : AiProvider {

    override val name: String = "DeepSeek"
    override val isAvailable: Boolean = true

    override fun getDefaultModel(): String = "deepseek-chat"

    override fun getAvailableModels(): List<String> = listOf(
        "deepseek-chat",
        "deepseek-reasoner"
    )

    override suspend fun chat(
        messages: List<AiMessage>,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): AiResponse = withContext(Dispatchers.IO) {
        val payload = buildPayload(messages, model, temperature, maxTokens)
        val url = URL("$baseUrl/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 120000
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                val msg = when (responseCode) {
                    401 -> "Неверный API-ключ. Проверьте AI_API_KEY в настройках"
                    402 -> "Недостаточно средств на аккаунте DeepSeek"
                    429 -> "Слишком много запросов. Попробуйте позже"
                    503 -> "Сервис временно недоступен. Попробуйте позже"
                    else -> "Ошибка $responseCode: $error"
                }
                throw AiException.Network(msg)
            }

            val responseBody = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(responseBody)
            val choice = json.getJSONArray("choices").getJSONObject(0)
            val text = choice.getJSONObject("message").getString("content")
            val usage = json.optJSONObject("usage")

            AiResponse(
                content = text,
                model = json.optString("model", model),
                usage = usage?.let {
                    AiUsage(
                        promptTokens = it.optInt("prompt_tokens", 0),
                        completionTokens = it.optInt("completion_tokens", 0),
                        totalTokens = it.optInt("total_tokens", 0)
                    )
                }
            )
        } finally {
            connection.disconnect()
        }
    }

    override fun stream(
        messages: List<AiMessage>,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): Flow<AiStreamChunk> = flow {
        val payload = buildPayload(messages, model, temperature, maxTokens)
        payload.put("stream", true)

        val url = URL("$baseUrl/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Accept", "text/event-stream")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 0
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                val msg = when (responseCode) {
                    401 -> "Неверный API-ключ DeepSeek"
                    402 -> "Недостаточно средств на аккаунте DeepSeek"
                    429 -> "Слишком много запросов"
                    503 -> "Сервис временно недоступен"
                    else -> "Ошибка $responseCode: $error"
                }
                throw AiException.Network(msg)
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            while (true) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ")
                if (data == "[DONE]") break

                try {
                    val json = JSONObject(data)
                    val choice = json.optJSONArray("choices")?.optJSONObject(0) ?: continue
                    val delta = choice.optJSONObject("delta")
                    val text = delta?.optString("content", "") ?: ""
                    if (text.isNotEmpty()) {
                        emit(AiStreamChunk(delta = text, isComplete = false, usage = null))
                    }
                } catch (_: Exception) {
                }
            }

            emit(AiStreamChunk(delta = "", isComplete = true, usage = null))
        } catch (e: Exception) {
            if (e is AiException) throw e
            throw AiException.Network("Ошибка соединения с DeepSeek: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun buildPayload(
        messages: List<AiMessage>,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): JSONObject {
        val jsonMessages = JSONArray()
        for (msg in messages) {
            val role = when (msg.role) {
                AiRole.SYSTEM -> "system"
                AiRole.USER -> "user"
                AiRole.ASSISTANT -> "assistant"
            }
            jsonMessages.put(JSONObject().apply {
                put("role", role)
                put("content", msg.content)
            })
        }

        return JSONObject().apply {
            put("model", model)
            put("messages", jsonMessages)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
        }
    }
}
