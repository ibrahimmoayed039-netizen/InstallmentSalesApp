package com.mystore.installments.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mystore.installments.printer.PaperWidth
import com.mystore.installments.printer.PrinterConnectionType
import com.mystore.installments.viewmodel.AppViewModel
import kotlinx.coroutines.launch

/**
 * شاشة إعدادات الطابعة: اختيار عرض الورق (58/80مم)، ثم الاتصال إما عبر
 * البلوتوث (من الأجهزة المقترنة مسبقاً) أو عبر USB (كابل OTG).
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val connectionType by viewModel.printerManager.connectionType.collectAsState()

    var paperWidth by remember { mutableStateOf(PaperWidth.MM80) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("إعدادات الطابعة") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            Text("عرض الورق", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.padding(vertical = 8.dp)) {
                FilterChip(
                    selected = paperWidth == PaperWidth.MM58,
                    onClick = { paperWidth = PaperWidth.MM58; viewModel.printerManager.paperWidth = PaperWidth.MM58 },
                    label = { Text("58 مم") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = paperWidth == PaperWidth.MM80,
                    onClick = { paperWidth = PaperWidth.MM80; viewModel.printerManager.paperWidth = PaperWidth.MM80 },
                    label = { Text("80 مم") }
                )
            }

            Divider(Modifier.padding(vertical = 12.dp))

            Text(
                "حالة الاتصال: " + when (connectionType) {
                    PrinterConnectionType.BLUETOOTH -> "متصل عبر البلوتوث ✅"
                    PrinterConnectionType.USB -> "متصل عبر USB ✅"
                    PrinterConnectionType.NONE -> "غير متصل ❌"
                },
                style = MaterialTheme.typography.titleMedium
            )
            if (connectionType != PrinterConnectionType.NONE) {
                TextButton(onClick = { viewModel.printerManager.disconnect() }) { Text("قطع الاتصال") }
            }

            Spacer(Modifier.height(12.dp))
            Text("الاتصال عبر البلوتوث", style = MaterialTheme.typography.titleMedium)
            Text("قم بإقران الطابعة أولاً من إعدادات بلوتوث الهاتف، ثم اختَرها من القائمة:",
                style = MaterialTheme.typography.bodyMedium)

            val bluetoothDevices = remember { viewModel.printerManager.pairedBluetoothDevices() }
            LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                items(bluetoothDevices) { device ->
                    ListItem(
                        headlineContent = { Text(device.name ?: device.address) },
                        supportingContent = { Text(device.address) },
                        trailingContent = {
                            Button(onClick = {
                                scope.launch {
                                    val ok = viewModel.printerManager.connectBluetooth(device)
                                    statusMessage = if (ok) "تم الاتصال بنجاح" else "فشل الاتصال بالطابعة"
                                }
                            }) { Text("اتصال") }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("الاتصال عبر USB (كابل OTG)", style = MaterialTheme.typography.titleMedium)
            val usbDevices = remember { viewModel.printerManager.connectedUsbDevices() }
            if (usbDevices.isEmpty()) {
                Text("لم يتم العثور على جهاز USB متصل", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                    items(usbDevices) { device ->
                        ListItem(
                            headlineContent = { Text(device.deviceName) },
                            trailingContent = {
                                Button(onClick = {
                                    if (!viewModel.printerManager.hasUsbPermission(device)) {
                                        viewModel.printerManager.requestUsbPermission(device)
                                        statusMessage = "يرجى الموافقة على صلاحية الوصول لجهاز USB ثم إعادة المحاولة"
                                    } else {
                                        scope.launch {
                                            val ok = viewModel.printerManager.connectUsb(device)
                                            statusMessage = if (ok) "تم الاتصال بنجاح" else "فشل الاتصال بالطابعة"
                                        }
                                    }
                                }) { Text("اتصال") }
                            }
                        )
                    }
                }
            }

            statusMessage?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
