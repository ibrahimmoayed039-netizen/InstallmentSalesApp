package com.mystore.installments.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mystore.installments.data.AppSettings

/**
 * نافذة تطلب رمز الصلاحية (PIN) قبل السماح بإجراء حسّاس (تعديل سعر يدوي، إضافة خصم...).
 * تُستخدم في أكثر من شاشة، لذا وُضعت هنا كمكوّن مشترك.
 */
@Composable
fun PinDialog(
    title: String = "أدخل رمز الصلاحية",
    appSettings: AppSettings,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("هذا الإجراء يتطلب رمز صلاحية المدير قبل التنفيذ.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it; error = false },
                    label = { Text("رمز الصلاحية") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error,
                    singleLine = true
                )
                if (error) {
                    Spacer(Modifier.height(4.dp))
                    Text("رمز غير صحيح", color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (appSettings.verifyPin(pin)) {
                    onSuccess()
                    onDismiss()
                } else {
                    error = true
                }
            }) { Text("تأكيد") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
