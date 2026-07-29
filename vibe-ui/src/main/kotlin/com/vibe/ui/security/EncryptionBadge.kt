package com.vibe.ui.security

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.vibe.ui.R

/**
 * Badge showing encryption status.
 */
class EncryptionBadge @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val statusText: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.vibe_badge_encryption, this, true)
        statusText = findViewById(R.id.encryption_status)
    }

    fun setEncrypted(encrypted: Boolean) {
        if (encrypted) {
            statusText.text = "🔒 E2EE"
            statusText.setTextColor(0xFF4CAF50.toInt())
        } else {
            statusText.text = "🔓"
            statusText.setTextColor(0xFFFFC107.toInt())
        }
    }
}
