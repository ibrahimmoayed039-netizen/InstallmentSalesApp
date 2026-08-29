package com.mystore.installments.printer

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * أوامر ESC/POS الأساسية لبناء محتوى الطباعة للطابعات الحرارية
 * تدعم عرض 58مم و80مم (الفرق الرئيسي هو عدد الأحرف بالسطر).
 */
class EscPosBuilder(private val charsPerLine: Int = 48) { // 48 حرفاً لعرض 80مم بخط عادي، استخدم 32 لعرض 58مم

    private val buffer = ByteArrayOutputStream()
    private val charset: Charset = Charset.forName("CP864") // ترميز يدعم العربية على أغلب الطابعات الحرارية، مع بديل UTF-8 عند الحاجة

    fun init(): EscPosBuilder {
        buffer.write(byteArrayOf(0x1B, 0x40)) // ESC @  إعادة تهيئة الطابعة
        return this
    }

    fun alignCenter(): EscPosBuilder { buffer.write(byteArrayOf(0x1B, 0x61, 0x01)); return this }
    fun alignLeft(): EscPosBuilder { buffer.write(byteArrayOf(0x1B, 0x61, 0x00)); return this }
    fun alignRight(): EscPosBuilder { buffer.write(byteArrayOf(0x1B, 0x61, 0x02)); return this }

    fun bold(on: Boolean): EscPosBuilder { buffer.write(byteArrayOf(0x1B, 0x45, if (on) 0x01 else 0x00)); return this }

    /**
     * ESC t n — يختار جدول الحروف (Code Page) المستخدم داخل الطابعة نفسها.
     * كل طابعة تُرقّم جداولها بشكل مختلف، لذا نفس البايتات المُرسلة (CP864) قد تظهر
     * كعربي صحيح تحت رقم جدول معيّن، وكرموز غير مفهومة تحت رقم آخر.
     */
    fun selectCharacterTable(n: Int): EscPosBuilder {
        buffer.write(byteArrayOf(0x1B, 0x74, n.toByte()))
        return this
    }

    fun doubleHeight(on: Boolean): EscPosBuilder {
        buffer.write(byteArrayOf(0x1D, 0x21, if (on) 0x11 else 0x00))
        return this
    }

    fun text(line: String): EscPosBuilder {
        val bytes = try {
            line.toByteArray(charset)
        } catch (e: Exception) {
            line.toByteArray(Charsets.UTF_8)
        }
        buffer.write(bytes)
        buffer.write('\n'.code)
        return this
    }

    /** خط فاصل منقط بعرض السطر الكامل (بدل الشرطات المتصلة، أقرب لشكل الفواتير الحرارية الفعلية) */
    fun divider(char: Char = '.'): EscPosBuilder {
        text(char.toString().repeat(charsPerLine))
        return this
    }

    /**
     * سطر بعنصرين: نص على اليمين وقيمة على اليسار (مناسب للواجهة العربية RTL).
     * القيمة تُحاذى داخل عمود ثابت العرض (valueWidth) من جهة اليمين، بحيث تتراصف كل
     * الأرقام (آحاد تحت آحاد، عشرات تحت عشرات...) عبر كل أسطر الفاتورة بدل أن تبدأ
     * كل قيمة من نفس نقطة البداية فقط دون محاذاة فعلية للأرقام نفسها.
     */
    fun keyValue(label: String, value: String, valueWidth: Int = 12): EscPosBuilder {
        text(formatKeyValue(label, value, charsPerLine, valueWidth))
        return this
    }

    /**
     * سطر عنصر مع مبلغ: اسم الصنف (يُقصّ إن طال) يليه المبلغ محاذى لعمود ثابت من اليمين،
     * بنفس فكرة keyValue، مخصص لقوائم الأصناف/الأقساط الطويلة كي تصطف أرقامها بانتظام
     * تحت بعضها بغض النظر عن اختلاف طول أسماء الأصناف.
     */
    fun itemLine(name: String, amount: String?, amountWidth: Int = 12): EscPosBuilder {
        text(formatItemLine(name, amount, charsPerLine, amountWidth))
        return this
    }

    /**
     * يطبع صورة (شعار المحل) كصورة نقطية (Raster) عبر أمر GS v 0، وهو الأمر المدعوم على
     * أغلب الطابعات الحرارية الرخيصة لطباعة الصور خلافاً للنص. تُحوَّل الصورة أولاً إلى
     * أبيض/أسود فقط (Threshold) لأن الطابعات الحرارية لا تدعم درجات الرمادي.
     * @param maxWidthDots عرض الورق بالنقاط (384 لعرض 58مم تقريباً، 576 لعرض 80مم بدقة 203dpi الشائعة)
     */
    fun image(bitmap: Bitmap, maxWidthDots: Int): EscPosBuilder {
        // عرض الصورة يجب أن يكون من مضاعفات 8 (كل بايت يمثل 8 نقاط أفقياً)
        val targetWidth = (maxWidthDots / 8) * 8
        val scale = targetWidth.toFloat() / bitmap.width
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)

