package com.vibe.ui.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibe.ui.i18n.VibeI18n

private data class PlusPlan(
    val itemType: String,
    val title: String,
    val price: String,
    val perMonth: String,
    val benefits: List<String>
)

private val PLUS_PLANS = listOf(
    PlusPlan(
        "vibe_plus_month",
        "1 месяц",
        "199 ₽",
        "199 ₽/мес",
        listOf(
            "Aurion AI без лимитов",
            "HD-качество звонков",
            "0% комиссия в маркетплейсе",
            "Искры каждый месяц",
            "3 дня бесплатно"
        )
    ),
    PlusPlan(
        "vibe_plus_6",
        "6 месяцев",
        "894 ₽",
        "149 ₽/мес",
        listOf(
            "Aurion AI без лимитов",
            "HD-качество звонков",
            "0% комиссия в маркетплейсе",
            "Искры каждый месяц",
            "Скидка 25% к месячному тарифу"
        )
    ),
    PlusPlan(
        "vibe_plus_year",
        "12 месяцев",
        "1428 ₽",
        "119 ₽/мес",
        listOf(
            "Aurion AI без лимитов",
            "HD-качество звонков",
            "0% комиссия в маркетплейсе",
            "Искры каждый месяц",
            "2 месяца бесплатно",
            "Скидка 40% к месячному тарифу"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibePlusScreen(
    onBack: () -> Unit,
    onPay: (itemType: String) -> Unit
) {
    var selected by remember { mutableStateOf(PLUS_PLANS[1].itemType) }
    val sparkState = com.vibe.ui.data.payment.SparkManager.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF8D2BFA), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Vibe+", fontWeight = FontWeight.Bold)
                    }
                },
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
                .padding(16.dp)
        ) {
            if (sparkState.value.isVibePlus) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF8D2BFA).copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF8D2BFA))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("VIBE+ активен",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8D2BFA))
                            if (sparkState.value.vibePlusExpiresAt > 0) {
                                Text(
                                    "до ${java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                                        .format(java.util.Date(sparkState.value.vibePlusExpiresAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF8D2BFA), Color(0xFFB06BFF))),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text("Vibe+",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Больше возможностей. Меньше границ.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.height(16.dp))
                    val selectedPlan = PLUS_PLANS.first { it.itemType == selected }
                    selectedPlan.benefits.forEach { b ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✦", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text(b, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            PLUS_PLANS.forEach { plan ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected == plan.itemType)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == plan.itemType,
                            onClick = { selected = plan.itemType }
                        )
                        Spacer(Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plan.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                            Text(plan.perMonth,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(plan.price,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8D2BFA))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onPay(selected) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8D2BFA)
                )
            ) {
                Text(VibeI18n.t("pay"), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
