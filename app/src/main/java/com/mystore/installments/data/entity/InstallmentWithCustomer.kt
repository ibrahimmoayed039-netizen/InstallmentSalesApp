package com.mystore.installments.data.entity

import androidx.room.Embedded

// نتيجة استعلام مُجمَّع (JOIN) بين الأقساط والمبيعات والعملاء،
// تُستخدم لعرض متابعة الأقساط مجمّعة على اسم العميل بدل رقم الفاتورة فقط.
data class InstallmentWithCustomer(
    @Embedded val installment: Installment,
    val customerId: Long,
    val customerName: String,
    val customerPhone: String
)
