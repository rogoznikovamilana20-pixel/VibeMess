package com.vibe.ui.e2e

import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Trust management for contacts.
 * Uses TOFU (Trust On First Use) with optional manual verification.
 *
 * Flow:
 * 1. First message exchange → auto-trust (TOFU)
 * 2. Key changes → subtle notification (not blocking)
 * 3. User can manually verify via QR/Safety Number if they want
 *
 * Trust state is persisted via SharedPreferences and restored on app restart.
 */
class TrustManager(context: Context? = null) {

    companion object {
        private const val TAG = "TrustManager"
        private const val PREFS_NAME = "vibe_trust_data"
        private const val KEY_TRUST_DATA = "trusted_keys"
    }

    private val trustedKeys = ConcurrentHashMap<String, TrustedKey>()
    private val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        loadFromPrefs()
    }

    fun isTrusted(contactUserId: String, identityKey: ByteArray): TrustState {
        val existing = trustedKeys[contactUserId]

        return when {
            existing == null -> {
                trustKey(contactUserId, identityKey)
                TrustState.TRUSTED_FIRST_TIME
            }
            existing.identityKey.contentEquals(identityKey) -> {
                TrustState.TRUSTED
            }
            else -> {
                Log.w(TAG, "Key changed for $contactUserId - possible MITM or re-registration")
                TrustState.KEY_CHANGED
            }
        }
    }

    fun trustKey(contactUserId: String, identityKey: ByteArray): Boolean {
        trustedKeys[contactUserId] = TrustedKey(
            identityKey = identityKey.copyOf(),
            trustedAt = System.currentTimeMillis(),
            verified = false
        )
        saveToPrefs()
        Log.d(TAG, "Key trusted for $contactUserId")
        return true
    }

    fun manuallyVerify(contactUserId: String): Boolean {
        val existing = trustedKeys[contactUserId] ?: return false
        trustedKeys[contactUserId] = existing.copy(verified = true)
        saveToPrefs()
        Log.d(TAG, "Key manually verified for $contactUserId")
        return true
    }

    fun getTrustStatus(contactUserId: String): TrustStatus {
        val trusted = trustedKeys[contactUserId] ?: return TrustStatus(
            state = TrustState.UNKNOWN,
            message = "No key"
        )

        return when {
            trusted.verified -> TrustStatus(
                state = TrustState.VERIFIED,
                message = "Manually verified"
            )
            else -> TrustStatus(
                state = TrustState.TRUSTED,
                message = "Auto-trusted (TOFU)"
            )
        }
    }

    fun getSafetyNumber(contactUserId: String): String? {
        val trusted = trustedKeys[contactUserId] ?: return null
        val hex = CryptoUtils.toHex(CryptoUtils.sha256(trusted.identityKey))
        return hex.chunked(5).joinToString(" ")
    }

    fun hasKeyChanged(contactUserId: String, currentKey: ByteArray): Boolean {
        val existing = trustedKeys[contactUserId] ?: return false
        return !existing.identityKey.contentEquals(currentKey)
    }

    fun exportTrustData(): String {
        val json = JSONObject()
        trustedKeys.forEach { (userId, trusted) ->
            json.put(userId, JSONObject().apply {
                put("identity_key", Base64.encodeToString(trusted.identityKey, Base64.NO_WRAP))
                put("trusted_at", trusted.trustedAt)
                put("verified", trusted.verified)
            })
        }
        return json.toString()
    }

    fun importTrustData(data: String) {
        try {
            val json = JSONObject(data)
            json.keys().forEach { userId ->
                val obj = json.getJSONObject(userId)
                trustedKeys[userId] = TrustedKey(
                    identityKey = Base64.decode(obj.getString("identity_key"), Base64.NO_WRAP),
                    trustedAt = obj.getLong("trusted_at"),
                    verified = obj.getBoolean("verified")
                )
            }
            saveToPrefs()
            Log.d(TAG, "Trust data imported: ${trustedKeys.size} contacts")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import trust data", e)
        }
    }

    fun clearAll() {
        trustedKeys.clear()
        prefs?.edit()?.remove(KEY_TRUST_DATA)?.apply()
        Log.d(TAG, "All trust data cleared")
    }

    fun getTrustedKey(contactUserId: String): ByteArray? {
        return trustedKeys[contactUserId]?.identityKey
    }

    private fun saveToPrefs() {
        try {
            prefs?.edit()?.putString(KEY_TRUST_DATA, exportTrustData())?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save trust data", e)
        }
    }

    private fun loadFromPrefs() {
        try {
            val data = prefs?.getString(KEY_TRUST_DATA, null) ?: return
            val json = JSONObject(data)
            json.keys().forEach { userId ->
                val obj = json.getJSONObject(userId)
                trustedKeys[userId] = TrustedKey(
                    identityKey = Base64.decode(obj.getString("identity_key"), Base64.NO_WRAP),
                    trustedAt = obj.getLong("trusted_at"),
                    verified = obj.getBoolean("verified")
                )
            }
            Log.d(TAG, "Trust data loaded: ${trustedKeys.size} contacts")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load trust data", e)
        }
    }
}

/**
 * Trust state for a contact.
 */
enum class TrustState {
    UNKNOWN,           // No key seen yet
    TRUSTED_FIRST_TIME, // Auto-trusted on first contact (TOFU)
    TRUSTED,           // Key matches stored key
    KEY_CHANGED,       // Key changed - suspicious
    VERIFIED           // Manually verified by user
}

/**
 * Trusted key record.
 */
data class TrustedKey(
    val identityKey: ByteArray,
    val trustedAt: Long,
    val verified: Boolean
)

/**
 * Trust status for UI display.
 */
data class TrustStatus(
    val state: TrustState,
    val message: String
)
