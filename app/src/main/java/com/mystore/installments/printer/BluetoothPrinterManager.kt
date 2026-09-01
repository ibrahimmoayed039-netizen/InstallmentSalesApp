package com.mystore.installments.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * إدارة الاتصال بالطابعة الحرارية عبر البلوتوث الكلاسيكي (SPP).
 * أغلب طابعات 58/80مم الرخيصة تستخدم بروفايل SPP مع UUID القياسي التالي.
 *
 * ملاحظة مهمة: على أندرويد 12 (API 31) وما فوق، أي استدعاء لدوال البلوتوث
 * (bondedDevices، createRfcommSocketToServiceRecord، startDiscovery...) بدون
 * منح صلاحية BLUETOOTH_CONNECT/BLUETOOTH_SCAN وقت التشغيل فعلياً يرمي
 * SecurityException ويُغلق التطبيق فوراً. لذلك كل دالة هنا محمية بـ try/catch
 * كخط دفاع أخير، لكن الأصل هو طلب الصلاحية من الشاشة قبل استدعاء هذه الدوال.
 */
class BluetoothPrinterManager(private val context: Context) {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var discoveryReceiver: BroadcastReceiver? = null

    @SuppressLint("MissingPermission")
    fun pairedPrinters(): List<BluetoothDevice> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            // الصلاحية غير ممنوحة بعد؛ الشاشة مسؤولة عن طلبها قبل إعادة المحاولة
            emptyList()
        }
    }

    /**
     * يبدأ البحث عن أجهزة بلوتوث قريبة غير مقترنة بعد (لعرض زر "بحث عن طابعة").
     * يستدعي onDeviceFound لكل جهاز جديد يُكتشف، و onFinished عند انتهاء المسح.
     * يجب إيقافه عبر stopDiscovery عند مغادرة الشاشة لتفادي تسريب الـ Receiver.
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery(onDeviceFound: (BluetoothDevice) -> Unit, onFinished: () -> Unit) {
        try {
            stopDiscovery()
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    when (intent.action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            val device: BluetoothDevice? =
                                if (Build.VERSION.SDK_INT >= 33)
                                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                                else
                                    @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            device?.let(onDeviceFound)
                        }
                        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> onFinished()
                    }
                }
            }
            discoveryReceiver = receiver
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            adapter.cancelDiscovery()
            val started = adapter.startDiscovery()
            if (!started) {
                // فشل بدء المسح فعلياً (بلوتوث مطفأ / throttling) فلن يصل ACTION_DISCOVERY_FINISHED أبداً
                stopDiscovery()
                onFinished()
            }
        } catch (e: SecurityException) {
            onFinished()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try { BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery() } catch (e: SecurityException) {}
        discoveryReceiver?.let {
            try { context.unregisterReceiver(it) } catch (e: IllegalArgumentException) {}
        }
        discoveryReceiver = null
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
            val sock = device.createRfcommSocketToServiceRecord(sppUuid)
            sock.connect()
            socket = sock
            true
        } catch (e: IOException) {
            disconnect()
            false
        } catch (e: SecurityException) {
            disconnect()
            false
        }
    }

    suspend fun printBytes(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val stream = socket?.outputStream ?: return@withContext false
            stream.write(data)
            stream.flush()
            true
        } catch (e: IOException) {
            false
        }
    }

    fun disconnect() {
        try { socket?.close() } catch (_: IOException) {}
        socket = null
    }

    fun isConnected(): Boolean = socket?.isConnected == true
}
