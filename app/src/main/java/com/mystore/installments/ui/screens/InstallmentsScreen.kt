package com.mystore.installments.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mystore.installments.data.entity.Installment
import com.mystore.installments.data.entity.InstallmentStatus
import com.mystore.installments.ui.components.AppBottomBar
import com.mystore.installments.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentsScreen(viewModel: AppViewModel, navController: NavController) {
    val installments by viewModel.unpaidInstallments.collectAsState()
    val now = System.currentTimeMillis()

    Scaffold(
        topBar = { TopAppBar(title = { Text("متابعة الأقساط") }) },
        bottomBar = { AppBottomBar(navController) }
    ) { padding ->
        if (installments.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("لا توجد أقساط مستحقة حالياً")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
                items(installments.sortedBy { it.dueDate }) { inst ->
                    val isLate = inst.dueDate < now
                    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Row(
                            Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
