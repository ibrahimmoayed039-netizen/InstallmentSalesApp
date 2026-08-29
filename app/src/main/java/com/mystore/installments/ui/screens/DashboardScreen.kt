package com.mystore.installments.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mystore.installments.ui.components.AppBottomBar
import com.mystore.installments.ui.nav.Routes
import com.mystore.installments.util.formatAmount
import com.mystore.installments.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: AppViewModel, navController: NavController) {
    val sales by viewModel.sales.collectAsState()
    val overdue by viewModel.overdueInstallments.collectAsState()
    val unpaid by viewModel.unpaidInstallments.collectAsState()

    val totalOutstanding = unpaid.sumOf { it.amount - it.paidAmount }
    val totalOverdue = overdue.sumOf { it.amount - it.paidAmount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لوحة التحكم") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "الإعدادات")
                    }
                }
            )
        },
        bottomBar = { AppBottomBar(navController) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        title = "عدد المبيعات",
                        value = sales.size.toString(),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary
                    )
                    SummaryCard(
                        title = "مبالغ مستحقة",
                        value = formatAmount(totalOutstanding),
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF2E7D32)
                    )
                }
            }
            item {
                SummaryCard(
                    title = "أقساط متأخرة (${overdue.size})",
                    value = formatAmount(totalOverdue),
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFC62828)
                )
            }

            item {
                Text(
                    "الأقساط المتأخرة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (overdue.isEmpty()) {
                item {
                    Text("لا توجد أقساط متأخرة حالياً 👍", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                items(overdue) { inst ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("القسط رقم ${inst.installmentNumber}", fontWeight = FontWeight.Bold)
                                Text(
                                    "الاستحقاق: " + SimpleDateFormat("yyyy-MM-dd", Locale("ar")).format(Date(inst.dueDate)),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFC62828))
                            Text(formatAmount(inst.amount - inst.paidAmount), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier, color: Color) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
