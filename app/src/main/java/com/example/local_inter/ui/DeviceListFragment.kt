package com.example.local_inter.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

class DevicesFragment : Fragment() {
    private lateinit var recyclerDevices: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnRefresh: ImageButton
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var deviceDiscover: DeviceDiscover
    private val deviceList = mutableListOf<Pair<String, String>>()
    private lateinit var adapter: DeviceAdapter

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
        
        recyclerDevices.layoutManager = LinearLayoutManager(context)
        adapter = DeviceAdapter(deviceList)
        recyclerDevices.adapter = adapter
        
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
        
        // 下拉刷新
        swipeRefresh.setOnRefreshListener {
            deviceDiscover.clearDiscoveredDevices()
            deviceList.clear()
            adapter.notifyDataSetChanged()
            updateEmptyState(true)
            deviceDiscover.broadcast()
            swipeRefresh.isRefreshing = false
        }
        
        // 刷新按钮
        btnRefresh.setOnClickListener {
            deviceDiscover.clearDiscoveredDevices()
            deviceList.clear()
            adapter.notifyDataSetChanged()
            updateEmptyState(true)
            deviceDiscover.broadcast()
            Toast.makeText(context, "正在搜索设备...", Toast.LENGTH_SHORT).show()
        }
        
        updateEmptyState(true)
    }
    
    override fun onResume() {
        super.onResume()
        // 每次返回页面时重新广播
        deviceDiscover.broadcast()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // 停止设备发现以释放资源
        deviceDiscover.stop()
    }

    private fun addDevice(ip: String, msg: String) {
        // 检查是否已存在
        if (!deviceList.any { it.first == ip }) {
            deviceList.add(Pair(ip, msg))
            adapter.notifyDataSetChanged()
            updateEmptyState(false)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerDevices.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    // 设备列表适配器
    private inner class DeviceAdapter(private val devices: List<Pair<String, String>>) :
        RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
        
        inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_device_name)
            val tvIp: TextView = view.findViewById(R.id.tv_device_ip)
            val btnConnect: android.widget.Button = view.findViewById(R.id.btn_connect)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_device, parent, false)
            return DeviceViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            val device = devices[position]
            holder.tvName.text = if (device.second.isNotEmpty()) device.second else "设备 ${position + 1}"
            holder.tvIp.text = device.first
            
            // 连接按钮点击事件
            holder.btnConnect.setOnClickListener {
                connectToDevice(device.first)
            }
        }
        
        override fun getItemCount() = devices.size
    }
    
    private fun connectToDevice(ip: String) {
        try {
            val url = "http://$ip:${MainActivity.NAS_PORT}/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "连接失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}