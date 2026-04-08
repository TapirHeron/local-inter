package com.example.local_inter.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
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
    private lateinit var btnToggle: Button
    private lateinit var btnCopy: Button
    private lateinit var switchEncryption: Switch
    private lateinit var btnKeyManage: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tvStatus = view.findViewById(R.id.tv_status)
        tvIp = view.findViewById(R.id.tv_ip)
        btnToggle = view.findViewById(R.id.btn_toggle)
        btnCopy = view.findViewById(R.id.btn_copy)
        switchEncryption = view.findViewById(R.id.switch_encryption)
        btnKeyManage = view.findViewById(R.id.btn_key_manage)

        val activity = requireActivity() as MainActivity
        updateUI(activity)

        btnToggle.setOnClickListener {
            toggleNasService(activity)
        }

        btnCopy.setOnClickListener {
            val ipAddress = getLocalIpAddress()
            val nasUrl = "http://$ipAddress:${MainActivity.NAS_PORT}"
            copyToClipboard(nasUrl)
        }
        
        // 加密开关
        switchEncryption.setOnCheckedChangeListener { _, isChecked ->
            activity.securityGuard.isEncryptionEnabled = isChecked
            Toast.makeText(
                context,
                if (isChecked) "已启用文件加密" else "已禁用文件加密",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        // 密钥管理按钮
        btnKeyManage.setOnClickListener {
            showKeyManagementDialog(activity)
        }
    }

    override fun onResume() {
        super.onResume()
        val activity = requireActivity() as MainActivity
        updateUI(activity)
    }

    private fun updateUI(activity: MainActivity) {
        val ipAddress = getLocalIpAddress()
        val nasUrl = "http://$ipAddress:${MainActivity.NAS_PORT}"
        tvIp.text = "访问地址：$nasUrl"
        
        if (activity.nasServer.isRunning) {
            tvStatus.text = "服务已启动"
            tvStatus.setTextColor(0xFF00C853.toInt())
            btnToggle.text = "停止 NAS 服务"
            btnToggle.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF5722.toInt())
        } else {
            tvStatus.text = "服务未启动"
            tvStatus.setTextColor(0xFFFF5722.toInt())
            btnToggle.text = "启动 NAS 服务"
            btnToggle.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt())
        }
        
        // 更新加密开关状态
        switchEncryption.isChecked = activity.securityGuard.isEncryptionEnabled
    }

    private fun toggleNasService(activity: MainActivity) {
        if (activity.nasServer.isRunning) {
            activity.nasServer.stopServer()
            Toast.makeText(context, "NAS 服务已停止", Toast.LENGTH_SHORT).show()
        } else {
            Thread {
                try {
                    activity.nasServer.startServer()
                    activity.runOnUiThread {
                        Toast.makeText(context, "NAS 服务已启动", Toast.LENGTH_SHORT).show()
                        updateUI(activity)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    activity.runOnUiThread {
                        Toast.makeText(context, "启动失败：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
        updateUI(activity)
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
    
    /**
     * 显示密钥管理对话框
     */
    private fun showKeyManagementDialog(activity: MainActivity) {
        val securityGuard = activity.securityGuard
        val currentKey = securityGuard.getEncryptionKeyBase64()
        
        val options = arrayOf(
            "查看当前密钥",
            "复制密钥到剪贴板",
            "从其他设备导入密钥",
            "生成新密钥（旧文件将无法解密）"
        )
        
        AlertDialog.Builder(requireContext())
            .setTitle("🔐 加密密钥管理")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // 查看密钥
                        AlertDialog.Builder(requireContext())
                            .setTitle("当前密钥")
                            .setMessage(currentKey)
                            .setPositiveButton("确定", null)
                            .show()
                    }
                    1 -> {
                        // 复制密钥
                        copyToClipboard(currentKey)
                        Toast.makeText(context, "密钥已复制，请安全分享给其他设备", Toast.LENGTH_LONG).show()
                    }
                    2 -> {
                        // 导入密钥
                        showImportKeyDialog(securityGuard)
                    }
                    3 -> {
                        // 生成新密钥
                        AlertDialog.Builder(requireContext())
                            .setTitle("⚠️ 警告")
                            .setMessage("生成新密钥后，之前加密的文件将无法解密！确定要继续吗？")
                            .setPositiveButton("确定") { _, _ ->
                                securityGuard.resetEncryptionKey()
                                Toast.makeText(context, "已生成新密钥", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("关闭", null)
            .show()
    }
    
    /**
     * 显示导入密钥对话框
     */
    private fun showImportKeyDialog(securityGuard: com.example.local_inter.core.SecurityGuard) {
        val editText = android.widget.EditText(requireContext())
        editText.hint = "粘贴 Base64 格式的密钥"
        editText.maxLines = 3
        
        AlertDialog.Builder(requireContext())
            .setTitle("导入密钥")
            .setMessage("请输入从其他设备复制的密钥：")
            .setView(editText)
            .setPositiveButton("导入") { _, _ ->
                val keyInput = editText.text.toString().trim()
                if (keyInput.isNotEmpty()) {
                    try {
                        securityGuard.setEncryptionKey(keyInput)
                        Toast.makeText(context, "密钥导入成功", Toast.LENGTH_SHORT).show()
                        updateUI(requireActivity() as MainActivity)
                    } catch (e: Exception) {
                        Toast.makeText(context, "密钥格式错误: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "请输入密钥", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}