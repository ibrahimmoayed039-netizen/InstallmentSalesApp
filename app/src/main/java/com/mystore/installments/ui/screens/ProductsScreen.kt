package com.mystore.installments.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mystore.installments.data.entity.Product
import com.mystore.installments.ui.components.AppBottomBar
import com.mystore.installments.ui.nav.Routes
import com.mystore.installments.util.formatAmount
import com.mystore.installments.viewmodel.AppViewModel

// أقل كمية بالمخزون قبل اعتبارها "منخفضة" وتنبيه صاحب المحل بها
private const val LOW_STOCK_THRESHOLD = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(viewModel: AppViewModel, navController: NavController) {
    val products by viewModel.products.collectAsState()
    var query by remember { mutableStateOf("") }

    val filtered = remember(products, query) {
        if (query.isBlank()) products
        else products.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.barcode.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("المنتجات (${products.size})") }) },
        bottomBar = { AppBottomBar(navController) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.PRODUCT_FORM) }) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة منتج")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (products.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("بحث بالاسم أو الباركود أو الفئة") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }

            if (products.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("لا توجد منتجات بعد، اضغط + للإضافة")
                    }
                }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا نتائج مطابقة لـ \"$query\"")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    items(filtered) { product ->
                        ProductCard(product = product, onClick = { navController.navigate(Routes.productForm(product.id)) })
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    val lowStock = product.stockQuantity in 1..LOW_STOCK_THRESHOLD
    val outOfStock = product.stockQuantity <= 0
    // هامش الربح: الفرق بين سعر البيع نقداً وسعر التكلفة، ونسبته المئوية إن كانت التكلفة معروفة
    val margin = product.cashPrice - product.costPrice
    val marginPercent = if (product.costPrice > 0) (margin / product.costPrice * 100) else null

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imageUri,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.Inventory2, contentDescription = null)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        product.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (product.category.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        AssistChip(onClick = {}, label = { Text(product.category) })
                    }
                }
                if (product.barcode.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(product.barcode, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("تكلفة: ${formatAmount(product.costPrice)}", style = MaterialTheme.typography.bodySmall)
                    Text("نقداً: ${formatAmount(product.cashPrice)}", style = MaterialTheme.typography.bodySmall)
                    Text("تقسيط: ${formatAmount(product.installmentPrice)}", style = MaterialTheme.typography.bodySmall)
                }
                if (marginPercent != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "هامش الربح: ${formatAmount(margin)} (${"%.0f".format(marginPercent)}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (margin >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
                Spacer(Modifier.height(6.dp))
                when {
                    outOfStock -> StockBadge("نفد من المخزون", Color(0xFFC62828))
                    lowStock -> StockBadge("مخزون منخفض: ${product.stockQuantity}", Color(0xFFEF6C00))
                    product.stockQuantity > 0 -> StockBadge("متوفر: ${product.stockQuantity}", Color(0xFF2E7D32))
                }
            }
        }
    }
}

@Composable
private fun StockBadge(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (color == Color(0xFFC62828) || color == Color(0xFFEF6C00)) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
    }
}
