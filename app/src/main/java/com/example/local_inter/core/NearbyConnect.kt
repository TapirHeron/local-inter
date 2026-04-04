package com.example.local_inter.core

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class NearbyConnect(private val context: Context) {
    private val bluetooth = BluetoothAdapter.getDefaultAdapter()
    var onNearbyDevice: ((String) -> Unit)? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            device?.let {
                val name = it.name ?: "未知设备"
                onNearbyDevice?.invoke(name)
            }
        }
    }

    fun startScan() {
        context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        bluetooth.startDiscovery()
    }

    fun stopScan() {
        context.unregisterReceiver(receiver)
    }
}