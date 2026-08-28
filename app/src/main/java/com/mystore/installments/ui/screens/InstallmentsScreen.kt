package com.mystore.installments.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mystore.installments.data.entity.InstallmentWithCustomer
import com.mystore.installments.ui.components.AppBottomBar
import com.mystore.installments.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * متابعة الأقساط: تُعرض مجمّعة على اسم كل عميل (بدل عرضها كقائمة مسطّحة بأرقام الفواتير فقط)،
 * مع مجموع المتبقي على كل عميل، وخانة بحث سريع بالاسم أو رقم الهاتف.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentsScreen(viewModel: AppViewModel, navController: NavController) {
    val installments by viewModel.unpaidInstallmentsWithCustomer.collectAsState()
    val now = System.currentTimeMillis()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(installments, searchQuery) {
        if (searchQuery.isBlank()) installments
        else installments.filter {
            it.customerName.contains(searchQuery, ignoreCase = true) ||
                it.customerPhone.contains(searchQuery, ignoreCase = true)
        }
    }

    // تجميع الأقساط حسب العميل، مع الحفاظ على ترتيب الأقرب استحقاقاً ضمن كل عميل
    val grouped = remember(filtered) {
        filtered.groupBy { Triple(it.customerId, it.customerName, it.customerPhone) }
            .toList()
            .sortedBy { it.first.second } // ترتيب أبجدي حسب اسم العميل
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("متابعة الأقساط") }) },
        bottomBar = { AppBottomBar(navController) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("ابحث باسم العميل أو رقم الهاتف") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            if (grouped.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isBlank()) "لا توجد أقساط مستحقة حالياً" else "لا يوجد عميل مطابق للبحث")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    grouped.forEach { (customer, customerInstallments) ->
                        val (customerId, customerName, customerPhone) = customer
                        val totalRemaining = customerInstallments.sumOf { it.installment.amount - it.installment.paidAmount }
                        val hasLate = customerInstallments.any { it.installment.dueDate < now }

                        item(key = "header_$customerId") {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        customerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasLate) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (customerPhone.isNotBlank()) {
                                        Text(customerPhone, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Text(
                                    "الإجمالي المتبقي: %.0f".format(totalRemaining),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider()
                        }

                        items(
                            customerInstallments.sortedBy { it.installment.dueDate },
                            key = { "inst_${it.installment.id}" }
                        ) { entry ->
                            val inst = entry.installment
                            val isLate = inst.dueDate < now
                            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                Row(
                                    Modifier.padding(14.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("قسط رقم ${inst.installmentNumber} — فاتورة #${inst.saleId}", fontWeight = FontWeight.Bold)
                                        Text(
                                            SimpleDateFormat("yyyy-MM-dd", Locale("ar")).format(Date(inst.dueDate)),
                                            color = if (isLate) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "%.0f".format(inst.amount - inst.paidAmount),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLate) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
