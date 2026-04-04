package com.example.local_inter.core

import android.content.Context
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DeviceDiscover(private val context: Context) {
    private val port = 9898
    private val socket = DatagramSocket(port)
    var onDeviceFound: ((String, String) -> Unit)? = null

    fun start() {
        Thread {
            val buffer = ByteArray(1024)
            while (true) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                val ip = packet.address.hostAddress ?: ""
                val msg = String(buffer, 0, packet.length)
                onDeviceFound?.invoke(ip, msg)
            }
        }.start()
    }

    fun broadcast() {
        Thread {
            val ds = DatagramSocket()
            val msg = "LAN_DEVICE"
            val packet = DatagramPacket(
                msg.toByteArray(), msg.length,
                InetAddress.getByName("255.255.255.255"), port
            )
            ds.send(packet)
            ds.close()
        }.start()
    }
}