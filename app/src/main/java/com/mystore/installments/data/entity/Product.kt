package com.mystore.installments.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// بيانات المنتج (السلعة) في المخزون
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val category: String = "",
    val costPrice: Double = 0.0,       // سعر الشراء (التكلفة)
    val cashPrice: Double = 0.0,       // سعر البيع نقداً
    val installmentPrice: Double = 0.0, // سعر البيع بالتقسيط
    val imageUri: String? = null,       // مسار صورة المنتج (اختياري)
    val stockQuantity: Int = 0,         // الكمية المتوفرة بالمخزون
    val createdAt: Long = System.currentTimeMillis()
)
