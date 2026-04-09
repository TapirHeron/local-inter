package com.example.local_inter.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.local_inter.MainActivity
import com.example.local_inter.R

class SettingsFragment : Fragment() {
    private lateinit var etDeviceName: EditText
    private lateinit var btnSaveDeviceName: Button
    private lateinit var etSavePath: EditText
    private lateinit var btnSaveSavePath: Button
    private lateinit var switchAutoScan: Switch
    private lateinit var tvVersion: TextView
    private lateinit var tvProtocol: TextView
    private lateinit var tvDeveloper: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_settings, container, false)
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化视图
        etDeviceName = view.findViewById(R.id.et_device_name)
        btnSaveDeviceName = view.findViewById(R.id.btn_save_device_name)
        etSavePath = view.findViewById(R.id.et_save_path)
        btnSaveSavePath = view.findViewById(R.id.btn_save_save_path)
        switchAutoScan = view.findViewById(R.id.switch_auto_scan)
        tvVersion = view.findViewById(R.id.tv_version)
        tvProtocol = view.findViewById(R.id.tv_protocol)
        tvDeveloper = view.findViewById(R.id.tv_developer)
        
        // 加载当前设置
        loadSettings()
        
        // 保存设备名称
        btnSaveDeviceName.setOnClickListener {
            saveDeviceName()
        }
        
        // 保存保存路径
        btnSaveSavePath.setOnClickListener {
            saveSavePath()
        }
        
        // 自动扫描开关
        switchAutoScan.setOnCheckedChangeListener { _, isChecked ->
            saveAutoScanSetting(isChecked)
        }
    }
    
    /**
     * 加载设置
     */
    private fun loadSettings() {
        val activity = requireActivity() as MainActivity
        val prefs = activity.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        
        // 设备名称
        val deviceName = prefs.getString("device_name", android.os.Build.MODEL) ?: android.os.Build.MODEL
        etDeviceName.setText(deviceName)
        
        // 保存路径
        val savePath = prefs.getString("save_path", "/storage/emulated/0/LanShare") ?: "/storage/emulated/0/LanShare"
        etSavePath.setText(savePath)
        
        // 自动扫描
        val autoScan = prefs.getBoolean("auto_scan", true)
        switchAutoScan.isChecked = autoScan
        
        // 关于信息
        tvVersion.text = "版本: 1.0.0"
        tvProtocol.text = "协议: TCP/IP + UDP广播"
        tvDeveloper.text = "开发者: Local Inter Team"
    }
    
    /**
     * 保存设备名称
     */
    private fun saveDeviceName() {
        val deviceName = etDeviceName.text.toString().trim()
        if (deviceName.isEmpty()) {
            Toast.makeText(context, "设备名称不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        
        val activity = requireActivity() as MainActivity
        val prefs = activity.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("device_name", deviceName).apply()
        
        Toast.makeText(context, "设备名称已保存", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 保存保存路径
     */
    private fun saveSavePath() {
        val savePath = etSavePath.text.toString().trim()
        if (savePath.isEmpty()) {
            Toast.makeText(context, "保存路径不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        
        val activity = requireActivity() as MainActivity
        val prefs = activity.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("save_path", savePath).apply()
        
        Toast.makeText(context, "保存路径已保存", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 保存自动扫描设置
     */
    private fun saveAutoScanSetting(enabled: Boolean) {
        val activity = requireActivity() as MainActivity
        val prefs = activity.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_scan", enabled).apply()
        
        Toast.makeText(context, if (enabled) "已启用自动扫描" else "已禁用自动扫描", Toast.LENGTH_SHORT).show()
    }
}
