package com.vibe.ui.security

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vibe.ui.R
import java.security.MessageDigest

/**
 * Activity for verifying encryption keys with a peer.
 */
class VerificationActivity : AppCompatActivity() {

    private lateinit var keyManager: KeyManager
    private lateinit var fingerprintText: TextView
    private lateinit var peerFingerprintInput: EditText
    private lateinit var verificationStatus: LinearLayout
    private lateinit var verificationIcon: TextView
    private lateinit var verificationText: TextView
    private lateinit var btnVerify: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vibe_screen_verification)

        keyManager = KeyManager(this)

        fingerprintText = findViewById(R.id.key_fingerprint)
        peerFingerprintInput = findViewById(R.id.peer_fingerprint_input)
        verificationStatus = findViewById(R.id.verification_status)
        verificationIcon = findViewById(R.id.verification_icon)
        verificationText = findViewById(R.id.verification_text)
        btnVerify = findViewById(R.id.btn_verify)

        // Display our key fingerprint
        val fingerprint = keyManager.getKeyFingerprint()
        fingerprintText.text = formatFingerprint(fingerprint)

        btnVerify.setOnClickListener {
            verifyFingerprint()
        }
    }

    private fun formatFingerprint(fingerprint: String): String {
        // Format as XX:XX:XX:XX groups
        return fingerprint.chunked(2).joinToString(":")
    }

    private fun verifyFingerprint() {
        val peerFingerprint = peerFingerprintInput.text.toString()
            .replace(":", "")
            .replace(" ", "")
            .uppercase()

        val ourFingerprint = keyManager.getKeyFingerprint()

        verificationStatus.visibility = View.VISIBLE

        // Use constant-time comparison to prevent timing attacks
        if (constantTimeEquals(peerFingerprint, ourFingerprint)) {
            verificationIcon.text = "✅"
            verificationText.text = "Ключи совпадают!"
            verificationText.setTextColor(0xFF4CAF50.toInt())
        } else if (peerFingerprint.length == ourFingerprint.length) {
            verificationIcon.text = "⚠️"
            verificationText.text = "Ключи не совпадают!"
            verificationText.setTextColor(0xFFFF5252.toInt())
        } else {
            verificationIcon.text = "❌"
            verificationText.text = "Неверная длина отпечатка"
            verificationText.setTextColor(0xFFFF5252.toInt())
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        return MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
    }
}
