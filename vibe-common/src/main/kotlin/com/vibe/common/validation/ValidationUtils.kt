package com.vibe.common.validation

/**
 * Utility class for input validation across the Vibe project.
 * Provides common validation functions with clear error messages.
 */
object ValidationUtils {

    /**
     * Validate chat ID.
     * @param chatId The chat ID to validate
     * @return true if valid, false otherwise
     */
    fun isValidChatId(chatId: Long): Boolean {
        return chatId != 0L
    }

    /**
     * Validate user ID.
     * @param userId The user ID to validate
     * @return true if valid, false otherwise
     */
    fun isValidUserId(userId: Long): Boolean {
        return userId > 0
    }

    /**
     * Validate message ID.
     * @param messageId The message ID to validate
     * @return true if valid, false otherwise
     */
    fun isValidMessageId(messageId: Long): Boolean {
        return messageId > 0
    }

    /**
     * Validate message text.
     * @param text The message text to validate
     * @param maxLength Maximum allowed length (default 4096 for Telegram)
     * @return true if valid, false otherwise
     */
    fun isValidMessageText(text: String?, maxLength: Int = 4096): Boolean {
        if (text == null) return false
        if (text.isEmpty()) return false
        if (text.length > maxLength) return false
        return true
    }

    /**
     * Validate chat title.
     * @param title The chat title to validate
     * @param maxLength Maximum allowed length (default 128)
     * @return true if valid, false otherwise
     */
    fun isValidChatTitle(title: String?, maxLength: Int = 128): Boolean {
        if (title == null) return false
        if (title.isEmpty()) return false
        if (title.length > maxLength) return false
        return true
    }

    /**
     * Validate username.
     * @param username The username to validate
     * @return true if valid, false otherwise
     */
    fun isValidUsername(username: String?): Boolean {
        if (username == null) return false
        if (username.isEmpty()) return false
        if (username.length > 32) return false
        // Username should start with @ and contain only alphanumeric characters and underscores
        val usernamePattern = Regex("^@[a-zA-Z0-9_]{5,32}$")
        return usernamePattern.matches(username)
    }

    /**
     * Validate phone number.
     * @param phone The phone number to validate
     * @return true if valid, false otherwise
     */
    fun isValidPhoneNumber(phone: String?): Boolean {
        if (phone == null) return false
        if (phone.isEmpty()) return false
        // Basic phone validation - should start with + and contain only digits
        val phonePattern = Regex("^\\+[1-9]\\d{1,14}$")
        return phonePattern.matches(phone)
    }

    /**
     * Validate URL.
     * @param url The URL to validate
     * @return true if valid, false otherwise
     */
    fun isValidUrl(url: String?): Boolean {
        if (url == null) return false
        if (url.isEmpty()) return false
        val urlPattern = Regex("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$")
        return urlPattern.matches(url)
    }

    /**
     * Validate file path.
     * @param path The file path to validate
     * @return true if valid, false otherwise
     */
    fun isValidFilePath(path: String?): Boolean {
        if (path == null) return false
        if (path.isEmpty()) return false
        if (path.length > 255) return false
        // Basic path validation - should not contain invalid characters
        val invalidChars = Regex("[<>:\"|?*]")
        return !invalidChars.containsMatchIn(path)
    }

    /**
     * Sanitize string input by removing potentially dangerous characters.
     * @param input The input string to sanitize
     * @return Sanitized string
     */
    fun sanitizeString(input: String): String {
        // Remove null bytes and control characters (except tab, newline, carriage return)
        return input.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
    }

    /**
     * Validate and throw exception if invalid.
     * @param isValid The validation result
     * @param errorMessage The error message to throw if invalid
     * @throws IllegalArgumentException if validation fails
     */
    fun requireValid(isValid: Boolean, errorMessage: String) {
        if (!isValid) {
            throw IllegalArgumentException(errorMessage)
        }
    }
}
