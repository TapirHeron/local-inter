package com.example.local_inter.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.local_inter.MainActivity
import com.example.local_inter.R
import com.example.local_inter.core.DeviceDiscover
import com.example.local_inter.core.DeviceInfo
import java.net.InetAddress
import java.net.NetworkInterface

class DevicesFragment : Fragment() {
    private lateinit var recyclerDevices: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnRefresh: ImageButton
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvLocalInfo: TextView
    private lateinit var deviceDiscover: DeviceDiscover
    private val deviceList = mutableListOf<DeviceInfo>()
    private lateinit var adapter: DeviceAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateDeviceStatus()
            handler.postDelayed(this, 5000) // 每5秒更新一次状态
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_devices, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerDevices = view.findViewById(R.id.recycler_devices)
        tvEmpty = view.findViewById(R.id.tv_empty)
        btnRefresh = view.findViewById(R.id.btn_refresh)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        tvLocalInfo = view.findViewById(R.id.tv_local_info)
        
        recyclerDevices.layoutManager = LinearLayoutManager(context)
        adapter = DeviceAdapter(deviceList)
        recyclerDevices.adapter = adapter
        
        // 显示本机信息
        updateLocalInfo()
        
        // 初始化设备发现
        deviceDiscover = DeviceDiscover(requireContext())
        deviceDiscover.onDeviceFound = { ip, msg ->
            activity?.runOnUiThread {
                addDevice(ip, msg)
            }
        }
        
        // 开始监听和广播
        deviceDiscover.start()
        deviceDiscover.broadcast()
        
        // 启动状态更新定时器
        handler.post(updateRunnable)
        
        // 下拉刷新
        swipeRefresh.setOnRefreshListener {
            refreshDevices()
        }
        
        // 刷新按钮
        btnRefresh.setOnClickListener {
            refreshDevices()
            Toast.makeText(context, "正在搜索设备...", Toast.LENGTH_SHORT).show()
        }
        
        updateEmptyState(true)
    }
    
    override fun onResume() {
        super.onResume()
        // 每次返回页面时重新广播
        deviceDiscover.broadcast()
        updateLocalInfo()
        handler.post(updateRunnable)
    }
    
    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // 停止设备发现以释放资源
        handler.removeCallbacks(updateRunnable)
        deviceDiscover.stop()
    }

    private fun addDevice(ip: String, msg: String) {
        // 获取本机IP，过滤掉自己
        val localIp = getLocalIpAddress()
        if (ip == localIp) {
            return
        }
        
        // 检查是否已存在
        val existingIndex = deviceList.indexOfFirst { it.ipAddress == ip }
        if (existingIndex >= 0) {
            // 更新现有设备的时间戳
            val oldDevice = deviceList[existingIndex]
            deviceList[existingIndex] = oldDevice.copy(lastSeen = System.currentTimeMillis(), isOnline = true)
        } else {
            // 添加新设备
            deviceList.add(DeviceInfo(name = msg, ipAddress = ip, lastSeen = System.currentTimeMillis()))
        }
        adapter.notifyDataSetChanged()
        updateEmptyState(false)
    }
    
    /**
     * 更新设备在线状态
     */
    private fun updateDeviceStatus() {
        val currentTime = System.currentTimeMillis()
        var changed = false
        
        for (i in deviceList.indices) {
            val device = deviceList[i]
            val newOnlineStatus = device.checkOnlineStatus()
            if (device.isOnline != newOnlineStatus) {
                deviceList[i] = device.copy(isOnline = newOnlineStatus)
                changed = true
            }
        }
        
        if (changed) {
            adapter.notifyDataSetChanged()
        }
        
        // 移除长时间离线的设备（超过30秒）
        val iterator = deviceList.iterator()
        while (iterator.hasNext()) {
            val device = iterator.next()
            if (currentTime - device.lastSeen > 30000) {
                iterator.remove()
                changed = true
            }
        }
        
        if (changed) {
            adapter.notifyDataSetChanged()
            updateEmptyState(deviceList.isEmpty())
        }
    }
    
    /**
     * 刷新设备列表
     */
    private fun refreshDevices() {
        deviceDiscover.clearDiscoveredDevices()
        deviceList.clear()
        adapter.notifyDataSetChanged()
        updateEmptyState(true)
        deviceDiscover.broadcast()
        swipeRefresh.isRefreshing = false
    }
    
    /**
     * 更新本机信息显示
     */
    private fun updateLocalInfo() {
        val deviceName = android.os.Build.MODEL
        val ipAddress = getLocalIpAddress()
        tvLocalInfo.text = "$deviceName | $ipAddress"
    }
    
    /**
     * 获取本机IP地址
     */
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

    private fun updateEmptyState(isEmpty: Boolean) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerDevices.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    // 设备列表适配器
    private inner class DeviceAdapter(private val devices: List<DeviceInfo>) :
        RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
        
        inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_device_name)
            val tvIp: TextView = view.findViewById(R.id.tv_device_ip)
            val tvStatus: TextView = view.findViewById(R.id.tv_status)
            val btnConnect: android.widget.Button = view.findViewById(R.id.btn_connect)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_device, parent, false)
            return DeviceViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            val device = devices[position]
            holder.tvName.text = if (device.name.isNotEmpty()) device.name else "设备 ${position + 1}"
            holder.tvIp.text = device.ipAddress
            
            // 设置在线状态
            if (device.isOnline) {
                holder.tvStatus.text = "● 在线"
                holder.tvStatus.setTextColor(0xFF00C853.toInt()) // 绿色
                holder.btnConnect.isEnabled = true
                holder.btnConnect.setBackgroundColor(0xFF4CAF50.toInt())
            } else {
                holder.tvStatus.text = "● 离线"
                holder.tvStatus.setTextColor(0xFF999999.toInt()) // 灰色
                holder.btnConnect.isEnabled = false
                holder.btnConnect.setBackgroundColor(0xFFCCCCCC.toInt())
            }
            
            // 连接按钮点击事件
            holder.btnConnect.setOnClickListener {
                if (device.isOnline) {
                    connectToDevice(device)
                }
            }
        }
        
        override fun getItemCount() = devices.size
    }
    
    private fun connectToDevice(device: DeviceInfo) {
        try {
            // 跳转到文件传输界面
            val intent = Intent(context, FileTransferActivity::class.java).apply {
                putExtra("device_name", device.name)
                putExtra("device_ip", device.ipAddress)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "连接失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}