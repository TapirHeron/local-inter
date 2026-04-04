package com.example.local_inter.core

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper

class P2pManager(private val context: Context) {
    private val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        manager.initialize(context, Looper.getMainLooper(), null)
    } else {
        TODO("VERSION.SDK_INT < ICE_CREAM_SANDWICH")
    }

    fun createGroup() {
        manager.createGroup(channel, null)
    }

    fun removeGroup() {
        manager.removeGroup(channel, null)
    }
}