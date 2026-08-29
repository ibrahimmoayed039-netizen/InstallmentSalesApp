package com.mystore.installments.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// أرقام غربية دائماً بفاصلة آلاف انجليزية (1,500) بغض النظر عن لغة الجهاز، حتى لا تتحول
// الأرقام إلى الترقيم الهندي العربي (١٬٥٠٠) على الأجهزة المضبوطة على العربية، لتبقى مقروءة
// بوضوح على الفاتورة المطبوعة وفي الشاشات
private val amountFormat = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))

/** يهيئ مبلغاً مالياً بفواصل الآلاف (مثال: 12500 → "12,500") بدل عرضه كسلسلة أرقام متلاصقة */
fun formatAmount(value: Double): String = amountFormat.format(value)
