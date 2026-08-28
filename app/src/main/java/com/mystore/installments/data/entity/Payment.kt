package com.mystore.installments.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// سجل كل عملية تسديد (لطباعة وصل وأرشفة تاريخ الدفعات)
@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val installmentId: Long,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)
