package com.mystore.installments.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mystore.installments.data.entity.Customer
import com.mystore.installments.viewmodel.AppViewModel

/**
 * شاشة إضافة عميل جديد أو تعديل بيانات عميل موجود (الاسم، الهاتف، العنوان).
 * إن كان customerId غير null يتم تحميل بيانات العميل للتعديل، وإلا فهي شاشة إضافة عميل جديد.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(viewModel: AppViewModel, navController: NavController, customerId: Long? = null) {
    var existingCustomer by remember { mutableStateOf<Customer?>(null) }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    // تحميل بيانات العميل عند فتح الشاشة للتعديل
    LaunchedEffect(customerId) {
        if (customerId != null) {
            val customer = viewModel.repository.getCustomer(customerId)
            if (customer != null) {
                existingCustomer = customer
                name = customer.name
                phone = customer.phone
                address = customer.address
            }
        }
    }

    val isEditing = existingCustomer != null
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "تعديل بيانات العميل" else "عميل جديد") },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف العميل")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isEditing) {
                // تذكير بسيط بأن التعديل هنا يغيّر اسم/بيانات العميل في كل الفواتير والأقساط
                // السابقة والقادمة أيضاً، لأنها كلها مرتبطة بنفس سجل العميل
                ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "أي تعديل هنا ينعكس على كل فواتير وأقساط هذا العميل تلقائياً",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; if (it.isNotBlank()) nameError = false },
                label = { Text("اسم العميل") },
                isError = nameError,
                supportingText = { if (nameError) Text("اسم العميل مطلوب") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("رقم الهاتف") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = address, onValueChange = { address = it },
                label = { Text("العنوان (اختياري)") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val current = existingCustomer
                    if (current != null) {
                        viewModel.updateCustomer(
                            current.copy(name = name.trim(), phone = phone.trim(), address = address.trim())
                        ) { navController.popBackStack() }
                    } else {
                        viewModel.addCustomer(name.trim(), phone.trim(), address.trim()) {
                            navController.popBackStack()
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isEditing) "حفظ التعديلات" else "حفظ العميل", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف العميل") },
            text = { Text("سيتم حذف بيانات العميل \"${existingCustomer?.name}\" نهائياً. هذا الإجراء لا يمكن التراجع عنه.") },
            confirmButton = {
                TextButton(onClick = {
                    existingCustomer?.let { c ->
                        viewModel.deleteCustomer(c) { navController.popBackStack() }
                    }
                    showDeleteConfirm = false
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("إلغاء") }
            }
        )
    }
}
