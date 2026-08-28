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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mystore.installments.ui.components.AppBottomBar
import com.mystore.installments.ui.nav.Routes
import com.mystore.installments.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(viewModel: AppViewModel, navController: NavController) {
    val products by viewModel.products.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("المنتجات") }) },
        bottomBar = { AppBottomBar(navController) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.PRODUCT_FORM) }) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة منتج")
            }
        }
    ) { padding ->
        if (products.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("لا توجد منتجات بعد، اضغط + للإضافة")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
                items(products) { product ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { navController.navigate(Routes.productForm(product.id)) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
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
                                    Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("التكلفة: ${product.costPrice}", style = MaterialTheme.typography.bodySmall)
                                    Text("نقداً: ${product.cashPrice}", style = MaterialTheme.typography.bodySmall)
                                    Text("تقسيط: ${product.installmentPrice}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

