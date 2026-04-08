package com.example.local_inter.core

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*

class NearbyConnect(private val context: Context) {
    companion object {
        private const val TAG = "NearbyConnect"
        private const val SERVICE_NAME = "LocalInter"
        private const val SERVICE_UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB"
        private val SERVICE_UUID = UUID.fromString(SERVICE_UUID_STRING)
    }
    
    private val bluetooth = BluetoothAdapter.getDefaultAdapter()
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var isConnected = false
    
    var onNearbyDevice: ((BluetoothDevice) -> Unit)? = null
    var onConnectionStateChanged: ((Boolean, String) -> Unit)? = null
    var onDataReceived: ((ByteArray) -> Unit)? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (it.bondState != BluetoothDevice.BOND_BONDED) {
                            onNearbyDevice?.invoke(it)
                            Log.d(TAG, "发现设备: ${it.name ?: "未知"} (${it.address})")
                        }
                    }
                }
                
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "蓝牙扫描完成")
                }
            }
        }
    }

    fun startScan() {
        if (!checkBluetoothPermissions()) {
            Log.e(TAG, "缺少蓝牙权限")
            return
        }
        
        if (bluetooth == null) {
            Log.e(TAG, "设备不支持蓝牙")
            return
        }
        
        if (!bluetooth.isEnabled) {
            Log.e(TAG, "蓝牙未启用")
            return
        }
        
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(receiver, filter)
            
            if (bluetooth.isDiscovering) {
                bluetooth.cancelDiscovery()
            }
            
            bluetooth.startDiscovery()
            Log.d(TAG, "开始蓝牙扫描")
        } catch (e: Exception) {
            Log.e(TAG, "启动扫描失败: ${e.message}")
        }
    }

    fun stopScan() {
        try {
            if (bluetooth?.isDiscovering == true) {
                bluetooth.cancelDiscovery()
            }
            context.unregisterReceiver(receiver)
            Log.d(TAG, "停止蓝牙扫描")
        } catch (e: Exception) {
            Log.e(TAG, "停止扫描失败: ${e.message}")
        }
    }
    
    fun startServer() {
        if (!checkBluetoothPermissions()) {
            Log.e(TAG, "缺少蓝牙权限")
            return
        }
        
        Thread {
            try {
                serverSocket = bluetooth?.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
                Log.d(TAG, "蓝牙服务器已启动，等待连接...")
                
                while (!isConnected) {
                    val socket = serverSocket?.accept()
                    if (socket != null) {
                        clientSocket = socket
                        isConnected = true
                        serverSocket?.close()
                        
                        onConnectionStateChanged?.invoke(true, "已连接")
                        Log.d(TAG, "客户端已连接")
                        
                        startDataListener(socket)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "服务器启动失败: ${e.message}")
                onConnectionStateChanged?.invoke(false, "服务器启动失败")
            }
        }.start()
    }
    
    fun connectToDevice(device: BluetoothDevice) {
        if (!checkBluetoothPermissions()) {
            Log.e(TAG, "缺少蓝牙权限")
            return
        }
        
        Thread {
            try {
                if (bluetooth?.isDiscovering == true) {
                    bluetooth.cancelDiscovery()
                }
                
                clientSocket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                clientSocket?.connect()
                
                isConnected = true
                onConnectionStateChanged?.invoke(true, "已连接到 ${device.name}")
                Log.d(TAG, "已连接到 ${device.name}")
                
                clientSocket?.let { startDataListener(it) }
            } catch (e: IOException) {
                Log.e(TAG, "连接失败: ${e.message}")
                onConnectionStateChanged?.invoke(false, "连接失败: ${e.message}")
                closeConnection()
            }
        }.start()
    }
    
    private fun startDataListener(socket: BluetoothSocket) {
        Thread {
            try {
                val buffer = ByteArray(4096)
                val inputStream = socket.inputStream
                
                while (isConnected) {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val data = buffer.copyOf(bytes)
                        onDataReceived?.invoke(data)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "数据监听失败: ${e.message}")
                onConnectionStateChanged?.invoke(false, "连接断开")
                isConnected = false
            }
        }.start()
    }
    
    fun sendData(data: ByteArray): Boolean {
        return try {
            if (!isConnected || clientSocket == null) {
                Log.e(TAG, "未连接")
                return false
            }
            
            clientSocket?.outputStream?.write(data)
            clientSocket?.outputStream?.flush()
            true
        } catch (e: IOException) {
            Log.e(TAG, "发送数据失败: ${e.message}")
            false
        }
    }
    
    fun closeConnection() {
        try {
            clientSocket?.close()
            serverSocket?.close()
            clientSocket = null
            serverSocket = null
            isConnected = false
            onConnectionStateChanged?.invoke(false, "连接已关闭")
            Log.d(TAG, "连接已关闭")
        } catch (e: IOException) {
            Log.e(TAG, "关闭连接失败: ${e.message}")
        }
    }
    
    private fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }
}