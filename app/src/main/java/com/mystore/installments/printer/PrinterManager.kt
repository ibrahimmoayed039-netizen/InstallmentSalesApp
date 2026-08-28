package com.mystore.installments.printer

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.hardware.usb.UsbDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class PrinterConnectionType { BLUETOOTH, USB, NONE }
enum class PaperWidth(val charsPerLine: Int) { MM58(32), MM80(48) }

/**
 * واجهة موحّدة فوق مديري البلوتوث و USB، بحيث تتعامل الشاشات مع نقطة واحدة
 * بغض النظر عن نوع الاتصال الفعلي بالطابعة الحرارية.
 */
class PrinterManager(context: Context) {
    private val bluetoothManager = BluetoothPrinterManager(context)
    private val usbManager = UsbPrinterManager(context)

    private val prefs = context.getSharedPreferences("printer_settings", Context.MODE_PRIVATE)

    private val _connectionType = MutableStateFlow(PrinterConnectionType.NONE)
    val connectionType: StateFlow<PrinterConnectionType> = _connectionType

    var paperWidth: PaperWidth = PaperWidth.MM80

    /** رقم جدول الحروف (Code Page) الذي أثبت أنه يطبع العربية بشكل صحيح على هذه الطابعة، إن وُجد */
    private val _codeTable = MutableStateFlow(prefs.getInt(KEY_CODE_TABLE, -1).takeIf { it >= 0 })
    val codeTable: StateFlow<Int?> = _codeTable

    fun setCodeTable(n: Int?) {
        _codeTable.value = n
        prefs.edit().apply {
            if (n == null) remove(KEY_CODE_TABLE) else putInt(KEY_CODE_TABLE, n)
        }.apply()
    }

    fun pairedBluetoothDevices(): List<BluetoothDevice> = bluetoothManager.pairedPrinters()
    fun connectedUsbDevices(): List<UsbDevice> = usbManager.connectedPrinters()
    fun hasUsbPermission(device: UsbDevice) = usbManager.hasPermission(device)
    fun requestUsbPermission(device: UsbDevice) = usbManager.requestPermission(device)

    suspend fun connectBluetooth(device: BluetoothDevice): Boolean {
        val ok = bluetoothManager.connect(device)
        if (ok) _connectionType.value = PrinterConnectionType.BLUETOOTH
        return ok
    }

    suspend fun connectUsb(device: UsbDevice): Boolean {
        val ok = usbManager.connect(device)
        if (ok) _connectionType.value = PrinterConnectionType.USB
        return ok
    }

    fun disconnect() {
        bluetoothManager.disconnect()
        usbManager.disconnect()
        _connectionType.value = PrinterConnectionType.NONE
    }

    /** إرسال بايتات جاهزة مباشرة عبر نوع الاتصال الحالي (مستخدم في اختبار جداول الحروف) */
    suspend fun printRawBytes(bytes: ByteArray): Boolean {
        return when (_connectionType.value) {
            PrinterConnectionType.BLUETOOTH -> bluetoothManager.printBytes(bytes)
            PrinterConnectionType.USB -> usbManager.printBytes(bytes)
            PrinterConnectionType.NONE -> false
        }
    }

    /** يطبع ورقة اختبار تعرض نفس الجملة العربية أسفل كل رقم جدول حروف من CP0 إلى CP47 ثم CP255 */
    suspend fun printCharacterTableTest(): Boolean {
        val bytes = CodePageTestBuilder.build(paperWidth.charsPerLine)
        return printRawBytes(bytes)
    }

    /** يطبع الوصل فعلياً عبر نوع الاتصال الحالي (بلوتوث أو USB) */
    suspend fun print(data: ReceiptData): Boolean {
        val bytes = ReceiptBuilder.build(data, paperWidth.charsPerLine, _codeTable.value)
        return printRawBytes(bytes)
    }

    fun isConnected(): Boolean = _connectionType.value != PrinterConnectionType.NONE

    companion object {
        private const val KEY_CODE_TABLE = "code_table"
    }
}
