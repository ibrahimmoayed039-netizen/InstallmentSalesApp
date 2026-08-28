package com.mystore.installments.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.mystore.installments.ui.components.PayInstallmentDialog
import com.mystore.installments.ui.nav.Routes
import com.mystore.installments.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * متابعة الأقساط: تُعرض مجمّعة على اسم كل عميل. أقساط كل عميل مطوية (مخفية) افتراضياً
 * ولا تظهر إلا عند الضغط على اسم العميل (لتوسيع/طي القائمة)، مع إمكانية تسديد أي قسط مباشرة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentsScreen(viewModel: AppViewModel, navController: NavController) {
    val installments by viewModel.unpaidInstallmentsWithCustomer.collectAsState()
    val now = System.currentTimeMillis()
    var searchQuery by remember { mutableStateOf("") }

    // معرّفات العملاء المفتوحين حالياً (أقساطهم ظاهرة)؛ فارغة افتراضياً يعني الكل مطوي
    var expandedCustomerIds by remember { mutableStateOf(setOf<Long>()) }

    // القسط الذي فُتحت له نافذة التسديد حالياً (يحمل معه اسم وهاتف صاحبه)
    var payDialogEntry by remember { mutableStateOf<InstallmentWithCustomer?>(null) }

    val filtered = remember(installments, searchQuery) {
        if (searchQuery.isBlank()) installments
        else installments.filter {
            it.customerName.contains(searchQuery, ignoreCase = true) ||
                it.customerPhone.contains(searchQuery, ignoreCase = true)
        }
    }

    // تجميع الأقساط حسب العميل، مرتّبة أبجدياً على اسم العميل
    val grouped = remember(filtered) {
        filtered.groupBy { Triple(it.customerId, it.customerName, it.customerPhone) }
            .toList()
            .sortedBy { it.first.second }
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
                        val isExpanded = customerId in expandedCustomerIds

                        item(key = "header_$customerId") {
                            ElevatedCard(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clickable {
                                        expandedCustomerIds = if (isExpanded) {
                                            expandedCustomerIds - customerId
                                        } else {
                                            expandedCustomerIds + customerId
                                        }
                                    }
                            ) {
                                Row(
                                    Modifier.padding(14.dp).fillMaxWidth(),
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
                                        Text(
                                            "${customerInstallments.size} قسط غير مسدد",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "%.0f".format(totalRemaining),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = if (isExpanded) "طي" else "توسيع"
                                        )
                                    }
                                }
                            }
                        }

                        if (isExpanded) {
                            items(
                                customerInstallments.sortedBy { it.installment.dueDate },
                                key = { "inst_${it.installment.id}" }
                            ) { entry ->
                                val inst = entry.installment
                                val isLate = inst.dueDate < now
                                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 5.dp, horizontal = 8.dp)) {
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "%.0f".format(inst.amount - inst.paidAmount),
                                                fontWeight = FontWeight.Bold,
                                                color = if (isLate) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Button(
                                                onClick = { payDialogEntry = entry },
                                                contentPadding = PaddingValues(horizontal = 10.dp)
                                            ) {
                                                Text("تسديد")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    payDialogEntry?.let { entry ->
        val inst = entry.installment
        PayInstallmentDialog(
            installmentId = inst.id,
            installmentNumber = inst.installmentNumber,
            remaining = inst.amount - inst.paidAmount,
            customerName = entry.customerName,
            customerPhone = entry.customerPhone,
            onDismiss = { payDialogEntry = null },
            onConfirmed = { navController.navigate(Routes.RECEIPT_PREVIEW) },
            viewModel = viewModel
        )
    }
}
