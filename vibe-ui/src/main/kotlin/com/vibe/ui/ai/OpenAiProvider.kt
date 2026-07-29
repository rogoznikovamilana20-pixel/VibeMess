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

/**
 * OpenAI API provider implementation.
 */
class OpenAiProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1"
) : AiProvider {

    override val name: String = "OpenAI"
    override val isAvailable: Boolean get() = apiKey.isNotBlank()

    override fun getDefaultModel(): String = "gpt-4o-mini"

    override fun getAvailableModels(): List<String> = listOf(
        "gpt-4o",
        "gpt-4o-mini",
        "gpt-4-turbo",
        "gpt-3.5-turbo"
    )

    override suspend fun chat(
        messages: List<AiMessage>,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): AiResponse = withContext(Dispatchers.IO) {
        val requestBody = JSONObject().apply {
            put("model", model)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            put("messages", JSONArray().apply {
                messages.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role.name.lowercase())
                        put("content", msg.content)
                    })
                }
            })
        }

        val url = URL("$baseUrl/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 60000
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
            }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                throw AiException.Network("OpenAI API error: $responseCode")
            }

            val jsonResponse = JSONObject(responseBody)
            val choice = jsonResponse.getJSONArray("choices").getJSONObject(0)
            val message = choice.getJSONObject("message")
            val usage = jsonResponse.optJSONObject("usage")

            AiResponse(
                content = message.getString("content"),
                model = jsonResponse.getString("model"),
                usage = usage?.let {
                    AiUsage(
                        promptTokens = it.getInt("prompt_tokens"),
                        completionTokens = it.getInt("completion_tokens"),
                        totalTokens = it.getInt("total_tokens")
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
        val requestBody = JSONObject().apply {
            put("model", model)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            put("stream", true)
            put("messages", JSONArray().apply {
                messages.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role.name.lowercase())
                        put("content", msg.content)
                    })
                }
            })
        }

        val url = URL("$baseUrl/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 120000
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
            }

            val reader = connection.inputStream.bufferedReader()

            reader.use { bufferedReader ->
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    if (currentLine.startsWith("data: ")) {
                        val data = currentLine.removePrefix("data: ").trim()
                        if (data == "[DONE]") break

                        try {
                            val chunk = JSONObject(data)
                            val delta = chunk.getJSONArray("choices")
                                .getJSONObject(0)
                                .optJSONObject("delta")
                                ?.optString("content", "") ?: ""

                            if (delta.isNotEmpty()) {
                                emit(AiStreamChunk(
                                    delta = delta,
                                    isComplete = false,
                                    usage = null
                                ))
                            }
                        } catch (e: Exception) {
                            // Skip malformed chunks
                        }
                    }
                }
            }

            emit(AiStreamChunk(delta = "", isComplete = true, usage = null))
        } finally {
            connection.disconnect()
        }
    }.flowOn(Dispatchers.IO)
}
