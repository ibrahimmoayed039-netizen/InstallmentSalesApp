package com.mystore.installments.printer

import com.mystore.installments.util.formatAmount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * يبني عقد/إقرار بيع بالتقسيط كصفحة HTML بحجم A4 قابلة للطباعة عبر إطار الطباعة القياسي
 * في أندرويد (WebView + PrintManager)، بدل الاعتماد على الطابعة الحرارية الضيقة.
 * نستخدم WebView/Chromium لأنه يهتم بترتيب وتشكيل الحروف العربية بشكل صحيح تلقائياً (RTL كامل).
 *
 * ملاحظة: نص "الشروط والأحكام" هنا عام وتوضيحي فقط وليس صياغة قانونية معتمدة؛
 * يُفضَّل أن يراجعه صاحب المحل أو محامٍ قبل الاعتماد عليه رسمياً.
 */
object ContractHtmlBuilder {

    private fun formatDate(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale("ar")).format(Date(millis))

    fun build(contract: ContractData): String {
        val itemsRows = contract.items.joinToString("") { item ->
            """
            <tr>
                <td>${escape(item.name)}</td>
                <td>${item.quantity}</td>
                <td>${formatAmount(item.lineTotal)}</td>
            </tr>
            """.trimIndent()
        }

        val installmentsRows = contract.installments.joinToString("") { inst ->
            """
            <tr>
                <td>${inst.number}</td>
                <td>${formatDate(inst.dueDateMillis)}</td>
                <td>${formatAmount(inst.amount)}</td>
                <td class="sig-cell"></td>
            </tr>
            """.trimIndent()
        }

        val remaining = contract.totalAmount - contract.downPayment

        return """
        <!DOCTYPE html>
        <html dir="rtl" lang="ar">
        <head>
        <meta charset="UTF-8">
        <style>
            @page { size: A4; margin: 16mm 14mm; }
            body { font-family: 'Arial', sans-serif; direction: rtl; color: #1B1B1B; font-size: 13px; line-height: 1.6; }
            h1 { text-align: center; font-size: 19px; margin: 0 0 2px; color: #1F3B57; }
            .subtitle { text-align: center; font-size: 12px; color: #555; margin-bottom: 16px; }
            .store-name { text-align: center; font-size: 15px; font-weight: bold; margin-bottom: 2px; }
            .store-meta { text-align: center; font-size: 11px; color: #555; margin-bottom: 14px; }
            .parties { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 14px; }
            .party-box { flex: 1; border: 1px solid #BDB6A8; border-radius: 6px; padding: 8px 10px; }
            .party-title { font-weight: bold; color: #1F3B57; margin-bottom: 4px; font-size: 12px; }
            .party-row { font-size: 12px; margin: 2px 0; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 14px; }
            th, td { border: 1px solid #BDB6A8; padding: 6px 8px; text-align: center; font-size: 12px; }
            th { background: #EDE7DD; color: #1B1B1B; }
            .sig-cell { width: 70px; }
            .section-title { font-weight: bold; color: #1F3B57; margin: 14px 0 6px; font-size: 13px; }
            .summary { display: flex; justify-content: flex-end; margin-bottom: 6px; }
            .summary table { width: 260px; }
            .summary td { text-align: right; }
            .summary td:first-child { text-align: right; color: #555; }
            .summary tr.total td { font-weight: bold; font-size: 13px; }
            .terms { font-size: 11.5px; color: #333; }
            .terms ol { padding-right: 18px; margin: 4px 0; }
            .terms li { margin-bottom: 5px; }
            .terms-note { font-size: 10.5px; color: #888; margin-top: 6px; }
            .signatures { display: flex; justify-content: space-between; margin-top: 40px; }
            .sign-box { width: 45%; text-align: center; }
            .sign-line { border-top: 1px solid #444; margin-top: 46px; padding-top: 4px; font-size: 12px; }
        </style>
        </head>
        <body>
            <div class="store-name">${escape(contract.storeName)}</div>
            <div class="store-meta">${escape(contract.storeAddress)}${if (contract.storeAddress.isNotBlank() && contract.storePhone.isNotBlank()) " • " else ""}${escape(contract.storePhone)}</div>

            <h1>عقد وإقرار بيع بالتقسيط</h1>
            <div class="subtitle">رقم الفاتورة: ${contract.saleId} — التاريخ: ${formatDate(contract.saleDateMillis)}</div>

            <div class="parties">
                <div class="party-box">
                    <div class="party-title">الطرف الأول (البائع)</div>
                    <div class="party-row">${escape(contract.storeName)}</div>
                    <div class="party-row">${escape(contract.storeAddress)}</div>
                    <div class="party-row">${escape(contract.storePhone)}</div>
                </div>
                <div class="party-box">
                    <div class="party-title">الطرف الثاني (المشتري)</div>
                    <div class="party-row">الاسم: ${escape(contract.customerName)}</div>
                    <div class="party-row">الهاتف: ${escape(contract.customerPhone)}</div>
                    <div class="party-row">العنوان: ${escape(contract.customerAddress.ifBlank { "-" })}</div>
                </div>
            </div>

            <div class="section-title">الأصناف المباعة</div>
            <table>
                <tr><th>الصنف</th><th>الكمية</th><th>الإجمالي</th></tr>
                $itemsRows
            </table>

            <div class="summary">
                <table>
                    <tr><td>إجمالي الأصناف</td><td>${formatAmount(contract.grossTotal)}</td></tr>
                    ${if (contract.discount > 0) "<tr><td>الخصم</td><td>${formatAmount(contract.discount)}</td></tr>" else ""}
                    <tr><td>الدفعة المقدمة</td><td>${formatAmount(contract.downPayment)}</td></tr>
                    <tr class="total"><td>المتبقي بالتقسيط</td><td>${formatAmount(remaining)}</td></tr>
                </table>
            </div>

            <div class="section-title">جدول الأقساط</div>
            <table>
                <tr><th>رقم القسط</th><th>تاريخ الاستحقاق</th><th>المبلغ</th><th>توقيع عند السداد</th></tr>
                $installmentsRows
            </table>

            <div class="section-title">إقرار وشروط عامة</div>
            <div class="terms">
                <ol>
                    <li>يقر الطرف الثاني (المشتري) بأنه استلم الأصناف المذكورة أعلاه بحالة جيدة، وبأنه اطّلع على السعر الإجمالي وجدول الأقساط الموضّح أعلاه ووافق عليه.</li>
                    <li>يلتزم المشتري بسداد كل قسط في تاريخ استحقاقه المحدد أعلاه دون تأخير.</li>
                    <li>في حال تأخر المشتري عن سداد أي قسط، يحق للبائع المطالبة بالمبلغ المتبقي بالكامل و/أو اتخاذ الإجراءات القانونية المتاحة لاسترداد حقه.</li>
                    <li>تبقى ملكية الأصناف المباعة للبائع حتى سداد كامل قيمة الفاتورة، ما لم يُتفق خطياً على خلاف ذلك.</li>
                    <li>هذا العقد نسخة واحدة موقعة من الطرفين، ولكل طرف الحق بالحصول على صورة منها.</li>
                </ol>
                <div class="terms-note">ملاحظة: هذا نص عام توضيحي وليس صياغة قانونية معتمدة؛ يُنصح بمراجعته من محامٍ قبل الاعتماد عليه رسمياً.</div>
            </div>

            <div class="signatures">
                <div class="sign-box">
                    <div class="sign-line">توقيع البائع</div>
                </div>
                <div class="sign-box">
                    <div class="sign-line">توقيع المشتري</div>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
