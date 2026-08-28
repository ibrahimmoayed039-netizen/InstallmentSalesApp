package com.mystore.installments.printer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * إدارة الاتصال بالطابعة الحرارية عبر منفذ USB (يتطلب كابل OTG على أغلب الهواتف).
 * يبحث عن أول واجهة USB تدعم نقل البيانات بالجملة (Bulk Transfer) وهو ما تعتمده أغلب طابعات ESC/POS.
 */
class UsbPrinterManager(private val context: Context) {

    private val usbManager: UsbManager by lazy { context.getSystemService(Context.USB_SERVICE) as UsbManager }
    private var connection: UsbDeviceConnection? = null
    private var endpointOut: UsbEndpoint? = null
    private var usbInterface: UsbInterface? = null

    companion object {
        const val ACTION_USB_PERMISSION = "com.mystore.installments.USB_PERMISSION"
    }

    fun connectedPrinters(): List<UsbDevice> = usbManager.deviceList.values.toList()

    fun hasPermission(device: UsbDevice): Boolean = usbManager.hasPermission(device)

    fun requestPermission(device: UsbDevice) {
        val flags = PendingIntent.FLAG_MUTABLE
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION), flags
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    suspend fun connect(device: UsbDevice): Boolean = withContext(Dispatchers.IO) {
        // البحث عن أول واجهة بها Endpoint من نوع BULK OUT
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            for (e in 0 until iface.endpointCount) {
                val endpoint = iface.getEndpoint(e)
                if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    endpoint.direction == UsbConstants.USB_DIR_OUT
                ) {
                    val conn = usbManager.openDevice(device) ?: return@withContext false
                    conn.claimInterface(iface, true)
                    connection = conn
                    endpointOut = endpoint
                    usbInterface = iface
                    return@withContext true
                }
            }
        }
        false
    }

    suspend fun printBytes(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val conn = connection ?: return@withContext false
        val endpoint = endpointOut ?: return@withContext false
        val result = conn.bulkTransfer(endpoint, data, data.size, 5000)
        result >= 0
    }

    fun disconnect() {
        usbInterface?.let { connection?.releaseInterface(it) }
        connection?.close()
        connection = null
        endpointOut = null
        usbInterface = null
    }

    fun isConnected(): Boolean = connection != null
}
