package com.mystore.installments.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import com.mystore.installments.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * نسخ احتياطي واستعادة لقاعدة بيانات التطبيق (ملف SQLite واحد).
 * مهم بشكل خاص لأن التطبيق كان يعتمد على fallbackToDestructiveMigration، فهذه الأداة
 * تعطي صاحب المحل طريقة يحمي فيها بياناته بنفسه (نسخة يحفظها في أي مكان يختاره:
 * تخزين الهاتف، Google Drive، بطاقة ذاكرة...) بغض النظر عن أي تحديثات مستقبلية للتطبيق.
 */
object BackupManager {

    /** ينسخ ملف قاعدة البيانات الحالي إلى الوجهة التي اختارها المستخدم (عبر منتقي حفظ الملفات في أندرويد) */
    suspend fun backupTo(context: Context, destination: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // نُفرغ سجل WAL أولاً (checkpoint) بحيث يحتوي ملف .db وحده على كل البيانات المكتوبة حديثاً
            checkpoint(context)

            val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
            if (!dbFile.exists()) return@withContext false

            context.contentResolver.openOutputStream(destination)?.use { out ->
                dbFile.inputStream().use { input -> input.copyTo(out) }
            } ?: return@withContext false

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * يستبدل ملف قاعدة البيانات الحالي بمحتوى الملف الذي اختاره المستخدم.
     * يُغلق الاتصال الحالي بالقاعدة أولاً حتى لا يفشل النسخ بسبب انشغال الملف.
     * بعد النجاح يجب إعادة تشغيل التطبيق ليُقرأ الملف الجديد من البداية (انظر restartApp).
     */
    suspend fun restoreFrom(context: Context, source: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            AppDatabase.closeAndReset()

            val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
            dbFile.parentFile?.mkdirs()

            context.contentResolver.openInputStream(source)?.use { input ->
                dbFile.outputStream().use { out -> input.copyTo(out) }
            } ?: return@withContext false

            // حذف ملفات WAL/SHM القديمة إن وُجدت حتى لا تتعارض مع الملف الجديد المستعاد
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()

            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkpoint(context: Context) {
        try {
            val db = AppDatabase.getInstance(context)
            db.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))
        } catch (_: Exception) {
            // إن فشل الـ checkpoint نتابع النسخ عادياً؛ Room يفتح القاعدة بوضع WAL افتراضياً
            // وقد يُفقد آخر تغييرات غير مكتوبة بعد للقرص في أسوأ الحالات، لكن هذا نادر جداً
        }
    }

    /** يعيد تشغيل التطبيق بالكامل من الصفر، مطلوب بعد الاستعادة كي تُقرأ القاعدة الجديدة */
    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
