package id.remping.hondascantool

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread

class BluetoothService(private val onLine: (String) -> Unit, private val onError: (String)->Unit) {
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var outStream: OutputStream? = null
    private var running = false

    private val TAG = "BTService"
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun isAvailable(): Boolean = adapter != null && adapter!!.isEnabled

    fun connectTo(deviceName: String) {
        val dev = adapter?.bondedDevices?.firstOrNull { it.name == deviceName }
        if (dev == null) {
            onError("Device $deviceName tidak terpasang/pairing")
            return
        }
        thread {
            try {
                val device: BluetoothDevice = dev
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                adapter?.cancelDiscovery()
                socket?.connect()
                outStream = socket?.outputStream
                val reader = BufferedReader(InputStreamReader(socket!!.inputStream))
                running = true
                onLine("CONNECTED")
                while (running) {
                    val line = reader.readLine() ?: break
                    onLine(line)
                }
            } catch (e: Exception) {
                onError("Gagal connect: ${e.message}")
                Log.e(TAG, "error", e)
            } finally {
                close()
            }
        }
    }

    fun send(bytes: ByteArray) {
        try {
            outStream?.write(bytes)
        } catch (e: Exception) {
            onError("Send error: ${e.message}")
        }
    }

    fun close() {
        running = false
        try { socket?.close() } catch (_: Exception) {}
    }
}
