package com.mystore.installments.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InstallmentStatus { PENDING, PAID, LATE }

// قسط مجدوَل ضمن خطة الدفع
@Entity(tableName = "installments")
data class Installment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val installmentNumber: Int,   // ترتيب القسط ضمن الخطة
    val dueDate: Long,            // تاريخ الاستحقاق
    val amount: Double,           // المبلغ المستحق لهذا القسط
    val paidAmount: Double = 0.0, // المبلغ المسدد فعلياً (يدعم السداد الجزئي)
    val paidDate: Long? = null,
    val status: InstallmentStatus = InstallmentStatus.PENDING
)
