package com.vibe.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vibe.ui.R
import com.vibe.ui.components.VibeButton

/**
 * Welcome Screen - Onboarding
 * 
 * Per TЗ §5:
 * - Logo Vibe (not just mascot)
 * - "Добро пожаловать в Vibe"
 * - "Общение. AI. Люди. Всё в одном пространстве."
 * - Create account + Login buttons
 * - Smooth animations
 */
class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vibe_screen_welcome)

        val logoContainer = findViewById<FrameLayout>(R.id.logo_container)
        val vLogo = findViewById<ImageView>(R.id.v_logo)
        val welcomeTitle = findViewById<TextView>(R.id.welcome_title)
        val welcomeSubtitle = findViewById<TextView>(R.id.welcome_subtitle)
        val btnCreateAccount = findViewById<VibeButton>(R.id.btn_create_account)
        val btnLogin = findViewById<VibeButton>(R.id.btn_login)

        // Setup buttons per TЗ
        btnCreateAccount.setText("Создать аккаунт")
        btnCreateAccount.setVariant(VibeButton.Variant.PRIMARY)
        btnLogin.setText("Войти")
        btnLogin.setVariant(VibeButton.Variant.OUTLINE)

        // Animate entrance per TЗ §5
        animateEntrance(logoContainer, vLogo, welcomeTitle, welcomeSubtitle, btnCreateAccount, btnLogin)

        btnCreateAccount.setOnClickListener {
            startActivity(Intent(this, AuthActivity::class.java))
        }

        btnLogin.setOnClickListener {
            startActivity(Intent(this, AuthActivity::class.java))
        }
    }

    private fun animateEntrance(
        logoContainer: FrameLayout,
        vLogo: ImageView,
        welcomeTitle: TextView,
        welcomeSubtitle: TextView,
        btnCreate: VibeButton,
        btnLogin: VibeButton
    ) {
        // Logo appears first
        logoContainer.alpha = 0f
        logoContainer.scaleX = 0.8f
        logoContainer.scaleY = 0.8f
        logoContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        // Title appears
        welcomeTitle.alpha = 0f
        welcomeTitle.translationY = 20f
        welcomeTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(200)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Subtitle appears
        welcomeSubtitle.alpha = 0f
        welcomeSubtitle.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Buttons appear
        btnCreate.alpha = 0f
        btnCreate.translationY = 20f
        btnCreate.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(600)
            .setInterpolator(DecelerateInterpolator())
            .start()

        btnLogin.alpha = 0f
        btnLogin.translationY = 20f
        btnLogin.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(700)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
}
