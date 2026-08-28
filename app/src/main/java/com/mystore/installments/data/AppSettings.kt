package com.mystore.installments.data

import android.content.Context
import java.security.MessageDigest

/**
 * إعدادات عامة للتطبيق تُحفظ محلياً (SharedPreferences):
 * - اسم المحل وشعاره (يظهران أعلى كل فاتورة/وصل مطبوع).
 * - رمز صلاحية (PIN) يُطلب عند تعديل السعر يدوياً أو إضافة خصم وقت البيع،
 *   بحيث لا يقدر أي موظف بيع على تخفيض الأسعار دون إذن صاحب المحل.
 */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // ---------- بيانات المحل ----------
    var storeName: String
        get() = prefs.getString(KEY_STORE_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_STORE_NAME, value).apply()

    /** مسار (Uri) صورة شعار المحل، إن وُجدت */
    var storeLogoUri: String?
        get() = prefs.getString(KEY_STORE_LOGO, null)
        set(value) = prefs.edit().putString(KEY_STORE_LOGO, value).apply()

    fun clearLogo() { prefs.edit().remove(KEY_STORE_LOGO).apply() }

    // ---------- صلاحية التعديل اليدوي على السعر / الخصم ----------
    val hasPin: Boolean get() = prefs.contains(KEY_PIN_HASH)

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()
    }

    fun clearPin() { prefs.edit().remove(KEY_PIN_HASH).apply() }

    fun verifyPin(pin: String): Boolean {
        val saved = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return saved == hash(pin)
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_STORE_NAME = "store_name"
        private const val KEY_STORE_LOGO = "store_logo_uri"
        private const val KEY_PIN_HASH = "manager_pin_hash"
    }
}
