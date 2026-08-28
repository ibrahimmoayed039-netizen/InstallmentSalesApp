package com.mystore.installments.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * إدارة الاتصال بالطابعة الحرارية عبر البلوتوث الكلاسيكي (SPP).
 * أغلب طابعات 58/80مم الرخيصة تستخدم بروفايل SPP مع UUID القياسي التالي.
 */
class BluetoothPrinterManager(private val context: Context) {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    fun pairedPrinters(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices?.toList() ?: emptyList()
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
