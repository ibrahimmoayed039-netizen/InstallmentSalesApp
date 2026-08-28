package com.mystore.installments.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// بيانات العميل
@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
