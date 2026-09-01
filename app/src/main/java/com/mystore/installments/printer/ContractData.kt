package com.mystore.installments.printer

/** بند واحد من عناصر الفاتورة المعروضة داخل جدول العقد */
data class ContractItemLine(
    val name: String,
    val quantity: Int,
    val lineTotal: Double
)

/** قسط واحد ضمن جدول السداد المعروض داخل العقد */
data class ContractInstallmentLine(
    val number: Int,
    val dueDateMillis: Long,
    val amount: Double
)

/**
 * كل البيانات اللازمة لبناء عقد/إقرار بيع بالتقسيط PDF جاهز للتوقيع.
 * يُبنى مرة واحدة عند إنشاء البيع، ويُخزَّن مؤقتاً بالـ ViewModel ريثما يفتح المستخدم شاشة المعاينة.
 */
data class ContractData(
    val saleId: Long,
    val saleDateMillis: Long,
    val storeName: String,
    val storeAddress: String,
    val storePhone: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val items: List<ContractItemLine>,
    val grossTotal: Double,
    val discount: Double,
    val totalAmount: Double,
    val downPayment: Double,
    val installments: List<ContractInstallmentLine>
)
