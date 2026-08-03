package com.vibe.ui.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibe.ui.i18n.VibeI18n
import com.vibe.ui.i18n.VibeLang
import com.vibe.ui.i18n.VibeLanguages
import com.vibe.ui.network.ServerConfig
import org.telegram.messenger.LocaleController
import org.telegram.messenger.UserConfig

private fun applyAppLanguage(context: Context, lang: VibeLang) {
    val localeController = LocaleController.getInstance()
    val localeInfo = localeController.languagesDict[lang.code]
        ?: localeController.remoteLanguagesDict[lang.code]
        ?: localeController.unofficialLanguages.firstOrNull { it.shortName == lang.code }
        ?: LocaleController.LocaleInfo.createWithString("${lang.englishName}|${lang.nativeName}|${lang.code}|remote")
    if (localeInfo == null) {
        Toast.makeText(context, "Язык недоступен: ${lang.nativeName}", Toast.LENGTH_SHORT).show()
        return
    }
    localeController.applyLanguage(localeInfo, true, false, UserConfig.selectedAccount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLanguageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val serverConfig = remember { ServerConfig(context) }
    val telegramCode = LocaleController.getInstance().currentLocaleInfo?.shortName
    var selectedCode by remember {
        mutableStateOf(VibeI18n.currentCode.ifBlank { telegramCode ?: serverConfig.getAppLanguageCode() })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(VibeI18n.t("language"), fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                VibeI18n.t("interface_language"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn {
                items(VibeLanguages.all, key = { it.code }) { lang ->
                    val isSelected = selectedCode == lang.code
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCode = lang.code
                                VibeI18n.setLanguage(lang.code)
                                serverConfig.setAppLanguage(lang.code)
                                applyAppLanguage(context, lang)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                Color(0xFF8D2BFA).copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                lang.nativeName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                lang.englishName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(0.dp))
                                Icon(
                                    Icons.Default.Check, null,
                                    tint = Color(0xFF8D2BFA)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                VibeI18n.t("language_applied_immediately"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
