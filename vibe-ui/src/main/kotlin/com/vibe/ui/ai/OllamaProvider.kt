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
 * Ollama local AI provider implementation.
 * Requires Ollama running locally or at a custom baseUrl.
 * Default model: llama3.1 (free, runs locally).
 */
class OllamaProvider(
    private val baseUrl: String = "http://localhost:11434"
) : AiProvider {

    override val name: String = "Ollama"
    override val isAvailable: Boolean
        get() = try {
            val url = URL("$baseUrl/api/tags")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "GET"
            val code = connection.responseCode
            connection.disconnect()
            code == 200
        } catch (e: Exception) {
            false
        }

    override fun getDefaultModel(): String = "llama3.1"

    override fun getAvailableModels(): List<String> {
        return try {
            val url = URL("$baseUrl/api/tags")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            connection.disconnect()

            val json = JSONObject(response)
            val models = json.getJSONArray("models")
            (0 until models.length()).map { i ->
                models.getJSONObject(i).getString("name")
            }
        } catch (e: Exception) {
            listOf("llama3.1", "mistral", "codellama")
        }
    }

    override suspend fun chat(
        messages: List<AiMessage>,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): AiResponse = withContext(Dispatchers.IO) {
        val requestBody = JSONObject().apply {
            put("model", model)
            put("stream", false)
            put("options", JSONObject().apply {
                put("temperature", temperature)
                put("num_predict", maxTokens)
            })
            put("messages", JSONArray().apply {
                messages.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role.name.lowercase())
                        put("content", msg.content)
                    })
                }
            })
        }

        val url = URL("$baseUrl/api/chat")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 300000
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
            }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                throw AiException.Network("Ollama API error: $responseCode")
            }

            val jsonResponse = JSONObject(responseBody)
            val message = jsonResponse.getJSONObject("message")

            AiResponse(
                content = message.getString("content"),
                model = jsonResponse.getString("model"),
                usage = AiUsage(
                    promptTokens = jsonResponse.optInt("prompt_eval_count", 0),
                    completionTokens = jsonResponse.optInt("eval_count", 0),
                    totalTokens = jsonResponse.optInt("prompt_eval_count", 0) +
                            jsonResponse.optInt("eval_count", 0)
                )
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
            put("stream", true)
            put("options", JSONObject().apply {
                put("temperature", temperature)
                put("num_predict", maxTokens)
            })
            put("messages", JSONArray().apply {
                messages.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role.name.lowercase())
                        put("content", msg.content)
                    })
                }
            })
        }

        val url = URL("$baseUrl/api/chat")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 300000
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
            }

            val reader = connection.inputStream.bufferedReader()

            reader.use { bufferedReader ->
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    if (currentLine.isBlank()) continue

                    try {
                        val chunk = JSONObject(currentLine)
                        val done = chunk.optBoolean("done", false)

                        if (done) {
                            val usage = AiUsage(
                                promptTokens = chunk.optInt("prompt_eval_count", 0),
                                completionTokens = chunk.optInt("eval_count", 0),
                                totalTokens = chunk.optInt("prompt_eval_count", 0) +
                                        chunk.optInt("eval_count", 0)
                            )
                            emit(AiStreamChunk(delta = "", isComplete = true, usage = usage))
                        } else {
                            val content = chunk.optJSONObject("message")
                                ?.optString("content", "") ?: ""

                            if (content.isNotEmpty()) {
                                emit(AiStreamChunk(
                                    delta = content,
                                    isComplete = false,
                                    usage = null
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        // Skip malformed lines
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }.flowOn(Dispatchers.IO)
}
