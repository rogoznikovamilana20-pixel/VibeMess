package com.vibe.ui.ai

/**
 * Sealed class for AI-related exceptions.
 */
sealed class AiException(message: String) : Exception(message) {

    /**
     * Rate limit exceeded (HTTP 429).
     */
    data class RateLimit(
        val retryAfter: Long? = null
    ) : AiException("Rate limit exceeded. Retry after ${retryAfter ?: "unknown"} seconds")

    /**
     * Authentication failed (HTTP 401/403).
     */
    data class Auth(
        val statusCode: Int
    ) : AiException("Authentication failed: $statusCode")

    /**
     * Network error (connection timeout, DNS failure, etc).
     */
    data class Network(
        val errorMessage: String
    ) : AiException(errorMessage)

    /**
     * Invalid response format.
     */
    data class InvalidResponse(
        val errorDetails: String
    ) : AiException("Invalid response: $errorDetails")

    /**
     * Model not available.
     */
    data class ModelNotAvailable(
        val model: String
    ) : AiException("Model not available: $model")

    /**
     * Token limit exceeded.
     */
    data class TokenLimitExceeded(
        val maxTokens: Int,
        val requestedTokens: Int
    ) : AiException("Token limit exceeded: $requestedTokens > $maxTokens")
}
