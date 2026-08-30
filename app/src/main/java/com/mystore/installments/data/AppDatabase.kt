package com.mystore.installments.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 6,
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
        const val DB_NAME = "installment_sales.db"

        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * خطوة ترقية حقيقية (وليست إعادة إنشاء مدمِّرة) من الإصدار 4 إلى 5: تضيف فقط
         * عمود الخصم (discount) الجديد إلى جدول المبيعات، وتحافظ على كل بيانات المستخدم.
         *
         * مهم جداً: أي تعديل مستقبلي على بنية القاعدة (إضافة/حذف عمود أو جدول) يجب أن يُرافقه
         * كائن Migration جديد هنا (وزيادة رقم version بالأعلى)، وإلا سيرفض Room فتح القاعدة.
         * لا نستخدم fallbackToDestructiveMigration() لأنه يمسح بيانات كل المستخدمين
         * (العملاء، الفواتير، الأقساط...) عند أي تحديث مستقبلي للتطبيق لم تُكتب له Migration.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sales ADD COLUMN discount REAL NOT NULL DEFAULT 0")
            }
        }

        /** ترقية 5 إلى 6: تضيف عمود كمية المخزون للمنتجات (بدون فقدان أي بيانات موجودة) */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN stockQuantity INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = build(context)
                INSTANCE = instance
                instance
            }
        }

        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                .build()
        }

        /**
         * تُستخدم فقط عند استعادة نسخة احتياطية: تُغلق الاتصال الحالي بالقاعدة وتُفرغ الكائن
         * المحفوظ (Singleton) كي يُعاد فتح ملف القاعدة (المُستبدَل حديثاً) من جديد بدل استخدام
         * اتصال قديم يشير لملف لم يعد موجوداً.
         */
        fun closeAndReset() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
