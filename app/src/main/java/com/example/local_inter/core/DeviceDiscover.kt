package com.example.local_inter.core

import android.content.Context
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class DeviceDiscover(private val context: Context) {
    companion object {
        private const val TAG = "DeviceDiscover"
        private const val PORT = 9898
        private const val BROADCAST_INTERVAL = 5000L
    }
    
    private val port = PORT
    private var socket: DatagramSocket? = null
    var onDeviceFound: ((String, String) -> Unit)? = null
    
    private val isRunning = AtomicBoolean(false)
    private var broadcastThread: Thread? = null
    private var receiveThread: Thread? = null
    
    private val discoveredDevices = mutableMapOf<String, Pair<String, Long>>()

    fun start() {
        if (isRunning.get()) {
            Log.w(TAG, "设备发现已在运行")
            return
        }
        
        isRunning.set(true)
        startReceiveThread()
        startBroadcastThread()
        Log.d(TAG, "设备发现已启动")
    }
    
    private fun startReceiveThread() {
        receiveThread = Thread {
            try {
                socket = DatagramSocket(port).apply {
                    broadcast = true
                    soTimeout = 0
                }
                
                val buffer = ByteArray(1024)
                while (isRunning.get()) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)
                        
                        val ip = packet.address.hostAddress ?: continue
                        val msg = String(buffer, 0, packet.length).trim()
                        val currentTime = System.currentTimeMillis()
                        
                        if (msg.startsWith("LAN_DEVICE")) {
                            val lastSeen = discoveredDevices[ip]?.second ?: 0
                            if (currentTime - lastSeen > 3000) {
                                discoveredDevices[ip] = Pair(msg, currentTime)
                                onDeviceFound?.invoke(ip, msg)
                                Log.d(TAG, "发现设备: $ip - $msg")
                            }
                        }
                    } catch (e: Exception) {
                        if (isRunning.get()) {
                            Log.e(TAG, "接收数据错误: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "接收线程异常: ${e.message}")
            }
        }.apply {
            name = "DeviceDiscover-Receiver"
            isDaemon = true
            start()
        }
    }
    
    private fun startBroadcastThread() {
        broadcastThread = Thread {
            while (isRunning.get()) {
                try {
                    broadcast()
                    Thread.sleep(BROADCAST_INTERVAL)
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "广播错误: ${e.message}")
                }
            }
        }.apply {
            name = "DeviceDiscover-Broadcaster"
            isDaemon = true
            start()
        }
    }

    fun broadcast() {
        try {
            val ds = DatagramSocket()
            ds.broadcast = true
            
            // 获取用户设置的设备名称
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val deviceName = prefs.getString("device_name", android.os.Build.MODEL) ?: android.os.Build.MODEL
            
            val msg = "LAN_DEVICE_$deviceName"
            val packet = DatagramPacket(
                msg.toByteArray(), msg.length,
                InetAddress.getByName("255.255.255.255"), port
            )
            ds.send(packet)
            ds.close()
            Log.d(TAG, "已广播设备信息: $msg")
        } catch (e: Exception) {
            Log.e(TAG, "广播失败: ${e.message}")
        }
    }
    
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            return
        }
        
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            Log.e(TAG, "关闭 Socket 失败: ${e.message}")
        }
        
        receiveThread?.interrupt()
        broadcastThread?.interrupt()
        
        receiveThread = null
        broadcastThread = null
        
        discoveredDevices.clear()
        Log.d(TAG, "设备发现已停止")
    }
    
    fun getDiscoveredDevices(): Map<String, Pair<String, Long>> {
        return discoveredDevices.toMap()
    }
    
    fun clearDiscoveredDevices() {
        discoveredDevices.clear()
    }
}