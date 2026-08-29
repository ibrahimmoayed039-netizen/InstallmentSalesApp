package com.mystore.installments.printer

import android.graphics.Bitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// نموذج بيانات موحّد للوصل، يُستخدم في المعاينة وفي بناء أوامر الطباعة معاً
// بحيث تكون المعاينة مطابقة تماماً لما سيُطبع فعلياً
data class ReceiptLine(val label: String, val value: String, val bold: Boolean = false)

/**
 * سطر ضمن قائمة الأصناف/الأقساط. إن كان amount غير فارغ، يُطبع الاسم مع محاذاة المبلغ
 * لعمود ثابت من اليمين (لتصطف كل الأرقام تحت بعضها)، وإلا يُطبع النص وحده على كامل السطر
 * (يُستخدم مثلاً لعناوين فرعية مثل رقم فاتورة ضمن كشف حساب).
 */
data class ReceiptItemLine(val text: String, val amount: String? = null)

data class ReceiptData(
    val storeName: String,
    val title: String,                 // مثال: "وصل استلام دفعة" أو "فاتورة بيع بالتقسيط"
    val customerName: String,
    val customerPhone: String,
    val date: Long = System.currentTimeMillis(),
    val lines: List<ReceiptLine>,       // تفاصيل (المبلغ، القسط رقم، المتبقي...)
    val itemsSummary: List<ReceiptItemLine> = emptyList(), // أسطر عناصر البيع إن وجدت
    val footerNote: String = "شكراً لتعاملكم معنا"
) {
    fun formattedDate(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")).format(Date(date))
}

// يحوّل بيانات الوصل إلى أوامر ESC/POS جاهزة للإرسال للطابعة
object ReceiptBuilder {
    fun build(
        data: ReceiptData,
        charsPerLine: Int = 48,
        codeTable: Int? = null,
        logo: Bitmap? = null,
        logoWidthDots: Int = 384
    ): ByteArray {
        val b = EscPosBuilder(charsPerLine).init()
        if (codeTable != null) b.selectCharacterTable(codeTable)
        b.alignCenter()
        if (logo != null) {
            b.image(logo, logoWidthDots)
            b.feed(1)
        }
        b.doubleHeight(true)
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
            data.itemsSummary.forEach { b.itemLine(it.text, it.amount) }
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
        val divider = ".".repeat(charsPerLine)
        val lines = mutableListOf<String>()
        lines.add(data.storeName)
        lines.add(data.title)
        lines.add(divider)
        lines.add(EscPosBuilder.formatKeyValue("العميل", data.customerName, charsPerLine))
        lines.add(EscPosBuilder.formatKeyValue("الهاتف", data.customerPhone, charsPerLine))
        lines.add(EscPosBuilder.formatKeyValue("التاريخ", data.formattedDate(), charsPerLine))
        lines.add(divider)
        data.itemsSummary.forEach { lines.add(EscPosBuilder.formatItemLine(it.text, it.amount, charsPerLine)) }
        if (data.itemsSummary.isNotEmpty()) lines.add(divider)
        data.lines.forEach { lines.add(EscPosBuilder.formatKeyValue(it.label, it.value, charsPerLine)) }
        lines.add(divider)
        lines.add(data.footerNote)
        return lines
    }
}
