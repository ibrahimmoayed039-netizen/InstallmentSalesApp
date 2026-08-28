package com.mystore.installments.repository

import com.mystore.installments.data.dao.CustomerDao
import com.mystore.installments.data.dao.InstallmentDao
import com.mystore.installments.data.dao.PaymentDao
import com.mystore.installments.data.dao.ProductDao
import com.mystore.installments.data.dao.SaleDao
import com.mystore.installments.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

// طبقة الوسيطة بين الواجهات وقاعدة البيانات، وتحتوي على منطق الأعمال الأساسي
class StoreRepository(
    private val customerDao: CustomerDao,
    private val saleDao: SaleDao,
    private val installmentDao: InstallmentDao,
    private val paymentDao: PaymentDao,
    private val productDao: ProductDao
) {
    // ---------- العملاء ----------
    fun getCustomers(): Flow<List<Customer>> = customerDao.getAll()
    fun searchCustomers(query: String): Flow<List<Customer>> = customerDao.search(query)
    suspend fun getCustomer(id: Long): Customer? = customerDao.getById(id)
    suspend fun addCustomer(customer: Customer): Long = customerDao.insert(customer)
    suspend fun updateCustomer(customer: Customer) = customerDao.update(customer)
    suspend fun deleteCustomer(customer: Customer) = customerDao.delete(customer)

    // ---------- المنتجات ----------
    fun getProducts(): Flow<List<Product>> = productDao.getAll()
    fun searchProducts(query: String): Flow<List<Product>> = productDao.search(query)
    suspend fun getProduct(id: Long): Product? = productDao.getById(id)
    suspend fun getProductByBarcode(barcode: String): Product? = productDao.getByBarcode(barcode)
    suspend fun addProduct(product: Product): Long = productDao.insert(product)
    suspend fun updateProduct(product: Product) = productDao.update(product)
    suspend fun deleteProduct(product: Product) = productDao.delete(product)

    // ---------- المبيعات ----------
    fun getSales(): Flow<List<Sale>> = saleDao.getAll()
    fun getSalesForCustomer(customerId: Long): Flow<List<Sale>> = saleDao.getByCustomer(customerId)
    suspend fun getSale(id: Long): Sale? = saleDao.getById(id)
    suspend fun getSaleItems(saleId: Long): List<SaleItem> = saleDao.getItemsForSale(saleId)
    fun getInstallmentsForSale(saleId: Long): Flow<List<Installment>> = installmentDao.getForSale(saleId)

    fun getAllInstallments(): Flow<List<Installment>> = installmentDao.getAll()
    fun getOverdueInstallments(): Flow<List<Installment>> = installmentDao.getOverdue(System.currentTimeMillis())
    fun getUnpaidInstallments(): Flow<List<Installment>> = installmentDao.getUnpaid()
    fun getUnpaidInstallmentsWithCustomer(): Flow<List<InstallmentWithCustomer>> = installmentDao.getUnpaidWithCustomer()

    /**
     * إنشاء عملية بيع بالتقسيط جديدة كاملة:
     * يحفظ الفاتورة، عناصرها، ثم يولّد جدول الأقساط تلقائياً بفاصل شهري.
     */
    suspend fun createSale(
        customerId: Long,
        items: List<SaleItem>,
        totalAmount: Double,
        downPayment: Double,
        numberOfInstallments: Int,
        notes: String = "",
        discount: Double = 0.0
    ): Long {
        val remaining = totalAmount - downPayment
        val installmentAmount = if (numberOfInstallments > 0) remaining / numberOfInstallments else 0.0

        val sale = Sale(
            customerId = customerId,
            totalAmount = totalAmount,
            downPayment = downPayment,
            numberOfInstallments = numberOfInstallments,
            installmentAmount = installmentAmount,
            notes = notes,
            discount = discount
        )
        val saleId = saleDao.insert(sale)

        if (items.isNotEmpty()) {
            saleDao.insertItems(items.map { it.copy(saleId = saleId) })
        }

        // توليد جدول الأقساط الشهري تلقائياً
        val schedule = mutableListOf<Installment>()
        val calendar = Calendar.getInstance()
        for (i in 1..numberOfInstallments) {
            calendar.add(Calendar.MONTH, if (i == 1) 1 else 1)
            schedule.add(
                Installment(
                    saleId = saleId,
                    installmentNumber = i,
                    dueDate = calendar.timeInMillis,
                    amount = installmentAmount
                )
            )
        }
        if (schedule.isNotEmpty()) installmentDao.insertAll(schedule)

        return saleId
    }

    /**
     * تسجيل عملية سداد لقسط معيّن (يدعم السداد الجزئي أو الكامل).
     */
    suspend fun payInstallment(installmentId: Long, amountPaid: Double, note: String = ""): Payment? {
        val installment = installmentDao.getById(installmentId) ?: return null
        val newPaidAmount = installment.paidAmount + amountPaid
        val newStatus = if (newPaidAmount >= installment.amount) InstallmentStatus.PAID else InstallmentStatus.PENDING

        installmentDao.update(
            installment.copy(
                paidAmount = newPaidAmount,
                paidDate = if (newStatus == InstallmentStatus.PAID) System.currentTimeMillis() else installment.paidDate,
                status = newStatus
            )
        )

        val payment = Payment(
            saleId = installment.saleId,
            installmentId = installmentId,
            amount = amountPaid,
            note = note
        )
        val id = paymentDao.insert(payment)
        return payment.copy(id = id)
    }

    fun getRecentPayments(limit: Int = 30): Flow<List<Payment>> = paymentDao.getRecent(limit)
    fun getPaymentsForSale(saleId: Long): Flow<List<Payment>> = paymentDao.getForSale(saleId)

    /**
     * يجمع كل عمليات بيع العميل مع جداول أقساطها، لبناء كشف حساب كامل (فاتورة واحدة تلخّص
     * كل تعاملات العميل بدل طباعة كل فاتورة على حدة).
     */
    suspend fun getCustomerStatementItems(customerId: Long): List<Pair<Sale, List<Installment>>> {
        // نستخدم أول قيمة فقط من التدفق (Flow) لأن هذه قراءة لحظية لغرض الطباعة، وليست عرضاً حياً
        val salesList = saleDao.getByCustomer(customerId).first()
        return salesList.map { sale -> sale to installmentDao.getForSaleOnce(sale.id) }
    }

    /** تحديث حالة الأقساط المتأخرة (يُستدعى دورياً عند فتح الشاشة الرئيسية) */
    suspend fun refreshOverdueStatuses() {
        val now = System.currentTimeMillis()
        // ملاحظة: التنفيذ الكامل عبر استعلام يحدّث الحالة مباشرة في قاعدة البيانات
        // تم تبسيطه هنا ليُنفَّذ عبر الشاشات عند القراءة (status = LATE يُحتسب بالمقارنة مع dueDate و paidAmount)
    }
}
