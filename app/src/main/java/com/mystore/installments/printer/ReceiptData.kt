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
    val storeAddress: String = "",      // عنوان المحل (اختياري)، يظهر أعلى الفاتورة إن وُجد
    val storePhone: String = "",        // هاتف المحل (اختياري)، يظهر أعلى الفاتورة إن وُجد
    val date: Long = System.currentTimeMillis(),
    val lines: List<ReceiptLine>,       // تفاصيل (المبلغ، القسط رقم، المتبقي...)
    val itemsSummary: List<ReceiptItemLine> = emptyList(), // أسطر عناصر البيع إن وجدت
    val footerNote: String = "شكراً لتعاملكم معنا"
) {
    fun formattedDate(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")).format(Date(date))
}

/** سطر معاينة واحد: bold للخط العريض، centered ليُعرض في وسط الفاتورة بدل التبرير الكامل على عرض السطر */
data class ReceiptPreviewLine(val text: String, val bold: Boolean = false, val centered: Boolean = false)

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
        // ---------- ترويسة الفاتورة: اسم المحل بارز، ثم عنوانه وهاتفه إن وُجدا ----------
        b.doubleHeight(true)
            .bold(true)
            .text(data.storeName)
            .doubleHeight(false)
            .bold(false)
        if (data.storeAddress.isNotBlank()) b.text(data.storeAddress)
        if (data.storePhone.isNotBlank()) b.text("هاتف: ${data.storePhone}")

        // إطار مزدوج (خط ثقيل) حول عنوان الفاتورة نفسها (رقمها/نوعها) لإبرازها كترويسة رسمية مستقلة
        b.divider('=')
        b.bold(true).text(data.title).bold(false)
        b.divider('=')

        // بيانات العميل والتاريخ: أسطر قصيرة في وسط الفاتورة (وليست ممتدة على كامل عرض السطر)
        // لأن قيمها عادة قصيرة، والتوسيط هنا أنسب بصرياً من التبرير على طرفي السطر
        b.alignCenter()
            .text("العميل: ${data.customerName}")
            .text("الهاتف: ${data.customerPhone}")
            .text("التاريخ: ${data.formattedDate()}")
            .divider()

        b.alignRight()
        if (data.itemsSummary.isNotEmpty()) {
            // صف عناوين لقائمة الأصناف/الأقساط، يعطي شكل "جدول" واضح بدل قائمة نصوص متفرقة
            b.bold(true).itemLine("البيان", "المبلغ").bold(false)
            b.divider()
            data.itemsSummary.forEach { b.itemLine(it.text, it.amount) }
            b.divider('=')
        }

        data.lines.forEach {
            b.bold(it.bold)
            b.keyValue(it.label, it.value)
        }
        b.bold(false)
        b.divider('=')
        b.alignCenter()
        b.text(data.footerNote)
        b.feed(3)
        b.cut()
        return b.build()
    }

    /** يبني نص المعاينة (سطراً بسطر) بنفس منطق الطباعة الفعلية تماماً، مع علامة السطور العريضة */
    fun buildPreviewLines(data: ReceiptData, charsPerLine: Int = 48): List<ReceiptPreviewLine> {
        val heavyDivider = "=".repeat(charsPerLine)
        val lightDivider = ".".repeat(charsPerLine)
        val lines = mutableListOf<ReceiptPreviewLine>()

        lines.add(ReceiptPreviewLine(data.storeName, bold = true, centered = true))
        if (data.storeAddress.isNotBlank()) lines.add(ReceiptPreviewLine(data.storeAddress, centered = true))
        if (data.storePhone.isNotBlank()) lines.add(ReceiptPreviewLine("هاتف: ${data.storePhone}", centered = true))

        lines.add(ReceiptPreviewLine(heavyDivider))
        lines.add(ReceiptPreviewLine(data.title, bold = true, centered = true))
        lines.add(ReceiptPreviewLine(heavyDivider))

        // بيانات العميل والتاريخ: أسطر قصيرة في وسط الفاتورة (وليست ممتدة على كامل عرض السطر)
        lines.add(ReceiptPreviewLine("العميل: ${data.customerName}", centered = true))
        lines.add(ReceiptPreviewLine("الهاتف: ${data.customerPhone}", centered = true))
        lines.add(ReceiptPreviewLine("التاريخ: ${data.formattedDate()}", centered = true))
        lines.add(ReceiptPreviewLine(lightDivider))

        if (data.itemsSummary.isNotEmpty()) {
            lines.add(ReceiptPreviewLine(EscPosBuilder.formatItemLine("البيان", "المبلغ", charsPerLine), bold = true))
            lines.add(ReceiptPreviewLine(lightDivider))
            data.itemsSummary.forEach {
                lines.add(ReceiptPreviewLine(EscPosBuilder.formatItemLine(it.text, it.amount, charsPerLine)))
            }
            lines.add(ReceiptPreviewLine(heavyDivider))
        }

        data.lines.forEach {
            lines.add(ReceiptPreviewLine(EscPosBuilder.formatKeyValue(it.label, it.value, charsPerLine), bold = it.bold))
        }
        lines.add(ReceiptPreviewLine(heavyDivider))
        lines.add(ReceiptPreviewLine(data.footerNote, centered = true))
        return lines
    }
}
