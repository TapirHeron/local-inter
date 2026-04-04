package com.example.local_inter.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.example.local_inter.core.NasServer

class SecurityGuard(
    private val context: Context,
    private val nasServer: NasServer
) {
    fun startMonitor() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder().build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    nasServer.stopServer()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}