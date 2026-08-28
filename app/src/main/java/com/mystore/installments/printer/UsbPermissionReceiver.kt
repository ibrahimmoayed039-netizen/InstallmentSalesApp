package com.mystore.installments.printer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// مستقبل نتيجة طلب صلاحية الوصول لجهاز USB (الطابعة)
class UsbPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // تُدار النتيجة عملياً داخل الشاشة عبر مراقبة registerReceiver المحلي في SettingsScreen
        // تُرك هذا الـ Receiver في المانيفست لدعم بعض إصدارات أندرويد التي تتطلب مستقبلاً مُعلناً
    }
}
