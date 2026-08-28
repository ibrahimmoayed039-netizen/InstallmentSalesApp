package com.mystore.installments.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mystore.installments.data.entity.Customer
import com.mystore.installments.data.entity.Product
import com.mystore.installments.data.entity.SaleItem
import com.mystore.installments.printer.ReceiptData
import com.mystore.installments.printer.ReceiptLine
import com.mystore.installments.ui.components.AppBottomBar
import com.mystore.installments.ui.components.PinDialog
import com.mystore.installments.ui.nav.Routes
import com.mystore.installments.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleScreen(viewModel: AppViewModel, navController: NavController) {
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()

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

    Scaffold(
        topBar = { TopAppBar(title = { Text("بيع جديد بالتقسيط") }) },
        bottomBar = { AppBottomBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // اختيار العميل
            ExposedDropdownMenuBox(expanded = customerMenuExpanded, onExpandedChange = { customerMenuExpanded = it }) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "اختر العميل",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("العميل") },
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

            Spacer(Modifier.height(12.dp))
            Text("عناصر البيع", style = MaterialTheme.typography.titleMedium)

            if (products.isEmpty()) {
                // لا توجد منتجات بعد: توجيه المستخدم لإضافة منتجات أولاً
                ElevatedCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("لا توجد منتجات مسجّلة بعد.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { navController.navigate(Routes.PRODUCT_FORM) }) {
                            Text("إضافة منتج الآن")
                        }
                    }
                }
            } else {
                // اختيار المنتج من القائمة
                ExposedDropdownMenuBox(expanded = productMenuExpanded, onExpandedChange = { productMenuExpanded = it }) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: "اختر المنتج",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المنتج") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = productMenuExpanded, onDismissRequest = { productMenuExpanded = false }) {
                        products.forEach { p ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(p.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "تقسيط: ${p.installmentPrice}  •  نقداً: ${p.cashPrice}",
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

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = itemQty, onValueChange = { itemQty = it },
                        label = { Text("الكمية") }, modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = itemPrice,
                        onValueChange = { if (priceUnlocked) itemPrice = it },
                        readOnly = !priceUnlocked,
                        label = { Text(if (priceUnlocked) "سعر البيع (قابل للتعديل)" else "سعر البيع بالتقسيط") },
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
                Button(
                    onClick = {
                        val product = selectedProduct ?: return@Button
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
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("إضافة الصنف")
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                items(cartItems) { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${item.name} × ${item.quantity} = %.0f".format(item.lineTotal))
                        IconButton(onClick = { cartItems.remove(item) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف")
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("إجمالي الأصناف: %.0f".format(grossTotal), style = MaterialTheme.typography.bodyMedium)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { if (discountUnlocked) discountText = it },
                    readOnly = !discountUnlocked,
                    label = { Text("خصم على الفاتورة") },
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
                    modifier = Modifier.weight(1f)
                )
            }
            Text("الإجمالي بعد الخصم: %.0f".format(totalAmount), style = MaterialTheme.typography.titleMedium)

            Row(Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = downPayment, onValueChange = { downPayment = it },
                    label = { Text("الدفعة المقدمة") }, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = installmentsCount, onValueChange = { installmentsCount = it },
                    label = { Text("عدد الأقساط") }, modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    val customer = selectedCustomer ?: return@Button
                    val down = downPayment.toDoubleOrNull() ?: 0.0
                    val count = installmentsCount.toIntOrNull() ?: 1
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
                                ReceiptLine("إجمالي الأصناف", "%.0f".format(grossTotal))
                            )
                            if (discount > 0) receiptLines.add(ReceiptLine("الخصم", "%.0f".format(discount)))
                            receiptLines.add(ReceiptLine("الإجمالي بعد الخصم", "%.0f".format(totalAmount)))
                            receiptLines.add(ReceiptLine("الدفعة المقدمة", "%.0f".format(down)))
                            receiptLines.add(ReceiptLine("عدد الأقساط", count.toString()))
                            receiptLines.add(ReceiptLine("قيمة القسط الشهري", "%.0f".format(installmentAmount), bold = true))
                            val receipt = ReceiptData(
                                storeName = viewModel.appSettings.storeName.ifBlank { "متجرنا" },
                                title = "فاتورة بيع بالتقسيط رقم $saleId",
                                customerName = customer.name,
                                customerPhone = customer.phone,
                                itemsSummary = cartItems.map { "${it.name} × ${it.quantity} = %.0f".format(it.lineTotal) },
                                lines = receiptLines
                            )
                            viewModel.setPendingReceipt(receipt)
                            cartItems.clear()
                            navController.navigate(Routes.RECEIPT_PREVIEW)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) { Text("حفظ البيع وطباعة الفاتورة") }
        }
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
