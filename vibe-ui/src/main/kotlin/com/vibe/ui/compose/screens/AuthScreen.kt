package com.vibe.ui.compose.screens

import android.content.Context
import android.telephony.TelephonyManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
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

private fun detectCountryCode(context: Context): String {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return "+7 "
    val iso = tm.simCountryIso?.uppercase() ?: tm.networkCountryIso?.uppercase() ?: return "+7 "
    return (COUNTRY_DIAL_CODES[iso] ?: "+7") + " "
}

private val COUNTRY_DIAL_CODES = mapOf(
    "RU" to "+7", "KZ" to "+7", "UA" to "+380", "BY" to "+375",
    "US" to "+1", "CA" to "+1", "GB" to "+44", "DE" to "+49",
    "FR" to "+33", "IT" to "+39", "ES" to "+34", "TR" to "+90",
    "CN" to "+86", "JP" to "+81", "KR" to "+82", "IN" to "+91",
    "BR" to "+55", "AU" to "+61", "PL" to "+48", "CZ" to "+420",
    "IL" to "+972", "SE" to "+46", "NO" to "+47", "FI" to "+358",
    "DK" to "+45", "NL" to "+31", "BE" to "+32", "CH" to "+41",
    "AT" to "+43", "PT" to "+351", "GR" to "+30", "IE" to "+353",
    "NZ" to "+64", "SG" to "+65", "MY" to "+60", "TH" to "+66",
    "VN" to "+84", "PH" to "+63", "ID" to "+62", "PK" to "+92",
    "BD" to "+880", "EG" to "+20", "ZA" to "+27", "NG" to "+234",
    "AZ" to "+994", "AM" to "+374", "GE" to "+995", "KG" to "+996",
    "TJ" to "+992", "TM" to "+993", "UZ" to "+998", "MD" to "+373",
    "LV" to "+371", "LT" to "+370", "EE" to "+372", "MN" to "+976",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf(detectCountryCode(context)) }
    var code by remember { mutableStateOf("") }
    var showCodeInput by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вход", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                text = "Введите номер телефона",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Мы отправим код подтверждения",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            VibeInput(
                value = phoneNumber,
                onValueChange = { phoneNumber = it; error = null },
                label = "Номер телефона",
                placeholder = "+7 (999) 123-45-67",
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
                error = error,
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null,
                         tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(visible = showCodeInput) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .padding(top = 16.dp)
                ) {
                    VibeInput(
                        value = code,
                        onValueChange = { code = it },
                        label = "Код подтверждения",
                        placeholder = "••••••",
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                        error = error,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Отправить код повторно через 30 сек",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            VibeButton(
                text = if (showCodeInput) "Подтвердить" else "Получить код",
                onClick = {
                    if (!showCodeInput) {
                        if (phoneNumber.isBlank()) {
                            error = "Введите номер телефона"
                        } else {
                            showCodeInput = true
                        }
                    } else {
                        onComplete()
                    }
                },
                fullWidth = true,
                size = VibeButtonSize.LARGE
            )
        }
    }
}
