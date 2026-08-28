package com.mystore.installments.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// حالة عملية البيع بالتقسيط
enum class SaleStatus { ACTIVE, COMPLETED, CANCELLED }

// عملية بيع بالتقسيط (فاتورة رئيسية)
@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val date: Long = System.currentTimeMillis(),
    val totalAmount: Double,       // إجمالي قيمة البيع
    val downPayment: Double,       // الدفعة المقدمة
    val numberOfInstallments: Int, // عدد الأقساط
    val installmentAmount: Double, // قيمة القسط الواحد
    val notes: String = "",
    val status: SaleStatus = SaleStatus.ACTIVE
)
