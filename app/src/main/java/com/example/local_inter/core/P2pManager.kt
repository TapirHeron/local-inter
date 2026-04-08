package com.example.local_inter.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log

class P2pManager(private val context: Context) {
    private var manager: WifiP2pManager?
    private var channel: WifiP2pManager.Channel?
    
    companion object {
        private const val TAG = "P2pManager"
    }

    var onPeersAvailable: ((List<WifiP2pDevice>) -> Unit)? = null
    var onConnectionChanged: ((WifiP2pInfo?) -> Unit)? = null
    var onThisDeviceChanged: ((WifiP2pDevice?) -> Unit)? = null
    
    private val peers = mutableListOf<WifiP2pDevice>()
    private var isRegistered = false

    init {
        try {
            manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
            channel = manager?.initialize(context, context.mainLooper, null)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "P2P 初始化失败: ${e.message}")
            manager = null
            channel = null
        }
    }

    fun createGroup() {
        try {
            if (manager != null && channel != null) {
                manager!!.createGroup(channel!!, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d(TAG, "P2P 组创建成功")
                    }

                    override fun onFailure(reason: Int) {
                        Log.e(TAG, "P2P 组创建失败: $reason")
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "createGroup 异常: ${e.message}")
        }
    }

    fun removeGroup() {
        try {
            if (manager != null && channel != null) {
                manager!!.removeGroup(channel!!, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d(TAG, "P2P 组移除成功")
                    }

                    override fun onFailure(reason: Int) {
                        Log.e(TAG, "P2P 组移除失败: $reason")
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "removeGroup 异常: ${e.message}")
        }
    }
    
    fun discoverPeers() {
        try {
            if (manager != null && channel != null) {
                manager!!.discoverPeers(channel!!, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d(TAG, "开始发现设备")
                    }

                    override fun onFailure(reason: Int) {
                        Log.e(TAG, "发现设备失败: $reason")
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "discoverPeers 异常: ${e.message}")
        }
    }
    
    fun requestPeers() {
        try {
            if (manager != null && channel != null) {
                manager!!.requestPeers(channel!!) { deviceList ->
                    peers.clear()
                    peers.addAll(deviceList.deviceList)
                    onPeersAvailable?.invoke(peers)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "requestPeers 异常: ${e.message}")
        }
    }
    
    fun connectToDevice(device: WifiP2pDevice) {
        try {
            if (manager != null && channel != null) {
                val config = WifiP2pConfig().apply {
                    deviceAddress = device.deviceAddress
                }
                
                manager!!.connect(channel!!, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d(TAG, "开始连接到 ${device.deviceName}")
                    }

                    override fun onFailure(reason: Int) {
                        Log.e(TAG, "连接失败: $reason")
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "connectToDevice 异常: ${e.message}")
        }
    }
    
    fun cancelDisconnect() {
        try {
            if (manager != null && channel != null) {
                manager!!.cancelConnect(channel!!, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d(TAG, "取消连接成功")
                    }

                    override fun onFailure(reason: Int) {
                        Log.e(TAG, "取消连接失败: $reason")
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "cancelDisconnect 异常: ${e.message}")
        }
    }
    
    fun registerBroadcastReceivers() {
        if (isRegistered) return
        
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        
        context.registerReceiver(p2pReceiver, filter)
        isRegistered = true
        Log.d(TAG, "广播接收器已注册")
    }
    
    fun unregisterBroadcastReceivers() {
        if (!isRegistered) return
        
        try {
            context.unregisterReceiver(p2pReceiver)
            isRegistered = false
            Log.d(TAG, "广播接收器已注销")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "注销广播接收器失败: ${e.message}")
        }
    }
    
    private val p2pReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        Log.d(TAG, "WiFi P2P 已启用")
                    } else {
                        Log.d(TAG, "WiFi P2P 已禁用")
                        peers.clear()
                        onPeersAvailable?.invoke(peers)
                    }
                }
                
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    requestPeers()
                }
                
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    if (manager == null || channel == null) return
                    
                    manager!!.requestConnectionInfo(channel!!) { info ->
                        onConnectionChanged?.invoke(info)
                        
                        if (info.groupFormed && info.isGroupOwner) {
                            Log.d(TAG, "作为 Group Owner 连接成功")
                        } else if (info.groupFormed) {
                            Log.d(TAG, "作为客户端连接成功")
                        }
                    }
                }
                
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device = intent.getParcelableExtra<WifiP2pDevice>(
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
                    )
                    onThisDeviceChanged?.invoke(device)
                }
            }
        }
    }
}