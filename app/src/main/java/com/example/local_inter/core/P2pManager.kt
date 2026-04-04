package com.example.local_inter.core

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager

class P2pManager(private val context: Context) {
    private var manager: WifiP2pManager?
    private var channel: WifiP2pManager.Channel?

    init {
        try {
            manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
            channel = manager?.initialize(context, context.mainLooper, null)
        } catch (e: Exception) {
            e.printStackTrace()
            manager = null
            channel = null
        }
    }

    fun createGroup() {
        try {
            if (manager != null && channel != null) {
                manager!!.createGroup(channel!!, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        // P2P 组创建成功
                    }

                    override fun onFailure(reason: Int) {
                        // P2P 组创建失败
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeGroup() {
        try {
            if (manager != null && channel != null) {
                manager!!.removeGroup(channel!!, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        // P2P 组移除成功
                    }

                    override fun onFailure(reason: Int) {
                        // P2P 组移除失败
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}