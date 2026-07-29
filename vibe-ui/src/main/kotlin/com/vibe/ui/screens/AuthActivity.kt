package com.vibe.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vibe.ui.R
import com.vibe.ui.VibeActivity
import com.vibe.ui.network.ServerConfig
import com.vibe.ui.network.VibeHttpClient
import kotlinx.coroutines.launch

/**
 * Auth Screen - Login/Registration
 * 
 * Per TЗ §5:
 * - Phone number input
 * - OTP verification
 * - Social login (Google, VK, Telegram)
 * - Loading/error states
 * - Smooth animations
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var serverConfig: ServerConfig
    private lateinit var httpClient: VibeHttpClient
    private var currentStep = Step.PHONE
    private var phoneNumber = ""

    enum class Step {
        PHONE,
        OTP,
        LOADING
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vibe_screen_auth)

        serverConfig = ServerConfig(this)
        httpClient = VibeHttpClient(serverConfig)

        // Check if already authenticated
        if (serverConfig.isAuthenticated()) {
            navigateToMain()
            return
        }

        showPhoneStep()
    }

    private fun showPhoneStep() {
        currentStep = Step.PHONE

        val phoneInput = findViewById<EditText>(R.id.input_phone)
        val btnSubmit = findViewById<LinearLayout>(R.id.btn_submit)
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val socialLogin = findViewById<LinearLayout>(R.id.social_login)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        phoneInput.visibility = View.VISIBLE
        btnSubmit.visibility = View.VISIBLE
        socialLogin.visibility = View.VISIBLE
        progressBar.visibility = View.GONE

        // Animate entrance
        phoneInput.alpha = 0f
        phoneInput.translationY = 20f
        phoneInput.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        btnBack.setOnClickListener { finish() }

        btnSubmit.setOnClickListener {
            phoneNumber = phoneInput.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                requestVerificationCode()
            } else {
                Toast.makeText(this, "Введите номер телефона", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestVerificationCode() {
        showLoading()

        lifecycleScope.launch {
            try {
                val code = httpClient.requestVerificationCode(phoneNumber)
                if (code != null) {
                    showOtpStep()
                    Toast.makeText(this@AuthActivity, "Код: $code", Toast.LENGTH_LONG).show()
                } else {
                    showError("Не удалось отправить код")
                    showPhoneStep()
                }
            } catch (e: Exception) {
                showError("Ошибка подключения: ${e.message}")
                showPhoneStep()
            }
        }
    }

    private fun showOtpStep() {
        currentStep = Step.OTP

        val phoneInput = findViewById<EditText>(R.id.input_phone)
        val otpContainer = findViewById<LinearLayout>(R.id.otp_container)
        val btnSubmit = findViewById<LinearLayout>(R.id.btn_submit)
        val socialLogin = findViewById<LinearLayout>(R.id.social_login)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        phoneInput.visibility = View.GONE
        socialLogin.visibility = View.GONE
        otpContainer.visibility = View.VISIBLE
        progressBar.visibility = View.GONE

        // Animate OTP fields
        for (i in 0 until otpContainer.childCount) {
            val child = otpContainer.getChildAt(i)
            child.alpha = 0f
            child.translationX = 20f
            child.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(300)
                .setStartDelay((i * 100).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        btnSubmit.setOnClickListener {
            val otpContainer = findViewById<LinearLayout>(R.id.otp_container)
            val code = (0 until otpContainer.childCount).joinToString("") { index ->
                val editText = otpContainer.getChildAt(index) as? EditText
                editText?.text.toString()
            }

            if (code.length == 6) {
                verifyCode(code)
            } else {
                Toast.makeText(this, "Введите 6-значный код", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyCode(code: String) {
        showLoading()

        lifecycleScope.launch {
            try {
                val result = httpClient.verifyCode(phoneNumber, code)
                if (result.success) {
                    // Store auth data
                    serverConfig.setUserId(result.userId)
                    serverConfig.setAuthToken(result.token)
                    serverConfig.setVibeId(result.vibeId)
                    serverConfig.setAuthenticated(true)

                    Toast.makeText(this@AuthActivity, "Добро пожаловать в Vibe!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    showError(result.error ?: "Ошибка верификации")
                    showOtpStep()
                }
            } catch (e: Exception) {
                showError("Ошибка подключения: ${e.message}")
                showOtpStep()
            }
        }
    }

    private fun showLoading() {
        currentStep = Step.LOADING
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val btnSubmit = findViewById<LinearLayout>(R.id.btn_submit)

        progressBar.visibility = View.VISIBLE
        btnSubmit.visibility = View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMain() {
        val intent = Intent(this, VibeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
