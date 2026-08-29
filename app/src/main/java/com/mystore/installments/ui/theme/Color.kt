package com.mystore.installments.ui.theme

import androidx.compose.ui.graphics.Color

// هوية لونية مخصصة (كحلي داكن + ذهبي دافئ) بدل الأزرق العام الافتراضي لتطبيقات الأعمال،
// لتعطي طابعاً مميزاً يناسب محل تجزئة/بيع بالتقسيط بدل شكل "تطبيق إداري" عام

// ---------- الوضع الفاتح ----------
val PrimaryLight = Color(0xFF1F3B57)          // كحلي داكن: الثقة والاستقرار المالي
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFD3E4F5)
val OnPrimaryContainerLight = Color(0xFF0C2338)

val SecondaryLight = Color(0xFFB8860B)        // ذهبي دافئ: لمسة "محل" مميزة للتفاصيل والتأكيد
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFF6E4B8)
val OnSecondaryContainerLight = Color(0xFF4A3A00)

val BackgroundLight = Color(0xFFF8F6F2)       // أبيض دافئ بدل الرمادي البارد المعتاد
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEDE7DD)
val OnSurfaceLight = Color(0xFF1B1B1B)
val OnSurfaceVariantLight = Color(0xFF4A4A45)
val OutlineLight = Color(0xFFBDB6A8)

// ---------- الوضع الليلي ----------
val PrimaryDark = Color(0xFF8FB8DE)
val OnPrimaryDark = Color(0xFF0C2338)
val PrimaryContainerDark = Color(0xFF2C4A66)
val OnPrimaryContainerDark = Color(0xFFD3E4F5)

val SecondaryDark = Color(0xFFE0BB5C)
val OnSecondaryDark = Color(0xFF3E2F00)
val SecondaryContainerDark = Color(0xFF5A4600)
val OnSecondaryContainerDark = Color(0xFFF6E4B8)

val BackgroundDark = Color(0xFF15171A)
val SurfaceDark = Color(0xFF1E2124)
val SurfaceVariantDark = Color(0xFF303339)
val OnSurfaceDark = Color(0xFFECEAE5)
val OnSurfaceVariantDark = Color(0xFFC7C4BC)
val OutlineDark = Color(0xFF8C8A83)

// ---------- ألوان دلالية (لا تتغيّر بين الفاتح والداكن، تدل دائماً على نفس الحالة) ----------
val AccentGreen = Color(0xFF2E7D32)   // مسدد/مكتمل
val AccentRed = Color(0xFFC62828)     // متأخر
val AccentOrange = Color(0xFFEF6C00)  // تنبيه/قريب الاستحقاق
