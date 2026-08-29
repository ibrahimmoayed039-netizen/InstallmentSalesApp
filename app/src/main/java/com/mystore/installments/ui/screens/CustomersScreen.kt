package com.mystore.installments.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mystore.installments.ui.components.AppBottomBar
import com.mystore.installments.ui.nav.Routes
import com.mystore.installments.viewmodel.AppViewModel
import kotlin.math.abs

/** لوحة ألوان ثابتة لأفاتار العملاء، يُختار منها لون بشكل ثابت حسب اسم العميل (لا يتغيّر بين الفتحات) */
private val avatarPalette = listOf(
    Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFEF6C00),
    Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFFAD1457),
    Color(0xFF4E342E), Color(0xFF283593)
)

private fun avatarColorFor(name: String): Color =
    avatarPalette[abs(name.hashCode()) % avatarPalette.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(viewModel: AppViewModel, navController: NavController) {
    val customers by viewModel.customers.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("العملاء") }) },
        bottomBar = { AppBottomBar(navController) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.CUSTOMER_FORM) }) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة عميل")
            }
        }
    ) { padding ->
        if (customers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("لا يوجد عملاء بعد، اضغط + للإضافة")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
                items(customers) { customer ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { navController.navigate(Routes.customerDetail(customer.id)) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // أفاتار بحرف اسم العميل الأول، بلون ثابت مشتق من اسمه للتمييز السريع بين العملاء بالقائمة
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(avatarColorFor(customer.name)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    customer.name.trim().firstOrNull()?.uppercase() ?: "؟",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Row {
                                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(customer.phone, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
