package com.vibe.common.result

/**
 * A sealed class representing the result of an operation.
 * @param T The type of the data returned on success.
 */
sealed class VibeResult<out T> {
    
    /**
     * Represents a successful operation.
     * @property data The result data.
     */
    data class Success<out T>(val data: T) : VibeResult<T>()
    
    /**
     * Represents a failed operation.
     * @property error The exception that caused the failure.
     */
    data class Failure(val error: Throwable) : VibeResult<Nothing>()

    /**
     * Returns true if the result is [Success].
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Returns true if the result is [Failure].
     */
    val isFailure: Boolean get() = this is Failure

    /**
     * Returns the data if the result is [Success], or null otherwise.
     */
    fun getOrNull(): T? = (this as? Success)?.data

    /**
     * Returns the error if the result is [Failure], or null otherwise.
     */
    fun exceptionOrNull(): Throwable? = (this as? Failure)?.error
}
