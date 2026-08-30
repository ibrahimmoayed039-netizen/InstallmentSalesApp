package com.mystore.installments.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mystore.installments.data.entity.Product
import com.mystore.installments.viewmodel.AppViewModel

/**
 * شاشة إضافة منتج جديد أو تعديل منتج موجود.
 * إن كان productId غير null يتم تحميل بيانات المنتج للتعديل، وإلا فهي شاشة إضافة منتج جديد.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(viewModel: AppViewModel, navController: NavController, productId: Long?) {
    val context = LocalContext.current
    var existingProduct by remember { mutableStateOf<Product?>(null) }

    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var cashPrice by remember { mutableStateOf("") }
    var installmentPrice by remember { mutableStateOf("") }
    var stockQuantity by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // منتقي الصور: يمنح صلاحية دائمة (Persistable) للاحتفاظ بمسار الصورة بعد إغلاق التطبيق
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // بعض المصادر لا تدعم الصلاحية الدائمة؛ ستظل الصورة صالحة للعرض في هذه الجلسة
            }
            imageUri = uri
        }
    }

    // تحميل بيانات المنتج عند فتح الشاشة للتعديل
    LaunchedEffect(productId) {
        if (productId != null && productId != 0L) {
            val product = viewModel.repository.getProduct(productId)
            if (product != null) {
                existingProduct = product
                name = product.name
                barcode = product.barcode
                category = product.category
                costPrice = if (product.costPrice == 0.0) "" else product.costPrice.toString()
                cashPrice = if (product.cashPrice == 0.0) "" else product.cashPrice.toString()
                installmentPrice = if (product.installmentPrice == 0.0) "" else product.installmentPrice.toString()
                stockQuantity = if (product.stockQuantity == 0) "" else product.stockQuantity.toString()
                imageUri = product.imageUri?.let { Uri.parse(it) }
            }
        }
    }

    val isEditing = existingProduct != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "تعديل منتج" else "منتج جديد") },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            existingProduct?.let { p ->
                                viewModel.deleteProduct(p) { navController.popBackStack() }
                            }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف المنتج")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------- صورة المنتج (اختياري) ----------
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePicker.launch(arrayOf("image/*")) },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "صورة المنتج",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "إزالة الصورة",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = "إضافة صورة")
                        Spacer(Modifier.height(4.dp))
                        Text("إضافة صورة", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("اسم المنتج") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = barcode, onValueChange = { barcode = it },
                label = { Text("الباركود / رمز الصنف") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = category, onValueChange = { category = it },
                label = { Text("الفئة / التصنيف") }, modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text("الأسعار", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = costPrice, onValueChange = { costPrice = it },
                label = { Text("سعر الشراء (التكلفة)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = cashPrice, onValueChange = { cashPrice = it },
                label = { Text("سعر البيع نقداً") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = installmentPrice, onValueChange = { installmentPrice = it },
                label = { Text("سعر البيع بالتقسيط") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text("المخزون", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = stockQuantity, onValueChange = { stockQuantity = it.filter { c -> c.isDigit() } },
                label = { Text("الكمية المتوفرة") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val cost = costPrice.toDoubleOrNull() ?: 0.0
                        val cash = cashPrice.toDoubleOrNull() ?: 0.0
                        val installment = installmentPrice.toDoubleOrNull() ?: 0.0
                        val stock = stockQuantity.toIntOrNull() ?: 0
                        val imageUriString = imageUri?.toString()

                        val current = existingProduct
                        if (current != null) {
                            viewModel.updateProduct(
                                current.copy(
                                    name = name,
                                    barcode = barcode,
                                    category = category,
                                    costPrice = cost,
                                    cashPrice = cash,
                                    installmentPrice = installment,
                                    imageUri = imageUriString,
                                    stockQuantity = stock
                                )
                            ) { navController.popBackStack() }
                        } else {
                            viewModel.addProduct(
                                name = name,
                                barcode = barcode,
                                category = category,
                                costPrice = cost,
                                cashPrice = cash,
                                installmentPrice = installment,
                                imageUri = imageUriString,
                                stockQuantity = stock
                            ) { navController.popBackStack() }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "حفظ التعديلات" else "حفظ المنتج")
            }
        }
    }
}
