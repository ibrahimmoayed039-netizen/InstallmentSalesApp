package com.mystore.installments.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// عنصر ضمن فاتورة البيع (سلعة أو خدمة)
@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long? = null, // ربط بالمنتج في المخزون (اختياري لدعم أصناف يدوية قديمة)
    val name: String,
    val quantity: Int,
    val price: Double
) {
    val lineTotal: Double get() = quantity * price
}
