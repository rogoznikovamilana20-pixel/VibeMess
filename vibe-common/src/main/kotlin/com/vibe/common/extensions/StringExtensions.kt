package com.vibe.common.extensions

/**
 * Returns true if the string is a valid email address.
 */
fun String.isValidEmail(): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return matches(emailRegex)
}

/**
 * Capitalizes the first letter of the string.
 */
fun String.capitalizeFirst(): String {
    if (isEmpty()) return this
    return substring(0, 1).uppercase() + substring(1)
}

/**
 * Truncates the string to a maximum length and adds an ellipsis if truncated.
 */
fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
    if (length <= maxLength) return this
    return substring(0, maxLength - ellipsis.length) + ellipsis
}
