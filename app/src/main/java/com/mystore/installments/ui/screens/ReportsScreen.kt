package com.mystore.installments.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystore.installments.data.entity.InstallmentStatus
import com.mystore.installments.util.formatAmount
import com.mystore.installments.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: AppViewModel) {
    val sales by viewModel.sales.collectAsState()
    val allInstallments by viewModel.repository.getAllInstallments().collectAsState(initial = emptyList())

    val totalSalesValue = sales.sumOf { it.totalAmount }
    val totalCollected = allInstallments.sumOf { it.paidAmount } + sales.sumOf { it.downPayment }
    val totalRemaining = allInstallments.filter { it.status != InstallmentStatus.PAID }
        .sumOf { it.amount - it.paidAmount }
    val overdueCount = allInstallments.count { it.status != InstallmentStatus.PAID && it.dueDate < System.currentTimeMillis() }

    Scaffold(topBar = { TopAppBar(title = { Text("التقارير") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReportRow("إجمالي قيمة المبيعات", formatAmount(totalSalesValue))
            ReportRow("إجمالي المُحصَّل (مقدمات + أقساط)", formatAmount(totalCollected))
            ReportRow("إجمالي المبالغ المتبقية", formatAmount(totalRemaining))
            ReportRow("عدد الأقساط المتأخرة", overdueCount.toString())
            ReportRow("عدد عمليات البيع", sales.size.toString())
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
