package com.mystore.installments.printer

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

    /** خط فاصل بعرض السطر الكامل */
    fun divider(char: Char = '-'): EscPosBuilder {
        text(char.toString().repeat(charsPerLine))
        return this
    }

    /** سطر بعنصرين: نص على اليمين وقيمة على اليسار (مناسب للواجهة العربية RTL) */
    fun keyValue(label: String, value: String): EscPosBuilder {
        val space = (charsPerLine - label.length - value.length).coerceAtLeast(1)
        text(value + " ".repeat(space) + label)
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
}
