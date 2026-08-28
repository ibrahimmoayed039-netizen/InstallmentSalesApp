package com.mystore.installments.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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

    val totalAmount = cartItems.sumOf { it.lineTotal }

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
                        value = itemPrice, onValueChange = { itemPrice = it },
                        label = { Text("سعر البيع بالتقسيط") }, modifier = Modifier.weight(1f)
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
            Text("الإجمالي: %.0f".format(totalAmount), style = MaterialTheme.typography.titleMedium)

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
                            notes = ""
                        ) { saleId ->
                            val installmentAmount = (totalAmount - down) / count
                            val receipt = ReceiptData(
                                storeName = "متجرنا",
                                title = "فاتورة بيع بالتقسيط رقم $saleId",
                                customerName = customer.name,
                                customerPhone = customer.phone,
                                itemsSummary = cartItems.map { "${it.name} × ${it.quantity} = %.0f".format(it.lineTotal) },
                                lines = listOf(
                                    ReceiptLine("الإجمالي", "%.0f".format(totalAmount)),
                                    ReceiptLine("الدفعة المقدمة", "%.0f".format(down)),
                                    ReceiptLine("عدد الأقساط", count.toString()),
                                    ReceiptLine("قيمة القسط الشهري", "%.0f".format(installmentAmount), bold = true)
                                )
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
}
