package com.vibe.common.extensions

/**
 * Returns a new list containing only unique elements based on a selector.
 */
fun <T, K> Iterable<T>.distinctBySelector(selector: (T) -> K): List<T> {
    return distinctBy(selector)
}

/**
 * Returns true if the collection is null or empty.
 */
fun <T> Collection<T>?.isNullOrEmpty(): Boolean {
    return this == null || isEmpty()
}

/**
 * Executes the given block if the collection is not empty.
 */
inline fun <T> Collection<T>.onNotEmpty(block: (Collection<T>) -> Unit) {
    if (isNotEmpty()) {
        block(this)
    }
}
