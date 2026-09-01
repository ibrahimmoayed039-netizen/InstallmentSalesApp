package com.mystore.installments.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mystore.installments.data.AppDatabase
import com.mystore.installments.data.AppSettings
import com.mystore.installments.data.entity.*
import com.mystore.installments.printer.PrinterManager
import com.mystore.installments.printer.ReceiptData
import com.mystore.installments.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = StoreRepository(
        db.customerDao(), db.saleDao(), db.installmentDao(), db.paymentDao(), db.productDao()
    )
    val printerManager = PrinterManager(application)
    val appSettings = AppSettings(application)

    val customers: StateFlow<List<Customer>> =
        repository.getCustomers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> =
        repository.getProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<Sale>> =
        repository.getSales().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdueInstallments: StateFlow<List<Installment>> =
        repository.getOverdueInstallments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unpaidInstallments: StateFlow<List<Installment>> =
        repository.getUnpaidInstallments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // نفس الأقساط غير المسددة لكن مربوطة باسم وهاتف العميل، لعرض متابعة الأقساط مجمّعة على العميل
    val unpaidInstallmentsWithCustomer: StateFlow<List<InstallmentWithCustomer>> =
        repository.getUnpaidInstallmentsWithCustomer().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCustomer(name: String, phone: String, address: String, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.addCustomer(Customer(name = name, phone = phone, address = address))
            onDone(id)
        }
    }

    fun updateCustomer(customer: Customer, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            onDone()
        }
    }

    fun deleteCustomer(customer: Customer, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            onDone()
        }
    }

    // ---------- المنتجات ----------
    fun addProduct(
        name: String,
        barcode: String,
        category: String,
        costPrice: Double,
        cashPrice: Double,
        installmentPrice: Double,
        imageUri: String? = null,
        stockQuantity: Int = 0,
        onDone: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repository.addProduct(
                Product(
                    name = name,
                    barcode = barcode,
                    category = category,
                    costPrice = costPrice,
                    cashPrice = cashPrice,
                    installmentPrice = installmentPrice,
                    imageUri = imageUri,
                    stockQuantity = stockQuantity
                )
            )
            onDone(id)
        }
    }

    fun updateProduct(product: Product, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateProduct(product)
            onDone()
        }
    }

    fun deleteProduct(product: Product, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            onDone()
        }
    }

    fun createSale(
        customerId: Long,
        items: List<SaleItem>,
        totalAmount: Double,
        downPayment: Double,
        numberOfInstallments: Int,
        notes: String,
        discount: Double = 0.0,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.createSale(customerId, items, totalAmount, downPayment, numberOfInstallments, notes, discount)
            onDone(id)
        }
    }

    fun payInstallment(installmentId: Long, amount: Double, note: String = "", onDone: (Payment?) -> Unit = {}) {
        viewModelScope.launch {
            val payment = repository.payInstallment(installmentId, amount, note)
            onDone(payment)
        }
    }

    // ---------- معاينة وطباعة الوصل ----------
    // يُحفظ الوصل المُراد معاينته/طباعته هنا لتفادي تمرير كائن معقّد عبر التنقل (Navigation)
    private val _pendingReceipt = MutableStateFlow<ReceiptData?>(null)
    val pendingReceipt: StateFlow<ReceiptData?> = _pendingReceipt

    fun setPendingReceipt(receipt: ReceiptData, contract: com.mystore.installments.printer.ContractData? = null) {
        _pendingReceipt.value = receipt
        // نصفّر عقد التقسيط المعلّق افتراضياً حتى لا يظهر زر "طباعة العقد" خطأً على وصل غير
        // مرتبط ببيع جديد (مثل طباعة فاتورة سابقة أو كشف حساب)، إلا إذا مُرِّر عقد صراحةً هنا
        _pendingContract.value = contract
    }

    // ---------- معاينة/طباعة عقد التقسيط (PDF قابل للتوقيع) ----------
    private val _pendingContract = MutableStateFlow<com.mystore.installments.printer.ContractData?>(null)
    val pendingContract: StateFlow<com.mystore.installments.printer.ContractData?> = _pendingContract

    suspend fun printPendingReceipt(): Boolean {
        val receipt = _pendingReceipt.value ?: return false
        return printerManager.print(receipt)
    }
}
