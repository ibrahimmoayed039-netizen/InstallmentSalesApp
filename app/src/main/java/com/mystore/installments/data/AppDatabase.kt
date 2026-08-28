package com.mystore.installments.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mystore.installments.data.dao.CustomerDao
import com.mystore.installments.data.dao.InstallmentDao
import com.mystore.installments.data.dao.PaymentDao
import com.mystore.installments.data.dao.ProductDao
import com.mystore.installments.data.dao.SaleDao
import com.mystore.installments.data.entity.Customer
import com.mystore.installments.data.entity.Installment
import com.mystore.installments.data.entity.Payment
import com.mystore.installments.data.entity.Product
import com.mystore.installments.data.entity.Sale
import com.mystore.installments.data.entity.SaleItem

// قاعدة بيانات محلية (SQLite عبر Room) تبقى محفوظة حتى بعد إغلاق التطبيق
@Database(
    entities = [Customer::class, Sale::class, SaleItem::class, Installment::class, Payment::class, Product::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun paymentDao(): PaymentDao
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "installment_sales.db"
                )
                    // ملاحظة: أثناء التطوير لا توجد خطوات Migration مكتوبة لهذا الإصدار الجديد،
                    // لذا سيتم إعادة إنشاء القاعدة عند تغيّر البنية بدل تعطّل التطبيق.
                    // عند الإصدار للمستخدمين يُستحسن استبدال هذا بخطوات Migration حقيقية للحفاظ على بياناتهم.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
