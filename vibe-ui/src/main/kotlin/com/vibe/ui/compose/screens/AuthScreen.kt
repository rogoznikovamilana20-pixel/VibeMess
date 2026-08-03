package com.vibe.ui.compose.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vibe.ui.compose.components.VibeButton
import com.vibe.ui.compose.components.VibeButtonSize
import com.vibe.ui.compose.components.VibeInput
import com.vibe.ui.i18n.VibeI18n
import com.vibe.ui.network.ServerConfig
import com.vibe.ui.network.SupabaseAuthManager
import com.vibe.ui.BuildConfig
import kotlinx.coroutines.launch

private enum class AuthStep { LOGIN, REGISTER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val serverConfig = remember { ServerConfig(context) }

    var step by remember { mutableStateOf(AuthStep.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val supabaseUrl = BuildConfig.SUPABASE_URL
    val anonKey = BuildConfig.SUPABASE_ANON_KEY

    fun doLogin() {
        if (email.isBlank() || !email.contains("@")) {
            error = VibeI18n.t("invalid_email")
            return
        }
        if (password.length < 6) {
            error = VibeI18n.t("password_short")
            return
        }
        error = null
        loading = true
        scope.launch {
            val result = SupabaseAuthManager.signIn(supabaseUrl, anonKey, email, password)
            if (result.success && result.token != null) {
                serverConfig.setAuthToken(result.token)
                result.refreshToken?.let { serverConfig.setRefreshToken(it) }
                result.userId?.let { serverConfig.setUserId(it) }
                result.email?.let { serverConfig.setUsername(it) }
                serverConfig.setAuthenticated(true)

                // Ensure profile exists in Supabase
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val existing = com.vibe.ui.network.SupabaseClient.getProfile(
                        supabaseUrl, anonKey, result.token, result.userId ?: ""
                    )
                    if (existing == null) {
                        val name = listOfNotNull(firstName.ifBlank { null }, lastName.ifBlank { null })
                            .joinToString(" ").ifBlank { email }
                        com.vibe.ui.network.SupabaseClient.createProfile(
                            supabaseUrl, anonKey, result.token, name
                        )
                    }
                }

                // Generate E2E keys if not already registered
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    if (!com.vibe.ui.e2e.E2EEngine.isReady()) {
                        com.vibe.ui.e2e.E2EEngine.generateUserKeys()
                    }
                }

                loading = false
                onComplete()
            } else {
                loading = false
                error = result.error ?: VibeI18n.t("login_error")
            }
        }
    }

    fun doRegister() {
        if (firstName.isBlank()) {
            error = VibeI18n.t("enter_name")
            return
        }
        if (email.isBlank() || !email.contains("@")) {
            error = VibeI18n.t("invalid_email")
            return
        }
        if (password.length < 6) {
            error = VibeI18n.t("password_short")
            return
        }
        error = null
        loading = true
        scope.launch {
            val result = SupabaseAuthManager.signUp(supabaseUrl, anonKey, email, password)
            if (result.success && result.token != null) {
                serverConfig.setAuthToken(result.token)
                result.refreshToken?.let { serverConfig.setRefreshToken(it) }
                result.userId?.let { serverConfig.setUserId(it) }
                val displayName = listOfNotNull(firstName.ifBlank { null }, lastName.ifBlank { null })
                    .joinToString(" ").ifBlank { email }
                serverConfig.setUsername(displayName)
                serverConfig.setAuthenticated(true)

                // Create profile in Supabase
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.vibe.ui.network.SupabaseClient.createProfile(
                        supabaseUrl, anonKey, result.token, displayName
                    )
                }

                // Generate E2E keys for new user
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    com.vibe.ui.e2e.E2EEngine.generateUserKeys()
                }

                loading = false
                onComplete()
            } else {
                loading = false
                error = result.error ?: VibeI18n.t("register_error")
            }
        }
    }

    val title = when (step) {
        AuthStep.LOGIN -> VibeI18n.t("login_title")
        AuthStep.REGISTER -> VibeI18n.t("create_account_title")
    }

    val subtitle = when (step) {
        AuthStep.LOGIN -> VibeI18n.t("login_subtitle")
        AuthStep.REGISTER -> VibeI18n.t("register_subtitle")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        when (step) {
                            AuthStep.LOGIN -> onBack()
                            AuthStep.REGISTER -> {
                                step = AuthStep.LOGIN
                                error = null
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(visible = step == AuthStep.REGISTER) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    VibeInput(
                        value = firstName,
                        onValueChange = { firstName = it; error = null },
                        label = VibeI18n.t("first_name"),
                        imeAction = ImeAction.Next,
                        error = if (step == AuthStep.REGISTER) error else null,
                        enabled = !loading,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null,
                                 tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    VibeInput(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = VibeI18n.t("last_name_optional"),
                        imeAction = ImeAction.Next,
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            VibeInput(
                value = email,
                onValueChange = { email = it; error = null },
                label = "Email",
                placeholder = "user@example.com",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                error = if (step == AuthStep.LOGIN) error else null,
                enabled = !loading,
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null,
                         tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            VibeInput(
                value = password,
                onValueChange = { password = it; error = null },
                label = VibeI18n.t("password"),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                error = if (step == AuthStep.LOGIN && error != null) error else null,
                enabled = !loading,
                isPassword = true,
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null,
                         tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (step == AuthStep.LOGIN && error != null) {
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            VibeButton(
                text = when {
                    loading && step == AuthStep.LOGIN -> VibeI18n.t("login_loading")
                    loading && step == AuthStep.REGISTER -> VibeI18n.t("register_loading")
                    step == AuthStep.LOGIN -> VibeI18n.t("login")
                    else -> VibeI18n.t("register")
                },
                onClick = {
                    when (step) {
                        AuthStep.LOGIN -> doLogin()
                        AuthStep.REGISTER -> doRegister()
                    }
                },
                fullWidth = true,
                size = VibeButtonSize.LARGE,
                enabled = !loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when (step) {
                        AuthStep.LOGIN -> VibeI18n.t("no_account")
                        AuthStep.REGISTER -> VibeI18n.t("has_account")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (step) {
                        AuthStep.LOGIN -> VibeI18n.t("register")
                        AuthStep.REGISTER -> VibeI18n.t("login")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(enabled = !loading) {
                        step = when (step) {
                            AuthStep.LOGIN -> AuthStep.REGISTER
                            AuthStep.REGISTER -> AuthStep.LOGIN
                        }
                        error = null
                    }
                )
            }
        }
    }
}
