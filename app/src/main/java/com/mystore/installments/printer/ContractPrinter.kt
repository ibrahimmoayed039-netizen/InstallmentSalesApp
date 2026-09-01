package com.mystore.installments.printer

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * يطبع عقد التقسيط كملف PDF حقيقي عبر إطار الطباعة القياسي في أندرويد (وليس عبر الطابعة
 * الحرارية الضيقة المستخدمة لبقية الوصولات). يعتمد على WebView لأنه يشكّل النص العربي RTL
 * بشكل صحيح تلقائياً، ثم يسلّم محتواه لـ PrintManager الذي يفتح نافذة النظام القياسية
 * (اختيار طابعة فعلية أو "حفظ كـ PDF" لمشاركته أو طباعته لاحقاً).
 */
object ContractPrinter {

    // نُبقي مرجعاً حياً لآخر WebView قيد الطباعة، لأن مهمة الطباعة غير المتزامنة قد تُلغى
    // إذا تم جمع الكائن (garbage collected) قبل انتهاء تحميل الصفحة وبدء مهمة الطباعة الفعلية
    private var activeWebView: WebView? = null

    fun printContract(context: Context, contract: ContractData, jobNamePrefix: String = "عقد_تقسيط") {
        val html = ContractHtmlBuilder.build(contract)
        val webView = WebView(context)
        activeWebView = webView
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null) {
                    val jobName = "${jobNamePrefix}_${contract.saleId}"
                    val adapter = view.createPrintDocumentAdapter(jobName)
                    printManager.print(jobName, adapter, PrintAttributes.Builder().build())
                }
                activeWebView = null
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
