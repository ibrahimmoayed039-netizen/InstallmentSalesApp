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

    private val _connectionType = MutableStateFlow(PrinterConnectionType.NONE)
    val connectionType: StateFlow<PrinterConnectionType> = _connectionType

    var paperWidth: PaperWidth = PaperWidth.MM80

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

    /** يطبع الوصل فعلياً عبر نوع الاتصال الحالي (بلوتوث أو USB) */
    suspend fun print(data: ReceiptData): Boolean {
        val bytes = ReceiptBuilder.build(data, paperWidth.charsPerLine)
        return when (_connectionType.value) {
            PrinterConnectionType.BLUETOOTH -> bluetoothManager.printBytes(bytes)
            PrinterConnectionType.USB -> usbManager.printBytes(bytes)
            PrinterConnectionType.NONE -> false
        }
    }

    fun isConnected(): Boolean = _connectionType.value != PrinterConnectionType.NONE
}
