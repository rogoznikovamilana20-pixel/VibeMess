package com.vibe.bridge.internal.media

/**
 * Internal interface to bridge Java and Kotlin media logic without reflection.
 */
internal interface IMediaRegistry {
    /**
     * Registers a Telegram media object (Document or Photo) to be retrievable by fileId.
     */
    fun registerMedia(fileId: String, account: Int, telegramObject: Any)
}
