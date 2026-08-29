package com.mystore.installments.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mystore.installments.printer.ReceiptData
import com.mystore.installments.printer.ReceiptLine
import com.mystore.installments.util.formatAmount
import com.mystore.installments.viewmodel.AppViewModel

/**
 * نافذة تسديد قسط، تُستخدم من أكثر من شاشة (تفاصيل العميل، ومتابعة الأقساط)
 * حتى لا يتكرر نفس المنطق. تسدد المبلغ عبر الـ ViewModel وتجهّز وصل استلام دفعة للطباعة.
 */
@Composable
fun PayInstallmentDialog(
    installmentId: Long,
    installmentNumber: Int,
    remaining: Double,
    customerName: String,
    customerPhone: String,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit,
    viewModel: AppViewModel
) {
    var amountText by remember { mutableStateOf(remaining.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسديد القسط رقم $installmentNumber") },
        text = {
            Column {
                Text("المتبقي على هذا القسط: ${formatAmount(remaining)}")
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
                    viewModel.payInstallment(installmentId, amount) { _ ->
                        val receipt = ReceiptData(
                            storeName = viewModel.appSettings.storeName.ifBlank { "متجرنا" },
                            title = "وصل استلام دفعة",
                            customerName = customerName,
                            customerPhone = customerPhone,
                            storeAddress = viewModel.appSettings.storeAddress,
                            storePhone = viewModel.appSettings.storePhone,
                            lines = listOf(
                                ReceiptLine("القسط رقم", installmentNumber.toString()),
                                ReceiptLine("المبلغ المسدد", formatAmount(amount), bold = true),
                                ReceiptLine("المتبقي على القسط", formatAmount((remaining - amount).coerceAtLeast(0.0)))
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
