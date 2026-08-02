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
        if (name.isBlank()) return "Введите имя"
        if (username.isBlank()) return "Введите имя пользователя"
        if (!username.matches(Regex("^[a-zA-Z0-9_]{3,32}$")))
            return "Имя пользователя: 3–32 символа, буквы, цифры, _"
        if (email.isBlank() || !emailValid) return "Введите корректный email"
        if (password.length < 6) return "Пароль должен быть не короче 6 символов"
        if (password != confirmPassword) return "Пароли не совпадают"
        return null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Регистрация", fontWeight = FontWeight.Bold) },
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
                text = "Создайте аккаунт Vibe",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Аккаунт хранится локально в защищённой базе данных",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Имя") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase(Locale.ROOT).replace(" ", ""); error = null },
                label = { Text("Имя пользователя") },
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
                    { Text("Некорректный email") }
                } else null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Пароль") },
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
                label = { Text("Повторите пароль") },
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
                text = if (registering) "Создание..." else "Зарегистрироваться",
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
                                    error = "Аккаунт с таким email уже существует"
                                    null
                                } else if (db.accountDao().getByUsername(username) != null) {
                                    error = "Имя пользователя занято"
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
                                error = "Не удалось сохранить аккаунт"
                                null
                            }
                        }
                        registering = false
                        val pair = result
                        if (pair != null) {
                            val serverConfig = ServerConfig(context)
                            serverConfig.setUserId(pair.first.toString())
                            serverConfig.setAuthenticated(true)
                            serverConfig.setVibeId(pair.second)
                            ProfileRepository(context).apply {
                                this.displayName = name.trim()
                                this.username = "@$username"
                                this.vibeId = pair.second
                            }
                            Toast.makeText(context, "Аккаунт создан", Toast.LENGTH_SHORT).show()
                            onComplete(pair.second)
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
