package com.mystore.installments.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mystore.installments.data.entity.Customer
import com.mystore.installments.data.entity.Installment
import com.mystore.installments.data.entity.InstallmentStatus
import com.mystore.installments.data.entity.Sale
import com.mystore.installments.printer.ReceiptData
import com.mystore.installments.printer.ReceiptLine
import com.mystore.installments.ui.components.PayInstallmentDialog
import com.mystore.installments.ui.nav.Routes
import com.mystore.installments.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(customerId: Long, viewModel: AppViewModel, navController: NavController) {
    var customer by remember { mutableStateOf<Customer?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(customerId) {
        customer = viewModel.repository.getCustomer(customerId)
    }

    val salesFlow = remember(customerId) { viewModel.repository.getSalesForCustomer(customerId) }
    val sales by salesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var payDialogInstallment by remember { mutableStateOf<Installment?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(customer?.name ?: "بيانات العميل") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            item {
                customer?.let {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text("الهاتف: ${it.phone}")
                            if (it.address.isNotBlank()) Text("العنوان: ${it.address}")
                            Spacer(Modifier.height(8.dp))
                            // كشف حساب كامل: كل مبيعات العميل وأقساطه في وصل واحد، مفيد عند
                            // مراجعة حساب العميل كاملاً أو تسليمه ورقة واحدة تلخّص كل تعاملاته
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val cust = customer ?: return@launch
                                        val saleGroups = viewModel.repository.getCustomerStatementItems(customerId)
                                        val receipt = buildStatementReceipt(
                                            storeName = viewModel.appSettings.storeName.ifBlank { "متجرنا" },
                                            customer = cust,
                                            saleGroups = saleGroups
                                        )
                                        viewModel.setPendingReceipt(receipt)
                                        navController.navigate(Routes.RECEIPT_PREVIEW)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("🧾 طباعة كشف حساب كامل") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Text("عمليات البيع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
            }

            items(sales) { sale ->
                SaleCard(
                    sale = sale,
                    viewModel = viewModel,
                    onPay = { installment -> payDialogInstallment = installment },
                    onPrintInvoice = { s ->
                        scope.launch {
                            val cust = customer ?: return@launch
                            val items = viewModel.repository.getSaleItems(s.id)
                            val lines = mutableListOf<ReceiptLine>()
                            if (s.discount > 0) {
                                lines.add(ReceiptLine("إجمالي الأصناف", "%.0f".format(s.totalAmount + s.discount)))
                                lines.add(ReceiptLine("الخصم", "%.0f".format(s.discount)))
                            }
                            lines.add(ReceiptLine("الإجمالي", "%.0f".format(s.totalAmount)))
                            lines.add(ReceiptLine("الدفعة المقدمة", "%.0f".format(s.downPayment)))
                            lines.add(ReceiptLine("عدد الأقساط", s.numberOfInstallments.toString()))
                            lines.add(ReceiptLine("قيمة القسط", "%.0f".format(s.installmentAmount), bold = true))
                            val receipt = ReceiptData(
                                storeName = viewModel.appSettings.storeName.ifBlank { "متجرنا" },
                                title = "فاتورة بيع بالتقسيط رقم ${s.id}",
                                customerName = cust.name,
                                customerPhone = cust.phone,
                                itemsSummary = items.map { "${it.name} × ${it.quantity} = ${"%.0f".format(it.lineTotal)}" },
                                lines = lines
                            )
                            viewModel.setPendingReceipt(receipt)
                            navController.navigate(Routes.RECEIPT_PREVIEW)
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    payDialogInstallment?.let { inst ->
        PayInstallmentDialog(
            installmentId = inst.id,
            installmentNumber = inst.installmentNumber,
            remaining = inst.amount - inst.paidAmount,
            customerName = customer?.name ?: "",
            customerPhone = customer?.phone ?: "",
            onDismiss = { payDialogInstallment = null },
            onConfirmed = { navController.navigate(Routes.RECEIPT_PREVIEW) },
            viewModel = viewModel
        )
    }
}

/** يبني وصل "كشف حساب" واحد يجمع كل فواتير العميل وجداول أقساطها وإجمالياتها */
private fun buildStatementReceipt(
    storeName: String,
    customer: Customer,
    saleGroups: List<Pair<Sale, List<Installment>>>
): ReceiptData {
    val itemsSummary = mutableListOf<String>()
    var totalSold = 0.0
    var totalPaid = 0.0
    var totalRemaining = 0.0

    saleGroups.forEach { (sale, installments) ->
        itemsSummary.add("── فاتورة #${sale.id} — ${sale.status} ──")
        itemsSummary.add("الإجمالي: %.0f  •  المقدم: %.0f".format(sale.totalAmount, sale.downPayment))
        installments.forEach { inst ->
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale("ar")).format(Date(inst.dueDate))
            val statusLabel = when (inst.status) {
                InstallmentStatus.PAID -> "مسدد"
                InstallmentStatus.LATE -> "متأخر"
                InstallmentStatus.PENDING -> "غير مسدد"
            }
            itemsSummary.add("قسط ${inst.installmentNumber} ($dateStr): %.0f/%.0f [$statusLabel]".format(inst.paidAmount, inst.amount))
        }
        totalSold += sale.totalAmount
        val paidForSale = installments.sumOf { it.paidAmount } + sale.downPayment
        totalPaid += paidForSale
        totalRemaining += (sale.totalAmount - paidForSale).coerceAtLeast(0.0)
    }

    return ReceiptData(
        storeName = storeName,
        title = "كشف حساب العميل",
        customerName = customer.name,
        customerPhone = customer.phone,
        itemsSummary = itemsSummary,
        lines = listOf(
            ReceiptLine("عدد الفواتير", saleGroups.size.toString()),
            ReceiptLine("إجمالي المبيعات", "%.0f".format(totalSold)),
            ReceiptLine("إجمالي المُحصَّل", "%.0f".format(totalPaid)),
            ReceiptLine("إجمالي المتبقي", "%.0f".format(totalRemaining), bold = true)
        )
    )
}

@Composable
private fun SaleCard(
    sale: Sale,
    viewModel: AppViewModel,
    onPay: (Installment) -> Unit,
    onPrintInvoice: (Sale) -> Unit
) {
    val installmentsFlow = remember(sale.id) { viewModel.repository.getInstallmentsForSale(sale.id) }
    val installments by installmentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("فاتورة #${sale.id} — الإجمالي %.0f".format(sale.totalAmount), fontWeight = FontWeight.Bold)
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "إخفاء" else "الأقساط") }
            }
            Text("الحالة: ${sale.status}", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onPrintInvoice(sale) }) { Text("طباعة الفاتورة") }

            if (expanded) {
                installments.forEach { inst ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("قسط ${inst.installmentNumber}")
                            Text(
                                SimpleDateFormat("yyyy-MM-dd", Locale("ar")).format(Date(inst.dueDate)),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text("%.0f / %.0f".format(inst.paidAmount, inst.amount))
                        if (inst.status != InstallmentStatus.PAID) {
                            Button(onClick = { onPay(inst) }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                                Text("تسديد")
                            }
                        } else {
                            AssistChip(onClick = {}, label = { Text("مسدد") })
                        }
                    }
                }
            }
        }
    }
}
