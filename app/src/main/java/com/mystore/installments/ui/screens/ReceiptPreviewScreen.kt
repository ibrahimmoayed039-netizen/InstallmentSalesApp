package com.mystore.installments.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
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

            // منطقة معاينة الورق: خلفية رمادية فاتحة تُبرز شريط الورق الأبيض في المنتصف،
            // بحواف مسننة أعلى وأسفل لمحاكاة شكل الورق الحراري الفعلي عند قصّه من البكرة
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFDCE0E3)),
                contentAlignment = Alignment.TopCenter
            ) {
                val data = receipt
                if (data == null) {
                    Text("لا يوجد وصل للمعاينة", modifier = Modifier.padding(24.dp))
                } else {
                    val charsPerLine = viewModel.printerManager.paperWidth.charsPerLine
                    val previewLines = ReceiptBuilder.buildPreviewLines(data, charsPerLine)
                    val logoUri = viewModel.appSettings.storeLogoUri

                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .fillMaxHeight()
                            .padding(vertical = 10.dp)
                    ) {
                        JaggedEdge(color = Color.White, pointingDown = false)
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            if (logoUri != null) {
                                item {
                                    AsyncImage(
                                        model = logoUri,
                                        contentDescription = "شعار المحل",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(90.dp)
                                            .padding(bottom = 8.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                            items(previewLines) { line ->
                                Text(
                                    text = line.text,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (line.bold) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = Color.Black,
                                    textAlign = if (line.centered) TextAlign.Center else TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        JaggedEdge(color = Color.White, pointingDown = true)
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

/**
 * حافة مسننة بسيطة (خط "أسنان منشار") لمحاكاة شكل حافة الورق الحراري المقصوص من البكرة.
 * pointingDown يحدد اتجاه الأسنان: false لحافة أعلى الورقة، true لحافة أسفلها.
 */
@Composable
private fun JaggedEdge(
    color: Color,
    pointingDown: Boolean,
    toothWidth: Dp = 12.dp,
    toothHeight: Dp = 7.dp
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(toothHeight)
    ) {
        val toothPx = toothWidth.toPx()
        val heightPx = toothHeight.toPx()
        val teethCount = (size.width / toothPx).toInt() + 2
        val path = Path()

        if (pointingDown) {
            // حافة أسفل الورقة: مستوية من الأعلى، مسننة من الأسفل
            path.moveTo(0f, 0f)
            for (i in 0..teethCount) {
                val x = (i * toothPx).coerceAtMost(size.width)
                val y = if (i % 2 == 0) 0f else heightPx
                path.lineTo(x, y)
            }
            path.lineTo(size.width, 0f)
        } else {
            // حافة أعلى الورقة: مسننة من الأعلى، مستوية من الأسفل
            path.moveTo(0f, heightPx)
            for (i in 0..teethCount) {
                val x = (i * toothPx).coerceAtMost(size.width)
                val y = if (i % 2 == 0) heightPx else 0f
                path.lineTo(x, y)
            }
            path.lineTo(size.width, heightPx)
        }
        path.close()
        drawPath(path, color)
    }
}
