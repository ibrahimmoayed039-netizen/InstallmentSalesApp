package com.mystore.installments.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mystore.installments.data.entity.Customer
import com.mystore.installments.data.entity.Product
import com.mystore.installments.data.entity.SaleItem
import com.mystore.installments.printer.ContractData
import com.mystore.installments.printer.ContractInstallmentLine
import com.mystore.installments.printer.ContractItemLine
import com.mystore.installments.printer.ReceiptData
import com.mystore.installments.printer.ReceiptItemLine
import com.mystore.installments.printer.ReceiptLine
import com.mystore.installments.ui.components.AppBottomBar
import com.mystore.installments.ui.components.PinDialog
import com.mystore.installments.ui.nav.Routes
import com.mystore.installments.util.formatAmount
import com.mystore.installments.viewmodel.AppViewModel
import kotlinx.coroutines.launch

// ---------- عنوان قسم صغير موحّد الشكل (أيقونة دائرية + نص) يُستخدم فوق كل بطاقة ----------
@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleScreen(viewModel: AppViewModel, navController: NavController) {
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()
    val unpaidWithCustomer by viewModel.unpaidInstallmentsWithCustomer.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerMenuExpanded by remember { mutableStateOf(false) }

    // اختيار المنتج من قائمة المخزون بدل كتابة اسم الصنف يدوياً
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var productMenuExpanded by remember { mutableStateOf(false) }
    var itemQty by remember { mutableStateOf("1") }
    var itemPrice by remember { mutableStateOf("") }
    val cartItems = remember { mutableStateListOf<SaleItem>() }

    var downPayment by remember { mutableStateOf("0") }
    var installmentsCount by remember { mutableStateOf("6") }

    // ---------- صلاحية تعديل السعر يدوياً / الخصم ----------
    // إن كان صاحب المحل قد عيّن رمز صلاحية (PIN) من الإعدادات، يبقى سعر كل صنف مقفلاً على
    // السعر الافتراضي للمنتج ولا يمكن تعديله أو إضافة خصم على الفاتورة إلا بعد إدخال الرمز.
    // إن لم يُعيَّن رمز أصلاً، يبقى التعديل متاحاً كما كان (بدون قيود) تفادياً لتعطيل من لا يريد هذه الميزة.
    val hasPin = viewModel.appSettings.hasPin
    var priceUnlocked by remember { mutableStateOf(!hasPin) }
    var showPricePinDialog by remember { mutableStateOf(false) }
    var discountText by remember { mutableStateOf("0") }
    var discountUnlocked by remember { mutableStateOf(!hasPin) }
    var showDiscountPinDialog by remember { mutableStateOf(false) }

    val grossTotal = cartItems.sumOf { it.lineTotal }
    val discount = (discountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, grossTotal)
    val totalAmount = grossTotal - discount
    val downPaymentValue = downPayment.toDoubleOrNull() ?: 0.0
    val installmentsCountValue = (installmentsCount.toIntOrNull() ?: 0).coerceAtLeast(0)
    val installmentPreview =
        if (installmentsCountValue > 0) (totalAmount - downPaymentValue) / installmentsCountValue else 0.0

    val canSave = selectedCustomer != null && cartItems.isNotEmpty() && installmentsCountValue > 0

    // ---------- تنبيه ذكي: هل عند العميل المختار أكثر من فاتورة متأخرة قبل الموافقة على بيع جديد ----------
    val now = remember { System.currentTimeMillis() }
    val overdueForSelectedCustomer = remember(selectedCustomer, unpaidWithCustomer) {
        val c = selectedCustomer
        if (c == null) emptyList()
        else unpaidWithCustomer.filter { it.customerId == c.id && it.installment.dueDate < now }
    }
    val overdueSaleIds = remember(overdueForSelectedCustomer) {
        overdueForSelectedCustomer.map { it.installment.saleId }.distinct()
    }
    val overdueRemainingTotal = remember(overdueForSelectedCustomer) {
        overdueForSelectedCustomer.sumOf { it.installment.amount - it.installment.paidAmount }
    }
    val hasMultipleOverdueInvoices = overdueSaleIds.size >= 2
    var showOverdueConfirmDialog by remember { mutableStateOf(false) }

    fun performSave() {
        val customer = selectedCustomer ?: return
        val down = downPaymentValue
        val count = installmentsCountValue
        if (cartItems.isNotEmpty() && count > 0) {
            viewModel.createSale(
                customerId = customer.id,
                items = cartItems.toList(),
                totalAmount = totalAmount,
                downPayment = down,
                numberOfInstallments = count,
                notes = "",
                discount = discount
            ) { saleId ->
                val installmentAmount = (totalAmount - down) / count
                val receiptLines = mutableListOf(
                    ReceiptLine("إجمالي الأصناف", formatAmount(grossTotal))
                )
                if (discount > 0) receiptLines.add(ReceiptLine("الخصم", formatAmount(discount)))
                receiptLines.add(ReceiptLine("الإجمالي بعد الخصم", formatAmount(totalAmount)))
                receiptLines.add(ReceiptLine("الدفعة المقدمة", formatAmount(down)))
                receiptLines.add(ReceiptLine("عدد الأقساط", count.toString()))
                receiptLines.add(ReceiptLine("قيمة القسط الشهري", formatAmount(installmentAmount), bold = true))
                val receipt = ReceiptData(
                    storeName = viewModel.appSettings.storeName.ifBlank { "متجرنا" },
                    title = "فاتورة بيع بالتقسيط رقم $saleId",
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    storeAddress = viewModel.appSettings.storeAddress,
                    storePhone = viewModel.appSettings.storePhone,
                    itemsSummary = cartItems.map { ReceiptItemLine("${it.name} × ${it.quantity}", formatAmount(it.lineTotal)) },
                    lines = receiptLines
                )

                // نبني بيانات عقد التقسيط (PDF قابل للتوقيع) من نفس عملية البيع، ونجلب جدول
                // الأقساط الفعلي المحفوظ بقاعدة البيانات بدل إعادة حسابه يدوياً هنا
                val itemsSnapshot = cartItems.toList()
                cartItems.clear()
                scope.launch {
                    val savedInstallments = viewModel.repository.getInstallmentsForSaleOnce(saleId)
                    val contract = ContractData(
                        saleId = saleId,
                        saleDateMillis = System.currentTimeMillis(),
                        storeName = viewModel.appSettings.storeName.ifBlank { "متجرنا" },
                        storeAddress = viewModel.appSettings.storeAddress,
                        storePhone = viewModel.appSettings.storePhone,
                        customerName = customer.name,
                        customerPhone = customer.phone,
                        customerAddress = customer.address,
                        items = itemsSnapshot.map { ContractItemLine(it.name, it.quantity, it.lineTotal) },
                        grossTotal = grossTotal,
                        discount = discount,
                        totalAmount = totalAmount,
                        downPayment = down,
                        installments = savedInstallments.map {
                            ContractInstallmentLine(it.installmentNumber, it.dueDate, it.amount)
                        }
                    )
                    viewModel.setPendingReceipt(receipt, contract)
                    navController.navigate(Routes.RECEIPT_PREVIEW)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بيع جديد بالتقسيط", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                // زر الحفظ ثابت فوق الشريط السفلي دائماً ظاهر بدون داعي للتمرير لآخر الشاشة
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        if (cartItems.isNotEmpty()) {
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الإجمالي بعد الخصم", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    formatAmount(totalAmount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Button(
                            enabled = canSave,
                            onClick = {
                                if (hasMultipleOverdueInvoices) {
                                    showOverdueConfirmDialog = true
                                } else {
                                    performSave()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("حفظ البيع وطباعة الفاتورة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                AppBottomBar(navController)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---------- بطاقة العميل ----------
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    SectionHeader(Icons.Filled.Person, "العميل")
                    Spacer(Modifier.height(10.dp))
                    ExposedDropdownMenuBox(expanded = customerMenuExpanded, onExpandedChange = { customerMenuExpanded = it }) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("اختر العميل") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = customerMenuExpanded, onDismissRequest = { customerMenuExpanded = false }) {
                            if (customers.isEmpty()) {
                                DropdownMenuItem(text = { Text("لا يوجد عملاء بعد") }, onClick = {}, enabled = false)
                            }
                            customers.forEach { c ->
                                DropdownMenuItem(text = { Text(c.name) }, onClick = {
                                    selectedCustomer = c
                                    customerMenuExpanded = false
                                })
                            }
                        }
                    }
                }
            }

            // ---------- تنبيه: العميل عنده أكثر من فاتورة متأخرة ----------
            if (hasMultipleOverdueInvoices) {
                Surface(
                    color = Color(0xFFFDECEA),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "تنبيه: عند هذا العميل ${overdueSaleIds.size} فواتير متأخرة",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7B1E1E)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "إجمالي المتأخر: ${formatAmount(overdueRemainingTotal)} — يُفضّل تحصيل جزء منه قبل بيع جديد بالتقسيط",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7B1E1E)
                            )
                        }
                    }
                }
            }

            // ---------- بطاقة إضافة الأصناف ----------
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    SectionHeader(Icons.Filled.Inventory2, "عناصر البيع")
                    Spacer(Modifier.height(10.dp))

                    if (products.isEmpty()) {
                        Text(
                            "لا توجد منتجات مسجّلة بعد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { navController.navigate(Routes.PRODUCT_FORM) },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("إضافة منتج الآن") }
                    } else {
                        ExposedDropdownMenuBox(expanded = productMenuExpanded, onExpandedChange = { productMenuExpanded = it }) {
                            OutlinedTextField(
                                value = selectedProduct?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("اختر المنتج") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = productMenuExpanded, onDismissRequest = { productMenuExpanded = false }) {
                                products.forEach { p ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(p.name, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "تقسيط: ${formatAmount(p.installmentPrice)}  •  نقداً: ${formatAmount(p.cashPrice)}  •  متوفر: ${p.stockQuantity}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedProduct = p
                                            itemPrice = if (p.installmentPrice > 0) p.installmentPrice.toString() else p.cashPrice.toString()
                                            productMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        selectedProduct?.let { p ->
                            val lowStock = p.stockQuantity in 1..3
                            val outOfStock = p.stockQuantity <= 0
                            if (outOfStock || lowStock) {
                                Text(
                                    if (outOfStock) "⚠ هذا المنتج نفد من المخزون" else "⚠ الكمية المتبقية بالمخزون: ${p.stockQuantity}",
                                    color = if (outOfStock) Color(0xFFC62828) else Color(0xFFEF6C00),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                            OutlinedTextField(
                                value = itemQty, onValueChange = { itemQty = it },
                                label = { Text("الكمية") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = itemPrice,
                                onValueChange = { if (priceUnlocked) itemPrice = it },
                                readOnly = !priceUnlocked,
                                label = { Text(if (priceUnlocked) "سعر البيع" else "سعر مقفل") },
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    if (hasPin) {
                                        IconButton(onClick = {
                                            if (priceUnlocked) priceUnlocked = false else showPricePinDialog = true
                                        }) {
                                            Icon(
                                                if (priceUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                                contentDescription = "تعديل السعر يدوياً"
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val product = selectedProduct ?: return@OutlinedButton
                                val qty = itemQty.toIntOrNull() ?: 1
                                val price = itemPrice.toDoubleOrNull() ?: 0.0
                                if (price > 0) {
                                    cartItems.add(
                                        SaleItem(
                                            saleId = 0,
                                            productId = product.id,
                                            name = product.name,
                                            quantity = qty,
                                            price = price
                                        )
                                    )
                                    selectedProduct = null
                                    itemQty = "1"
                                    itemPrice = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("إضافة الصنف")
                        }
                    }
                }
            }

            // ---------- بطاقة سلة الأصناف المضافة ----------
            if (cartItems.isNotEmpty()) {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader(Icons.Filled.ShoppingCart, "الأصناف المضافة (${cartItems.size})")
                        Spacer(Modifier.height(10.dp))
                        cartItems.forEachIndexed { index, item ->
                            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${item.quantity} × ${formatAmount(item.price)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    formatAmount(item.lineTotal),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                IconButton(onClick = { cartItems.remove(item) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---------- بطاقة الإجمالي والخصم ----------
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    SectionHeader(Icons.Filled.ReceiptLong, "الفاتورة")
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي الأصناف", style = MaterialTheme.typography.bodyMedium)
                        Text(formatAmount(grossTotal), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { if (discountUnlocked) discountText = it },
                        readOnly = !discountUnlocked,
                        label = { Text("خصم على الفاتورة") },
                        leadingIcon = { Icon(Icons.Filled.Percent, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (hasPin) {
                                IconButton(onClick = {
                                    if (discountUnlocked) discountUnlocked = false else showDiscountPinDialog = true
                                }) {
                                    Icon(
                                        if (discountUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                        contentDescription = "إضافة خصم"
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "الإجمالي بعد الخصم",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                formatAmount(totalAmount),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // ---------- بطاقة خطة التقسيط ----------
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    SectionHeader(Icons.Filled.CalendarMonth, "خطة التقسيط")
                    Spacer(Modifier.height(10.dp))
                    Row {
                        OutlinedTextField(
                            value = downPayment, onValueChange = { downPayment = it },
                            label = { Text("الدفعة المقدمة") },
                            leadingIcon = { Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = installmentsCount, onValueChange = { installmentsCount = it },
                            label = { Text("عدد الأقساط") },
                            leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (installmentsCountValue > 0 && totalAmount > 0) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "قيمة القسط الشهري",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    formatAmount(installmentPreview),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // مساحة إضافية أسفل المحتوى حتى لا يختفي آخر عنصر خلف زر الحفظ الثابت
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showOverdueConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showOverdueConfirmDialog = false },
            icon = { Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Color(0xFFC62828)) },
            title = { Text("عند هذا العميل ${overdueSaleIds.size} فواتير متأخرة") },
            text = {
                Text("إجمالي المتأخر عليه ${formatAmount(overdueRemainingTotal)}. هل متأكد إنك بدك تكمل بيع جديد بالتقسيط له؟")
            },
            confirmButton = {
                TextButton(onClick = {
                    showOverdueConfirmDialog = false
                    performSave()
                }) { Text("متابعة البيع", color = Color(0xFFC62828)) }
            },
            dismissButton = {
                TextButton(onClick = { showOverdueConfirmDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showPricePinDialog) {
        PinDialog(
            title = "صلاحية تعديل السعر",
            appSettings = viewModel.appSettings,
            onDismiss = { showPricePinDialog = false },
            onSuccess = { priceUnlocked = true }
        )
    }
    if (showDiscountPinDialog) {
        PinDialog(
            title = "صلاحية إضافة خصم",
            appSettings = viewModel.appSettings,
            onDismiss = { showDiscountPinDialog = false },
            onSuccess = { discountUnlocked = true }
        )
    }
}
