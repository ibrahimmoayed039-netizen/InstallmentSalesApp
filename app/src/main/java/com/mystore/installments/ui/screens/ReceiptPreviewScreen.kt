package com.mystore.installments.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mystore.installments.printer.PrinterConnectionType
import com.mystore.installments.printer.ReceiptBuilder
import com.mystore.installments.ui.nav.Routes
import com.mystore.installments.viewmodel.AppViewModel
import kotlinx.coroutines.launch

/**
 * شاشة معاينة الوصل/الفاتورة قبل الطباعة الفعلية.
 * تعرض نفس تنسيق المحتوى الذي سيُرسل للطابعة الحرارية (58/80مم) بدقة،
 * حتى لا تكون هناك أي مفاجآت بعد الطباعة الفعلية على الورق.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPreviewScreen(viewModel: AppViewModel, navController: NavController) {
    val receipt by viewModel.pendingReceipt.collectAsState()
    val connectionType by viewModel.printerManager.connectionType.collectAsState()
    val scope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("معاينة قبل الطباعة") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            if (connectionType == PrinterConnectionType.NONE) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("⚠️ لا توجد طابعة متصلة حالياً.")
                        TextButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                            Text("الذهاب لإعدادات الطابعة")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ورقة المعاينة بنفس عرض الطابعة الحرارية تقريباً (محاكاة بصرية)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val data = receipt
                if (data == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("لا يوجد وصل للمعاينة")
                    }
                } else {
                    val charsPerLine = viewModel.printerManager.paperWidth.charsPerLine
                    val previewLines = ReceiptBuilder.buildPreviewLines(data, charsPerLine)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                        items(previewLines) { line ->
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            resultMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = if (it.contains("فشل")) Color(0xFFC62828) else Color(0xFF2E7D32))
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                    Text("رجوع بدون طباعة")
                }
                Button(
                    enabled = receipt != null && connectionType != PrinterConnectionType.NONE && !isPrinting,
                    onClick = {
                        isPrinting = true
                        scope.launch {
                            val success = viewModel.printPendingReceipt()
                            resultMessage = if (success) "تمت الطباعة بنجاح ✅" else "فشل الطباعة، تحقق من الاتصال بالطابعة"
                            isPrinting = false
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isPrinting) "جارِ الطباعة..." else "طباعة الآن")
                }
            }
        }
    }
}
