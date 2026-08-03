package com.vibe.ui.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import com.vibe.ui.data.AchievementManager
import com.vibe.ui.data.ProfileRepository
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.MarketplaceListingEntity
import com.vibe.ui.i18n.VibeI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

private val categories = listOf(VibeI18n.t("all"), "Услуги", "Товары", "Цифровое", "Обучение", "Другое")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MarketplaceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val profileRepo = remember { ProfileRepository(context) }
    val db = remember { VibeDatabase.getDatabase(context) }
    var selectedCategory by remember { mutableStateOf(VibeI18n.t("all")) }
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Услуги") }
    val scope = rememberCoroutineScope()
    val resetForm = {
        title = ""
        description = ""
        price = ""
        category = "Услуги"
    }

    // Paging 3 data
    val pagingFlow = remember(selectedCategory) {
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false)
        ) {
            if (selectedCategory == VibeI18n.t("all")) {
                db.marketplaceDao().getActiveListingsPaging()
            } else {
                db.marketplaceDao().getListingsByCategoryPaging(selectedCategory)
            }
        }.flow
    }
    val listings = pagingFlow.collectAsLazyPagingItems()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(VibeI18n.t("marketplace"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, VibeI18n.t("new_listing"))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            if (listings.itemCount == 0) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Sell, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(VibeI18n.t("no_listings"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(VibeI18n.t("tap_plus_to_create"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                var buyListing by remember { mutableStateOf<MarketplaceListingEntity?>(null) }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    items(
                        count = listings.itemCount,
                        key = { listings[it]?.id ?: it.toLong() }
                    ) { index ->
                        listings[index]?.let { listing ->
                            ListingCard(listing, onBuy = { buyListing = listing })
                        }
                    }
                }
                buyListing?.let { listing ->
                    val priceSparks = listing.price.toLong().coerceAtLeast(1)
                    val commission = com.vibe.ui.data.payment.SparkManager.commissionFor(priceSparks)
                    AlertDialog(
                        onDismissRequest = { buyListing = null },
                        title = { Text("Покупка") },
                        text = {
                            Column {
                                Text("${listing.title} — $priceSparks ⚡")
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Комиссия платформы: $commission ⚡ (5%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Продавцу зачислится ${priceSparks - commission} ⚡",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch {
                                    val ok = com.vibe.ui.data.payment.SparkManager.spendSparks(priceSparks)
                                    if (ok) {
                                        db.marketplaceDao().deactivate(listing.id)
                                        android.widget.Toast.makeText(
                                            context,
                                            "Куплено за $priceSparks ⚡",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            VibeI18n.t("not_enough_sparks"),
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                buyListing = null
                            }) { Text(VibeI18n.t("buy")) }
                        },
                        dismissButton = {
                            TextButton(onClick = { buyListing = null }) { Text(VibeI18n.t("cancel")) }
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false; resetForm() },
            title = { Text(VibeI18n.t("new_listing")) },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(VibeI18n.t("title")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(VibeI18n.t("description")) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text(VibeI18n.t("price")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(VibeI18n.t("category"), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.filter { it != VibeI18n.t("all") }.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        val priceVal = price.toDoubleOrNull() ?: 0.0
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.marketplaceDao().insertListing(MarketplaceListingEntity(
                                    title = title.trim(),
                                    description = description.trim(),
                                    price = priceVal,
                                    category = category,
                                    createdAt = System.currentTimeMillis(),
                                    sellerName = profileRepo.displayName
                                ))
                                AchievementManager(context).unlock(AchievementManager.Id.FIRST_LISTING)
                            }
                        }
                    }
                    showDialog = false
                    resetForm()
                }) { Text(VibeI18n.t("create")) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; resetForm() }) {
                    Text(VibeI18n.t("cancel"))
                }
            }
        )
    }
}

@Composable
private fun ListingCard(
    listing: MarketplaceListingEntity,
    onBuy: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(listing.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                Text("${listing.price.toInt()} ₽",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(listing.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            if (listing.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(listing.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Продавец: ${listing.sellerName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onBuy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Купить за ⚡", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}


