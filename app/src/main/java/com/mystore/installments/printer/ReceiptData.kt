package com.mystore.installments.printer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// نموذج بيانات موحّد للوصل، يُستخدم في المعاينة وفي بناء أوامر الطباعة معاً
// بحيث تكون المعاينة مطابقة تماماً لما سيُطبع فعلياً
data class ReceiptLine(val label: String, val value: String, val bold: Boolean = false)

data class ReceiptData(
    val storeName: String,
    val title: String,                 // مثال: "وصل استلام دفعة" أو "فاتورة بيع بالتقسيط"
    val customerName: String,
    val customerPhone: String,
    val date: Long = System.currentTimeMillis(),
    val lines: List<ReceiptLine>,       // تفاصيل (المبلغ، القسط رقم، المتبقي...)
    val itemsSummary: List<String> = emptyList(), // أسطر عناصر البيع إن وجدت
    val footerNote: String = "شكراً لتعاملكم معنا"
) {
    fun formattedDate(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")).format(Date(date))
}

// يحوّل بيانات الوصل إلى أوامر ESC/POS جاهزة للإرسال للطابعة
object ReceiptBuilder {
    fun build(data: ReceiptData, charsPerLine: Int = 48): ByteArray {
        val b = EscPosBuilder(charsPerLine).init()
            .alignCenter()
            .doubleHeight(true)
            .bold(true)
            .text(data.storeName)
            .doubleHeight(false)
            .bold(false)
            .text(data.title)
            .divider()
            .alignRight()
            .keyValue("العميل", data.customerName)
            .keyValue("الهاتف", data.customerPhone)
            .keyValue("التاريخ", data.formattedDate())
            .divider()

        if (data.itemsSummary.isNotEmpty()) {
            data.itemsSummary.forEach { b.text(it) }
            b.divider()
        }

        data.lines.forEach {
            b.bold(it.bold)
            b.keyValue(it.label, it.value)
        }
        b.bold(false)
        b.divider()
        b.alignCenter()
        b.text(data.footerNote)
        b.feed(3)
        b.cut()
        return b.build()
    }

    /** يبني نص المعاينة (سطر بسطر) بنفس منطق الطباعة الفعلية، لعرضه قبل الإرسال للطابعة */
    fun buildPreviewLines(data: ReceiptData, charsPerLine: Int = 48): List<String> {
        val lines = mutableListOf<String>()
        lines.add(data.storeName)
        lines.add(data.title)
        lines.add("-".repeat(charsPerLine))
        lines.add("العميل: ${data.customerName}")
        lines.add("الهاتف: ${data.customerPhone}")
        lines.add("التاريخ: ${data.formattedDate()}")
        lines.add("-".repeat(charsPerLine))
        lines.addAll(data.itemsSummary)
        if (data.itemsSummary.isNotEmpty()) lines.add("-".repeat(charsPerLine))
        data.lines.forEach { lines.add("${it.label}: ${it.value}") }
        lines.add("-".repeat(charsPerLine))
        lines.add(data.footerNote)
        return lines
    }
}
