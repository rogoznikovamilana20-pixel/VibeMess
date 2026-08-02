package com.vibe.ui.compose.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.PurchaseEntity
import com.vibe.ui.data.payment.SparkManager
import com.vibe.ui.data.payment.YooKassaPaymentProvider
import com.vibe.ui.i18n.VibeI18n
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val POLL_ATTEMPTS = 30
private const val POLL_INTERVAL_MS = 2000L

private enum class PaymentStep { CREATING, WAITING_PAYMENT, COMPLETING, DONE, FAILED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentFlowScreen(
    itemType: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val provider = remember { YooKassaPaymentProvider.create(context) }
    val db = remember { VibeDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(PaymentStep.CREATING) }
    var paymentId by remember { mutableStateOf("") }
    var confirmationUrl by remember { mutableStateOf<String?>(null) }
    var isDemo by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pollingActive by remember { mutableStateOf(false) }

    fun applyPayment(type: String) {
        when {
            type.startsWith("sparks_") -> {
                val sparks = when (type) {
                    "sparks_100" -> 100L
                    "sparks_500" -> 500L
                    "sparks_1000" -> 1000L
                    else -> 0L
                }
                if (sparks > 0) {
                    scope.launch {
                        SparkManager.addSparks(sparks)
                    }
                }
            }
            type.startsWith("vibe_plus") -> {
                val days = when (type) {
                    "vibe_plus_month" -> 30L
                    "vibe_plus_6" -> 180L
                    "vibe_plus_year" -> 365L
                    else -> 30L
                }
                SparkManager.activateVibePlus(days)
            }
        }
        SparkManager.refreshFromServer()
    }

    LaunchedEffect(Unit) {
        provider.createPayment(itemType)
            .onSuccess { payment ->
                paymentId = payment.paymentId
                confirmationUrl = payment.confirmationUrl
                isDemo = payment.demo
                db.purchaseDao().insert(
                    PurchaseEntity(
                        itemType = payment.itemType,
                        amountKopecks = payment.amountKopecks,
                        status = payment.status,
                        providerPaymentId = payment.paymentId
                    )
                )
                step = PaymentStep.WAITING_PAYMENT
            }
            .onFailure { e ->
                errorMessage = e.message ?: "Ошибка"
                step = PaymentStep.FAILED
            }
    }

    LaunchedEffect(pollingActive) {
        if (!pollingActive) return@LaunchedEffect
        repeat(POLL_ATTEMPTS) {
            delay(POLL_INTERVAL_MS)
            val status = provider.checkStatus(paymentId) ?: return@repeat
            if (status == "succeeded") {
                pollingActive = false
                step = PaymentStep.COMPLETING
                applyPayment(itemType)
                step = PaymentStep.DONE
                return@LaunchedEffect
            }
            if (status == "cancelled") {
                pollingActive = false
                errorMessage = "Оплата отменена"
                step = PaymentStep.FAILED
                return@LaunchedEffect
            }
        }
        pollingActive = false
        errorMessage = "Время ожидания истекло"
        step = PaymentStep.FAILED
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(VibeI18n.t("pay"), fontWeight = FontWeight.Bold) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (step) {
                    PaymentStep.CREATING -> {
                        CircularProgressIndicator(color = Color(0xFF8B5CF6))
                        Spacer(Modifier.height(16.dp))
                        Text("Создание платежа...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    PaymentStep.WAITING_PAYMENT -> {
                        Text("Оплата через СБП",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isDemo) "Демо-режим: подтвердите платеж"
                            else "Откройте приложение банка или QR-код для оплаты",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        if (isDemo) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        step = PaymentStep.COMPLETING
                                        val ok = provider.completeDemo(paymentId)
                                        if (ok) {
                                            applyPayment(itemType)
                                            step = PaymentStep.DONE
                                        } else {
                                            errorMessage = "Не удалось подтвердить"
                                            step = PaymentStep.FAILED
                                        }
                                    }
                                },
                                modifier = Modifier.size(width = 220.dp, height = 48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8B5CF6)
                                )
                            ) {
                                Text("Подтвердить (демо)", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    confirmationUrl?.let { url ->
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                        pollingActive = true
                                    }
                                },
                                modifier = Modifier.size(width = 220.dp, height = 48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8B5CF6)
                                )
                            ) {
                                Text("Оплатить", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { pollingActive = true },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Я уже оплатил")
                            }
                        }
                    }

                    PaymentStep.COMPLETING -> {
                        CircularProgressIndicator(color = Color(0xFF8B5CF6))
                        Spacer(Modifier.height(16.dp))
                        Text("Подтверждение платежа...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    PaymentStep.DONE -> {
                        Text("✓", color = Color(0xFF4ADE80), fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Оплата прошла успешно",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5CF6)
                            )
                        ) {
                            Text("Готово", fontWeight = FontWeight.Bold)
                        }
                    }

                    PaymentStep.FAILED -> {
                        Text("✕", color = MaterialTheme.colorScheme.error, fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(errorMessage ?: "Ошибка оплаты",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = onBack,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Назад")
                        }
                    }
                }
            }
        }
    }
}
