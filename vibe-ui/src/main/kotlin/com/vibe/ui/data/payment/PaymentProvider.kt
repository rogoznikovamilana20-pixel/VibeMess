package com.vibe.ui.data.payment

import android.content.Context
import com.vibe.ui.network.ServerConfig
import com.vibe.ui.network.VibeHttpClient

data class CreatedPayment(
    val paymentId: String,
    val itemType: String,
    val amountKopecks: Long,
    val status: String,
    val confirmationUrl: String?,
    val demo: Boolean
)

interface VibePaymentProvider {
    suspend fun createPayment(itemType: String): Result<CreatedPayment>
    suspend fun checkStatus(paymentId: String): String?
    suspend fun completeDemo(paymentId: String): Boolean
}

/**
 * Delegates to vibe-server; the server talks to YooKassa (SBP)
 * when YOOKASSA_SHOP_ID/YOOKASSA_SECRET are configured, otherwise runs in demo mode.
 */
class YooKassaPaymentProvider(private val httpClient: VibeHttpClient) : VibePaymentProvider {

    override suspend fun createPayment(itemType: String): Result<CreatedPayment> {
        return httpClient.rustCreatePayment(itemType).map {
            CreatedPayment(
                paymentId = it.paymentId,
                itemType = it.itemType,
                amountKopecks = it.amountKopecks,
                status = it.status,
                confirmationUrl = it.confirmationUrl,
                demo = it.demo
            )
        }
    }

    override suspend fun checkStatus(paymentId: String): String? = httpClient.rustPaymentStatus(paymentId)

    override suspend fun completeDemo(paymentId: String): Boolean = httpClient.rustDemoComplete(paymentId)

    companion object {
        fun create(context: Context): YooKassaPaymentProvider {
            val serverConfig = ServerConfig(context)
            return YooKassaPaymentProvider(VibeHttpClient(serverConfig))
        }
    }
}
