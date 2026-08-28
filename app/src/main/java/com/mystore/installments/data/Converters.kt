package com.mystore.installments.data

import androidx.room.TypeConverter
import com.mystore.installments.data.entity.InstallmentStatus
import com.mystore.installments.data.entity.SaleStatus

// محولات Room لتخزين أنواع Enum كنص في SQLite
class Converters {
    @TypeConverter
    fun fromSaleStatus(status: SaleStatus): String = status.name

    @TypeConverter
    fun toSaleStatus(value: String): SaleStatus = SaleStatus.valueOf(value)

    @TypeConverter
    fun fromInstallmentStatus(status: InstallmentStatus): String = status.name

    @TypeConverter
    fun toInstallmentStatus(value: String): InstallmentStatus = InstallmentStatus.valueOf(value)
}
