package com.vibe.ui.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vibe.ui.R
import com.vibe.ui.VibeActivity
import com.vibe.ui.security.IntegrityVerifier
import com.vibe.ui.security.SecureKeyManager
import com.vibe.common.logging.VibeLogger

/**
 * Vibe Splash Screen - Premium Launch Experience
 * 
 * Implementation per ТЗ №02:
 * - Phase 1: Dark Space (0-100ms)
 * - Phase 2: Signal (100-250ms)
 * - Phase 3: V Formation (250-500ms)
 * - Phase 4: Vibe Pulse (500-600ms)
 * - Phase 5: Brand Reveal (600-750ms)
 * - Phase 6: Transition (750-900ms)
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        private const val PREFS_NAME = "vibe_app_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch_done"
        
        // Animation timings (in ms)
        private const val PHASE_1_DURATION = 100L   // Dark Space
        private const val PHASE_2_DURATION = 150L   // Signal
        private const val PHASE_3_DURATION = 250L   // V Formation
        private const val PHASE_4_DURATION = 100L   // Vibe Pulse
        private const val PHASE_5_DURATION = 150L   // Brand Reveal
        private const val PHASE_6_DURATION = 150L   // Transition
        
        // Returning user timings (faster)
        private const val FAST_SPLASH_DURATION = 400L
    }

    private lateinit var logoContainer: FrameLayout
    private lateinit var vLogo: View
    private lateinit var glowView: View
    private lateinit var appName: TextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var statusText: TextView

    private var isFirstLaunch = true
    private val handler = Handler(Looper.getMainLooper())
    private var navigationRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vibe_screen_splash_new)

        // Initialize views
        logoContainer = findViewById(R.id.logo_container)
        vLogo = findViewById(R.id.v_logo)
        glowView = findViewById(R.id.glow_view)
        appName = findViewById(R.id.app_name)
        loadingIndicator = findViewById(R.id.loading_indicator)
        statusText = findViewById(R.id.status_text)

        // Check if first launch
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        isFirstLaunch = !prefs.getBoolean(KEY_FIRST_LAUNCH, false)

        // Start splash animation
        startSplashAnimation()
    }

    private fun startSplashAnimation() {
        // Hide all elements initially
        vLogo.alpha = 0f
        vLogo.scaleX = 0.8f
        vLogo.scaleY = 0.8f
        glowView.alpha = 0f
        appName.alpha = 0f
        loadingIndicator.alpha = 0f
        statusText.alpha = 0f

        if (isFirstLaunch) {
            // Full animation for first launch
            performFullAnimation()
        } else {
            // Fast animation for returning users
            performFastAnimation()
        }
    }

    private fun performFullAnimation() {
        var delay = 0L

        // Phase 1: Dark Space (already dark background)
        delay += PHASE_1_DURATION

        // Phase 2: Signal - Glow appears
        handler.postDelayed({
            animateGlowIn()
        }, delay)
        delay += PHASE_2_DURATION

        // Phase 3: V Formation - Logo appears and scales
        handler.postDelayed({
            animateVFormation()
        }, delay)
        delay += PHASE_3_DURATION

        // Phase 4: Vibe Pulse - Brief scale pulse
        handler.postDelayed({
            animateVibePulse()
        }, delay)
        delay += PHASE_4_DURATION

        // Phase 5: Brand Reveal - "Vibe" text appears
        handler.postDelayed({
            animateBrandReveal()
        }, delay)
        delay += PHASE_5_DURATION

        // Phase 6: Check state and transition
        handler.postDelayed({
            checkAppStateAndTransition()
        }, delay)
    }

    private fun performFastAnimation() {
        // Faster animation for returning users
        handler.postDelayed({
            // Quick glow + logo appearance
            glowView.animate()
                .alpha(0.3f)
                .setDuration(100)
                .setInterpolator(DecelerateInterpolator())
                .start()

            vLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    // Quick transition
                    checkAppStateAndTransition()
                }
                .start()
        }, 50)
    }

    private fun animateGlowIn() {
        glowView.animate()
            .alpha(0.15f)
            .setDuration(PHASE_2_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateVFormation() {
        // Logo appears with scale animation
        vLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(PHASE_3_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Glow intensifies slightly
        glowView.animate()
            .alpha(0.2f)
            .setDuration(PHASE_3_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateVibePulse() {
        // Subtle scale pulse - Vibe activated
        val pulseUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(vLogo, "scaleX", 1f, 1.03f),
                ObjectAnimator.ofFloat(vLogo, "scaleY", 1f, 1.03f),
                ObjectAnimator.ofFloat(glowView, "alpha", 0.2f, 0.3f)
            )
            duration = PHASE_4_DURATION / 2
            interpolator = OvershootInterpolator(1.1f)
        }

        val pulseDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(vLogo, "scaleX", 1.03f, 1f),
                ObjectAnimator.ofFloat(vLogo, "scaleY", 1.03f, 1f),
                ObjectAnimator.ofFloat(glowView, "alpha", 0.3f, 0.15f)
            )
            duration = PHASE_4_DURATION / 2
            interpolator = DecelerateInterpolator()
        }

        AnimatorSet().apply {
            playSequentially(pulseUp, pulseDown)
            start()
        }
    }

    private fun animateBrandReveal() {
        // "Vibe" text appears with fade-in
        appName.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(PHASE_5_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun checkAppStateAndTransition() {
        // Check app state and determine next screen
        val secureKeyManager = SecureKeyManager(this)
        val integrityVerifier = IntegrityVerifier(this)

        // Check integrity
        val integrity = integrityVerifier.performFullCheck()
        if (!integrity.isSecure) {
            VibeLogger.w(TAG, "Integrity warnings: ${integrity.getWarnings()}")
        }

        // Determine next screen based on state
        val nextScreen = determineNextScreen(secureKeyManager)

        // Transition to next screen
        transitionToScreen(nextScreen)
    }

    private fun determineNextScreen(secureKeyManager: SecureKeyManager): NextScreen {
        // Demo mode: always go to main Vibe UI (Compose) without server
        return NextScreen.OFFLINE_MAIN
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun transitionToScreen(screen: NextScreen) {
        // Fade out current view
        val contentView = findViewById<View>(android.R.id.content)
        contentView.animate()
            .alpha(0f)
            .setDuration(150)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                // Mark first launch as done
                if (isFirstLaunch) {
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_FIRST_LAUNCH, true)
                        .apply()
                }

                // Navigate to appropriate screen
                val intent = when (screen) {
                    NextScreen.WELCOME -> Intent(this, WelcomeActivity::class.java)
                    NextScreen.LOGIN -> Intent(this, AuthActivity::class.java)
                    NextScreen.MAIN -> Intent(this, VibeActivity::class.java)
                    NextScreen.OFFLINE_MAIN -> Intent(this, VibeActivity::class.java).apply {
                        putExtra("offline_mode", true)
                    }
                    NextScreen.OFFLINE_ERROR -> Intent(this, OfflineErrorActivity::class.java)
                }

                startActivity(intent)
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
                finish()
            }
            .start()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        navigationRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }

    enum class NextScreen {
        WELCOME,
        LOGIN,
        MAIN,
        OFFLINE_MAIN,
        OFFLINE_ERROR
    }
}
