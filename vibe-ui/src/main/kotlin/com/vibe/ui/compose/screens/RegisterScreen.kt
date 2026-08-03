package com.vibe.ui.compose.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vibe.ui.compose.components.VibeButton
import com.vibe.ui.compose.components.VibeButtonSize
import com.vibe.ui.data.ProfileRepository
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.AccountEntity
import com.vibe.ui.i18n.VibeI18n
import com.vibe.ui.network.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Locale
import kotlin.random.Random

private fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

private fun randomVibeId(): String {
    val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
    return "v_" + (1..10).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onComplete: (userId: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var registering by remember { mutableStateOf(false) }

    val emailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun validate(): String? {
        if (name.isBlank()) return VibeI18n.t("enter_name")
        if (username.isBlank()) return VibeI18n.t("enter_username")
        if (!username.matches(Regex("^[a-zA-Z0-9_]{3,32}$")))
            return VibeI18n.t("username_hint")
        if (email.isBlank() || !emailValid) return VibeI18n.t("invalid_email")
        if (password.length < 6) return VibeI18n.t("password_short")
        if (password != confirmPassword) return VibeI18n.t("passwords_no_match")
        return null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(VibeI18n.t("register_title"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = VibeI18n.t("create_account_title"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = VibeI18n.t("create_account_desc"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text(VibeI18n.t("first_name")) },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase(Locale.ROOT).replace(" ", ""); error = null },
                label = { Text(VibeI18n.t("username")) },
                leadingIcon = { Icon(Icons.Default.AlternateEmail, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim(); error = null },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Mail, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                isError = email.isNotBlank() && !emailValid,
                supportingText = if (email.isNotBlank() && !emailValid) {
                    { Text(VibeI18n.t("incorrect_email")) }
                } else null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text(VibeI18n.t("password")) },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; error = null },
                label = { Text(VibeI18n.t("confirm_password")) },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            VibeButton(
                text = if (registering) VibeI18n.t("create_account_loading") else VibeI18n.t("register"),
                onClick = {
                    val validationError = validate()
                    if (validationError != null) {
                        error = validationError
                        return@VibeButton
                    }
                    registering = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                val db = VibeDatabase.getDatabase(context)
                                val trimmedEmail = email.trim().lowercase(Locale.ROOT)
                                if (db.accountDao().getByEmail(trimmedEmail) != null) {
                                    error = VibeI18n.t("email_exists")
                                    null
                                } else if (db.accountDao().getByUsername(username) != null) {
                                    error = VibeI18n.t("username_taken")
                                    null
                                } else {
                                    val vibeId = randomVibeId()
                                    val account = AccountEntity(
                                        name = name.trim(),
                                        username = username,
                                        email = trimmedEmail,
                                        passwordHash = sha256(password),
                                        vibeId = vibeId
                                    )
                                    val newId = db.accountDao().insert(account)
                                    newId to vibeId
                                }
                            }.getOrElse { e ->
                                android.util.Log.w("Register", "registration failed", e)
                                error = VibeI18n.t("save_failed")
                                null
                            }
                        }
                        registering = false
                        val pair = result
                        if (pair != null) {
                            val serverConfig = ServerConfig(context)
                            ProfileRepository(context).apply {
                                this.displayName = name.trim()
                                this.username = "@$username"
                                this.vibeId = pair.second
                            }

                            // Register with Supabase Auth to get a token
                            val authResult = com.vibe.ui.network.SupabaseAuthManager.signUp(
                                com.vibe.ui.BuildConfig.SUPABASE_URL,
                                com.vibe.ui.BuildConfig.SUPABASE_ANON_KEY,
                                email.trim().lowercase(Locale.ROOT),
                                password
                            )

                            // If signup failed (e.g. "already registered"), try login
                            val finalResult = if (!authResult.success) {
                                com.vibe.ui.network.SupabaseAuthManager.signIn(
                                    com.vibe.ui.BuildConfig.SUPABASE_URL,
                                    com.vibe.ui.BuildConfig.SUPABASE_ANON_KEY,
                                    email.trim().lowercase(Locale.ROOT),
                                    password
                                )
                            } else authResult

                            if (finalResult.success && finalResult.token != null) {
                                serverConfig.setAuthToken(finalResult.token)
                                finalResult.refreshToken?.let { serverConfig.setRefreshToken(it) }
                                finalResult.userId?.let { serverConfig.setUserId(it) }
                                serverConfig.setAuthenticated(true)

                                // Create profile in Supabase (non-blocking)
                                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                    com.vibe.ui.network.SupabaseClient.createProfile(
                                        com.vibe.ui.BuildConfig.SUPABASE_URL,
                                        com.vibe.ui.BuildConfig.SUPABASE_ANON_KEY,
                                        finalResult.token,
                                        name.trim(),
                                        username
                                    )
                                }

                                // Generate E2E keys
                                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                    com.vibe.ui.e2e.E2EEngine.generateUserKeys()
                                }

                                Toast.makeText(context, VibeI18n.t("account_created"), Toast.LENGTH_SHORT).show()
                                onComplete(pair.second)
                            } else {
                                // Email confirmation required or Supabase unavailable
                                // Try signIn in case account already exists
                                val signInResult = com.vibe.ui.network.SupabaseAuthManager.signIn(
                                    com.vibe.ui.BuildConfig.SUPABASE_URL,
                                    com.vibe.ui.BuildConfig.SUPABASE_ANON_KEY,
                                    email.trim().lowercase(Locale.ROOT),
                                    password
                                )
                                if (signInResult.success && signInResult.token != null) {
                                    serverConfig.setAuthToken(signInResult.token)
                                    signInResult.refreshToken?.let { serverConfig.setRefreshToken(it) }
                                    signInResult.userId?.let { serverConfig.setUserId(it) }
                                    serverConfig.setAuthenticated(true)

                                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                        com.vibe.ui.network.SupabaseClient.createProfile(
                                            com.vibe.ui.BuildConfig.SUPABASE_URL,
                                            com.vibe.ui.BuildConfig.SUPABASE_ANON_KEY,
                                            signInResult.token,
                                            name.trim(),
                                            username
                                        )
                                    }

                                    Toast.makeText(context, VibeI18n.t("account_created"), Toast.LENGTH_SHORT).show()
                                    onComplete(pair.second)
                                } else {
                                    // Fall back to local-only mode
                                    serverConfig.setUserId(pair.first.toString())
                                    serverConfig.setAuthenticated(true)
                                    serverConfig.setVibeId(pair.second)
                                    Toast.makeText(context, VibeI18n.t("account_created_offline"), Toast.LENGTH_LONG).show()
                                    onComplete(pair.second)
                                }
                            }
                        }
                    }
                },
                fullWidth = true,
                size = VibeButtonSize.LARGE
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
