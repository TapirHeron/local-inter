package com.example.local_inter.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.local_inter.MainActivity
import com.example.local_inter.R
import java.net.InetAddress
import java.net.NetworkInterface

class HomeFragment : Fragment() {
    private lateinit var tvStatus: TextView
    private lateinit var tvIp: TextView
    private lateinit var btnCopy: Button
    private lateinit var btnRefresh: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            tvStatus = view.findViewById(R.id.tv_status)
            tvIp = view.findViewById(R.id.tv_ip)
            btnCopy = view.findViewById(R.id.btn_copy)
            btnRefresh = view.findViewById(R.id.btn_refresh)

            val activity = requireActivity() as MainActivity
            val ipAddress = getLocalIpAddress()
            val nasUrl = "http://$ipAddress:${MainActivity.NAS_PORT}"
            
            tvIp.text = "访问地址：$nasUrl"
            
            if (activity.nasServer.isRunning) {
                tvStatus.text = "服务已启动"
                tvStatus.setTextColor(0xFF00C853.toInt())
            } else {
                tvStatus.text = "服务未启动"
                tvStatus.setTextColor(0xFFFF5722.toInt())
            }

            btnCopy.setOnClickListener {
                copyToClipboard(nasUrl)
            }

            btnRefresh.setOnClickListener {
                refreshStatus(activity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(".") == true) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
            "127.0.0.1"
        } catch (e: Exception) {
            e.printStackTrace()
            "127.0.0.1"
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("NAS Address", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun refreshStatus(activity: MainActivity) {
        val ipAddress = getLocalIpAddress()
        val nasUrl = "http://$ipAddress:${MainActivity.NAS_PORT}"
        tvIp.text = "访问地址：$nasUrl"
        
        if (activity.nasServer.isRunning) {
            tvStatus.text = "服务已启动"
            tvStatus.setTextColor(0xFF00C853.toInt())
        } else {
            tvStatus.text = "服务未启动"
            tvStatus.setTextColor(0xFFFF5722.toInt())
        }
        Toast.makeText(context, "已刷新", Toast.LENGTH_SHORT).show()
    }
}