        val widthBytes = targetWidth / 8
        val raster = ByteArray(widthBytes * targetHeight)

        for (y in 0 until targetHeight) {
            for (xByte in 0 until widthBytes) {
                var b = 0
                for (bit in 0 until 8) {
                    val x = xByte * 8 + bit
                    val pixel = scaled.getPixel(x, y)
                    // نحسب درجة الإضاءة ونعتبر أي بكسل داكن نقطة "حبر" تُطبع (1)
                    val luminance = (Color.red(pixel) * 0.3 + Color.green(pixel) * 0.59 + Color.blue(pixel) * 0.11)
                    val isDark = Color.alpha(pixel) > 32 && luminance < 160
                    if (isDark) b = b or (0x80 shr bit)
                }
                raster[y * widthBytes + xByte] = b.toByte()
            }
        }
        scaled.recycle()

        // GS v 0: m=0 (وضع عادي)، ثم عرض الصورة بالبايت (منخفض/عالي) وارتفاعها بالبايت (منخفض/عالي)
        buffer.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
        buffer.write(widthBytes and 0xFF)
        buffer.write((widthBytes shr 8) and 0xFF)
        buffer.write(targetHeight and 0xFF)
        buffer.write((targetHeight shr 8) and 0xFF)
        buffer.write(raster)
        return this
    }

    fun feed(lines: Int = 1): EscPosBuilder {
        repeat(lines) { buffer.write('\n'.code) }
        return this
    }

    fun cut(): EscPosBuilder {
        buffer.write(byteArrayOf(0x1D, 0x56, 0x00)) // GS V 0  قص كامل للورق
        return this
    }

    fun build(): ByteArray = buffer.toByteArray()

    companion object {
        /**
         * منطق محاذاة "قيمة/مبلغ" مشترك بين الطباعة الفعلية وشاشة المعاينة، حتى تكون
         * المعاينة مطابقة تماماً لما سيخرج من الطابعة (بما فيه محاذاة الأرقام).
         */
        fun formatKeyValue(label: String, value: String, charsPerLine: Int, valueWidth: Int = 12): String {
            val paddedValue = value.take(valueWidth).padStart(valueWidth)
            val labelSpace = (charsPerLine - valueWidth).coerceAtLeast(label.length)
            return paddedValue + label.padStart(labelSpace)
        }

        fun formatItemLine(name: String, amount: String?, charsPerLine: Int, amountWidth: Int = 12): String {
            if (amount == null) return name
            val nameWidth = (charsPerLine - amountWidth - 1).coerceAtLeast(4)
            val trimmedName = if (name.length > nameWidth) name.take(nameWidth - 1) + "…" else name
            return trimmedName.padEnd(nameWidth) + " " + amount.take(amountWidth).padStart(amountWidth)
        }
    }
}

/**
 * يبني ورقة اختبار تطبع نفس الجملة العربية تحت كل جدول حروف (Code Page) على حدة،
 * بدءاً من CP0 وحتى CP47، ثم CP255 (القيمة الخاصة المستخدمة في كثير من طابعات Epson/OEM
 * الرخيصة لجدول العربية WPC1256/Arabic). يقارن المستخدم الأسطر ليجد الرقم الذي تظهر تحته
 * الجملة بشكل عربي صحيح، ثم يستخدم هذا الرقم بشكل دائم عبر الإعدادات.
 */
/**
 * أداة مساعدة لتحويل نص أوامر ESC/POS مكتوب بصيغة hex (مثال: "1B 40 1B 61 01")
 * إلى بايتات جاهزة للإرسال مباشرة للطابعة. تُستخدم في شاشة الإعدادات للسماح
 * للمستخدم بتجربة أو تغيير أوامر الطباعة الخام يدوياً دون تعديل الكود.
 */
object RawCommandParser {
    /** يُرجع null إذا كان النص فارغاً أو يحتوي على قيمة hex غير صالحة */
    fun parse(input: String): ByteArray? {
        val tokens = input.trim().split(Regex("[\\s,]+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        return try {
            ByteArray(tokens.size) { i -> tokens[i].removePrefix("0x").removePrefix("0X").toInt(16).toByte() }
        } catch (e: NumberFormatException) {
            null
        }
    }
}

object CodePageTestBuilder {
    private val sampleText = "اختبار الحروف العربية ١٢٣"

    /** كل أرقام جداول الحروف الشائعة التي يجب تجربتها: 0 إلى 47، ثم 255 */
    val testedTables: List<Int> = (0..47) + 255

    fun build(charsPerLine: Int = 32): ByteArray {
        val builder = EscPosBuilder(charsPerLine).init().alignLeft()
        testedTables.forEach { n ->
            builder.selectCharacterTable(n)
            builder.text("CP$n: $sampleText")
        }
        // إعادة الطابعة لجدول الحروف الافتراضي (0) بعد الاختبار
        builder.selectCharacterTable(0)
        builder.feed(3)
        builder.cut()
        return builder.build()
    }
}
