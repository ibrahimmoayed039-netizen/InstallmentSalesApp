package com.mystore.installments.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.mystore.installments.backup.BackupManager
import com.mystore.installments.printer.PaperWidth
import com.mystore.installments.printer.PrinterConnectionType
import com.mystore.installments.printer.RawCommandParser
import com.mystore.installments.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة إعدادات الطابعة: اختيار عرض الورق (58/80مم)، ثم الاتصال إما عبر
 * البلوتوث (من الأجهزة المقترنة مسبقاً أو عبر البحث المباشر) أو عبر USB (كابل OTG).
 *
 * مهم: صلاحيات BLUETOOTH_CONNECT / BLUETOOTH_SCAN مطلوبة إجبارياً وقت التشغيل
 * على أندرويد 12+ (API 31+)، وبدونها كانت الشاشة تتعطّل فوراً عند الدخول لأن
 * قراءة الأجهزة المقترنة كانت تُستدعى مباشرة دون طلب الصلاحية أولاً. الآن تُطلب
 * الصلاحية أولاً، وتُعرض الأجهزة فقط بعد منحها.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val connectionType by viewModel.printerManager.connectionType.collectAsState()
    val savedCodeTable by viewModel.printerManager.codeTable.collectAsState()

    var paperWidth by remember { mutableStateOf(PaperWidth.MM80) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var codeTableInput by remember(savedCodeTable) { mutableStateOf(savedCodeTable?.toString() ?: "") }
    var customCommandInput by remember { mutableStateOf("") }

    // ---------- بيانات المحل (اسم + عنوان + هاتف + شعار يظهر أعلى كل فاتورة) ----------
    var storeName by remember { mutableStateOf(viewModel.appSettings.storeName) }
    var storeAddress by remember { mutableStateOf(viewModel.appSettings.storeAddress) }
    var storePhone by remember { mutableStateOf(viewModel.appSettings.storePhone) }
    var storeLogoUri by remember { mutableStateOf(viewModel.appSettings.storeLogoUri?.let { Uri.parse(it) }) }
    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { /* بعض المصادر لا تدعم الصلاحية الدائمة */ }
            storeLogoUri = uri
            viewModel.appSettings.storeLogoUri = uri.toString()
        }
    }

    // ---------- صلاحية تعديل السعر/الخصم (PIN) ----------
    var hasPin by remember { mutableStateOf(viewModel.appSettings.hasPin) }
    var newPin by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<String?>(null) }

    // ---------- نسخ احتياطي واستعادة ----------
    val scopeBackup = rememberCoroutineScope()
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirm by remember { mutableStateOf<Uri?>(null) }
    val backupFileName = remember {
        "installment_sales_backup_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale("ar")).format(Date()) + ".db"
    }
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            scopeBackup.launch {
                val ok = BackupManager.backupTo(context, uri)
                backupMessage = if (ok) "تم حفظ النسخة الاحتياطية بنجاح ✅" else "فشل إنشاء النسخة الاحتياطية"
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) showRestoreConfirm = uri
    }

    // ---------- إدارة صلاحيات البلوتوث وقت التشغيل ----------
    val requiredBtPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    fun hasBtPermissions(): Boolean = requiredBtPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    var btPermissionGranted by remember { mutableStateOf(hasBtPermissions()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        btPermissionGranted = results.values.all { it }
        if (!btPermissionGranted) {
            statusMessage = "لم تُمنح صلاحية البلوتوث؛ لن تظهر الأجهزة المقترنة أو نتائج البحث"
        }
    }
    // نطلب الصلاحية تلقائياً عند فتح الشاشة إن لم تكن ممنوحة بعد
    LaunchedEffect(Unit) {
        if (!btPermissionGranted) permissionLauncher.launch(requiredBtPermissions)
    }

    // إعادة قراءة الأجهزة المقترنة فقط بعد التأكد من منح الصلاحية
    var pairedDevices by remember { mutableStateOf(emptyList<BluetoothDevice>()) }
    LaunchedEffect(btPermissionGranted) {
        pairedDevices = if (btPermissionGranted) viewModel.printerManager.pairedBluetoothDevices() else emptyList()
    }

    // ---------- البحث المباشر عن طابعات بلوتوث قريبة ----------
    var isScanning by remember { mutableStateOf(false) }
    var discoveredDevices by remember { mutableStateOf(listOf<BluetoothDevice>()) }
    val pairedAddresses = remember(pairedDevices) { pairedDevices.map { it.address }.toSet() }

    fun startScan() {
        if (!btPermissionGranted) {
            permissionLauncher.launch(requiredBtPermissions)
            return
        }
        discoveredDevices = emptyList()
        isScanning = true
        viewModel.printerManager.startBluetoothDiscovery(
            onDeviceFound = { device ->
                if (device.address !in pairedAddresses && discoveredDevices.none { it.address == device.address }) {
                    discoveredDevices = discoveredDevices + device
                }
            },
            onFinished = { isScanning = false }
        )
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.printerManager.stopBluetoothDiscovery() }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("الإعدادات") }) }) { padding ->
        // تمرير عمودي للشاشة كاملة، لأن المحتوى (بيانات المحل + بلوتوث + USB + إصلاح العربية +
        // الأوامر المخصّصة + النسخ الاحتياطي) أطول من الشاشة على أغلب الهواتف
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            // ---------- بيانات المحل: الاسم والشعار (يظهران أعلى كل فاتورة مطبوعة) ----------
            Text("بيانات المحل", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { logoPicker.launch(arrayOf("image/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    if (storeLogoUri != null) {
                        AsyncImage(
                            model = storeLogoUri,
                            contentDescription = "شعار المحل",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                storeLogoUri = null
                                viewModel.appSettings.clearLogo()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f), RoundedCornerShape(50))
                                .size(20.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "إزالة الشعار", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                        }
                    } else {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = "إضافة شعار")
                    }
                }
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it; viewModel.appSettings.storeName = it },
                    label = { Text("اسم المحل (يظهر أعلى الفاتورة)") },
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "اضغط على الصورة لاختيار شعار المحل؛ سيُطبع أعلى كل فاتورة ووصل بشكل تلقائي.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = storeAddress,
                onValueChange = { storeAddress = it; viewModel.appSettings.storeAddress = it },
                label = { Text("عنوان المحل (اختياري، يظهر أعلى الفاتورة)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = storePhone,
                onValueChange = { storePhone = it; viewModel.appSettings.storePhone = it },
                label = { Text("هاتف المحل (اختياري، يظهر أعلى الفاتورة)") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            // ---------- صلاحية تعديل السعر يدوياً/الخصم وقت البيع ----------
            Text("صلاحية الخصم وتعديل السعر", style = MaterialTheme.typography.titleMedium)
            Text(
                if (hasPin) "مفعّلة حالياً: يُطلب رمز الصلاحية عند تعديل السعر أو إضافة خصم في شاشة البيع."
                else "غير مفعّلة: يمكن لأي مستخدم تعديل السعر أو إضافة خصم دون قيود. عيّن رمزاً أدناه لتفعيلها.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it.filter { c -> c.isDigit() } },
                    label = { Text(if (hasPin) "رمز جديد (لتغييره)" else "عيّن رمز صلاحية") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (newPin.length >= 4) {
                        viewModel.appSettings.setPin(newPin)
                        hasPin = true
                        newPin = ""
                        pinMessage = "تم حفظ رمز الصلاحية"
                    } else {
                        pinMessage = "الرمز يجب أن يكون 4 أرقام على الأقل"
                    }
                }) { Text("حفظ") }
            }
            if (hasPin) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = {
                    viewModel.appSettings.clearPin()
                    hasPin = false
                    pinMessage = "تم إلغاء صلاحية القفل، التعديل أصبح متاحاً للجميع"
                }) { Text("إلغاء تفعيل الصلاحية") }
            }
            pinMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            // ---------- نسخ احتياطي واستعادة لقاعدة البيانات ----------
            Text("نسخ احتياطي واستعادة", style = MaterialTheme.typography.titleMedium)
            Text(
                "احفظ نسخة من كل بيانات التطبيق (العملاء، المنتجات، الفواتير، الأقساط) في مكان تختاره " +
                    "(تخزين الهاتف، بطاقة ذاكرة، أو أي تطبيق مزامنة سحابي). يُنصح بأخذ نسخة بشكل دوري.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { backupLauncher.launch(backupFileName) },
                    modifier = Modifier.weight(1f)
                ) { Text("💾 إنشاء نسخة احتياطية") }
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f)
                ) { Text("♻️ استعادة من نسخة") }
            }
            backupMessage?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

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

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

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

            if (!btPermissionGranted) {
                Spacer(Modifier.height(8.dp))
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text("يحتاج التطبيق صلاحية البلوتوث لعرض الطابعات والاتصال بها.")
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = { permissionLauncher.launch(requiredBtPermissions) }) {
                            Text("منح صلاحية البلوتوث")
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("الاتصال عبر البلوتوث", style = MaterialTheme.typography.titleMedium)
                if (isScanning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("جارٍ البحث...", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    TextButton(onClick = { startScan() }) { Text("🔍 بحث عن طابعة") }
                }
            }
            Text("قم بإقران الطابعة أولاً من إعدادات بلوتوث الهاتف، أو اضغط \"بحث عن طابعة\" لاكتشافها مباشرة:",
                style = MaterialTheme.typography.bodyMedium)

            if (pairedDevices.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("الأجهزة المقترنة", style = MaterialTheme.typography.labelLarge)
                LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                    items(pairedDevices) { device ->
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
            }

            if (discoveredDevices.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("أجهزة مكتشَفة قريبة (غير مقترنة)", style = MaterialTheme.typography.labelLarge)
                LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                    items(discoveredDevices) { device ->
                        ListItem(
                            headlineContent = { Text(device.name ?: "جهاز غير معروف") },
                            supportingContent = { Text(device.address) },
                            trailingContent = {
                                Button(onClick = {
                                    scope.launch {
                                        viewModel.printerManager.stopBluetoothDiscovery()
                                        isScanning = false
                                        val ok = viewModel.printerManager.connectBluetooth(device)
                                        statusMessage = if (ok) "تم الاتصال بنجاح" else "فشل الاتصال؛ قد تحتاج لإقران الجهاز أولاً من إعدادات الهاتف"
                                    }
                                }) { Text("اتصال") }
                            }
                        )
                    }
                }
            } else if (!isScanning && pairedDevices.isEmpty() && btPermissionGranted) {
                Text("لا توجد أجهزة مقترنة، اضغط \"بحث عن طابعة\" لاكتشافها.", style = MaterialTheme.typography.bodySmall)
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

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            // ---------- اختبار جداول الحروف لإصلاح ظهور اللغة العربية ----------
            Text("إصلاح ظهور اللغة العربية", style = MaterialTheme.typography.titleMedium)
            Text(
                "إذا كانت الطابعة تطبع رموزاً غريبة بدل النص العربي، اطبع ورقة الاختبار التالية. " +
                    "ستظهر نفس الجملة العربية مكررة تحت كل رقم جدول من CP0 إلى CP47 ثم CP255. " +
                    "ابحث عن الرقم الذي تظهر تحته الجملة بشكل عربي صحيح، ثم أدخله أدناه واحفظه.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        val ok = viewModel.printerManager.printCharacterTableTest()
                        statusMessage = if (ok) "تم إرسال ورقة الاختبار للطابعة" else "الطابعة غير متصلة، اتصل بها أولاً"
                    }
                },
                enabled = connectionType != PrinterConnectionType.NONE,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔍 اختبار جداول الحروف (لإصلاح اللغة العربية)")
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = codeTableInput,
                    onValueChange = { codeTableInput = it },
                    label = { Text("رقم الجدول الصحيح (مثال: 22)") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val n = codeTableInput.toIntOrNull()
                    viewModel.printerManager.setCodeTable(n)
                    statusMessage = if (n != null) "تم حفظ جدول الحروف رقم $n، سيُستخدم في كل الفواتير القادمة"
                    else "تم إلغاء تخصيص جدول الحروف"
                }) { Text("حفظ") }
            }
            if (savedCodeTable != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "الجدول المحفوظ حالياً: CP$savedCodeTable",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            // ---------- إرسال/تغيير أوامر طباعة مخصّصة (متقدّم) ----------
            Text("أوامر طباعة مخصّصة (متقدّم)", style = MaterialTheme.typography.titleMedium)
            Text(
                "لتجربة أو تغيير أوامر ESC/POS الخام يدوياً: اكتب البايتات بصيغة hex مفصولة بمسافات، " +
                    "مثال: 1B 40 1B 61 01 (تهيئة الطابعة ثم توسيط النص).",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = customCommandInput,
                onValueChange = { customCommandInput = it },
                label = { Text("أمر hex، مثال: 1B 40") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val bytes = RawCommandParser.parse(customCommandInput)
                    if (bytes == null) {
                        statusMessage = "صيغة الأمر غير صحيحة، تأكد من كتابته بصيغة hex مفصولة بمسافات"
                    } else {
                        scope.launch {
                            val ok = viewModel.printerManager.printRawBytes(bytes)
                            statusMessage = if (ok) "تم إرسال الأمر المخصّص للطابعة" else "الطابعة غير متصلة، اتصل بها أولاً"
                        }
                    }
                },
                enabled = connectionType != PrinterConnectionType.NONE,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إرسال الأمر المخصّص")
            }

            statusMessage?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    // تأكيد قبل الاستعادة، لأنها تستبدل كل البيانات الحالية بمحتوى الملف المختار (إجراء لا يمكن التراجع عنه)
    showRestoreConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = null },
            title = { Text("تأكيد الاستعادة") },
            text = {
                Text(
                    "سيتم استبدال كل البيانات الحالية (العملاء، الفواتير، الأقساط) بمحتوى النسخة " +
                        "المختارة، ولا يمكن التراجع عن هذا الإجراء. سيُعاد تشغيل التطبيق تلقائياً بعد الاستعادة."
                )
            },
            confirmButton = {
                Button(onClick = {
                    val target = uri
                    showRestoreConfirm = null
                    scopeBackup.launch {
                        val ok = BackupManager.restoreFrom(context, target)
                        if (ok) {
                            BackupManager.restartApp(context)
                        } else {
                            backupMessage = "فشلت عملية الاستعادة، تأكد أن الملف المختار نسخة احتياطية صحيحة"
                        }
                    }
                }) { Text("استعادة الآن") }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = null }) { Text("إلغاء") } }
        )
    }
}
