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
                            val receipt = ReceiptData(
                                storeName = "متجرنا",
                                title = "فاتورة بيع بالتقسيط رقم ${s.id}",
                                customerName = cust.name,
                                customerPhone = cust.phone,
                                itemsSummary = items.map { "${it.name} × ${it.quantity} = ${"%.0f".format(it.lineTotal)}" },
                                lines = listOf(
                                    ReceiptLine("الإجمالي", "%.0f".format(s.totalAmount)),
                                    ReceiptLine("الدفعة المقدمة", "%.0f".format(s.downPayment)),
                                    ReceiptLine("عدد الأقساط", s.numberOfInstallments.toString()),
                                    ReceiptLine("قيمة القسط", "%.0f".format(s.installmentAmount), bold = true)
                                )
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
            installment = inst,
            customer = customer,
            onDismiss = { payDialogInstallment = null },
            onConfirmed = { navController.navigate(Routes.RECEIPT_PREVIEW) },
            viewModel = viewModel
        )
    }
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

@Composable
private fun PayInstallmentDialog(
    installment: Installment,
    customer: Customer?,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit,
    viewModel: AppViewModel
) {
    val remaining = installment.amount - installment.paidAmount
    var amountText by remember { mutableStateOf(remaining.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسديد القسط رقم ${installment.installmentNumber}") },
        text = {
            Column {
                Text("المتبقي على هذا القسط: %.0f".format(remaining))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ المسدد الآن") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountText.toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    viewModel.payInstallment(installment.id, amount) { payment ->
                        val receipt = ReceiptData(
                            storeName = "متجرنا",
                            title = "وصل استلام دفعة",
                            customerName = customer?.name ?: "",
                            customerPhone = customer?.phone ?: "",
                            lines = listOf(
                                ReceiptLine("القسط رقم", installment.installmentNumber.toString()),
                                ReceiptLine("المبلغ المسدد", "%.0f".format(amount), bold = true),
                                ReceiptLine("المتبقي على القسط", "%.0f".format((remaining - amount).coerceAtLeast(0.0)))
                            )
                        )
                        viewModel.setPendingReceipt(receipt)
                    }
                    onDismiss()
                    onConfirmed()
                }
            }) { Text("تأكيد وطباعة الوصل") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
