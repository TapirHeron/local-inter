package com.example.local_inter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.local_inter.core.NasServer
import com.example.local_inter.core.P2pManager
import com.example.local_inter.core.SecurityGuard
import com.example.local_inter.core.FileTransferManager
import com.example.local_inter.core.TransferHistoryManager

class MainActivity : AppCompatActivity() {
    companion object {
        const val NAS_PORT = 9999
    }

    lateinit var nasServer: NasServer
    private lateinit var p2pManager: P2pManager
    internal lateinit var securityGuard: SecurityGuard
    internal lateinit var fileTransferManager: FileTransferManager
    internal lateinit var transferHistoryManager: TransferHistoryManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
            initializeServices()
        } else {
            Toast.makeText(this, "部分权限被拒绝，功能可能受限", Toast.LENGTH_LONG).show()
            initializeServices()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (checkAndRequestPermissions()) {
            initializeServices()
        }

        setupNavigation()
    }
    
    private fun initializeServices() {
        try {
            securityGuard = SecurityGuard(this, NasServer(NAS_PORT))
            nasServer = NasServer(NAS_PORT, securityGuard)
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("MainActivity", "NAS Server 初始化失败: ${e.message}")
        }
        
        try {
            p2pManager = P2pManager(this)
            p2pManager.registerBroadcastReceivers()
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("MainActivity", "P2P Manager 初始化失败: ${e.message}")
        }
        
        try {
            securityGuard.startMonitor()
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("MainActivity", "SecurityGuard 初始化失败: ${e.message}")
        }
        
        // 初始化文件传输管理器
        try {
            fileTransferManager = FileTransferManager()
            
            // 设置文件接收请求回调
            fileTransferManager.onFileReceiveRequest = { fileName, fileSize, senderIp ->
                runOnUiThread {
                    android.util.Log.d("MainActivity", "收到文件请求: $fileName from $senderIp")
                    showFileReceiveDialog(fileName, fileSize, senderIp)
                }
            }
            
            fileTransferManager.startReceiveService()
            android.util.Log.d("MainActivity", "文件传输服务已启动")
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("MainActivity", "文件传输服务初始化失败: ${e.message}")
        }
        
        // 初始化历史记录管理器
        try {
            transferHistoryManager = TransferHistoryManager(this)
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("MainActivity", "历史记录管理器初始化失败: ${e.message}")
        }
    }
    
    private fun setupNavigation() {
        try {
            val navView = findViewById<BottomNavigationView>(R.id.nav_view)
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host) as? androidx.navigation.fragment.NavHostFragment
            val navController = navHostFragment?.navController
            
            if (navController != null) {
                navView.setupWithNavController(navController)
                android.util.Log.d("MainActivity", "导航设置成功")
            } else {
                android.util.Log.e("MainActivity", "无法获取 NavController")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("MainActivity", "导航初始化失败: ${e.message}")
        }
    }
    
    private fun checkAndRequestPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        
        return if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
            false
        } else {
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nasServer.stopServer()
        p2pManager.unregisterBroadcastReceivers()
        p2pManager.removeGroup()
        fileTransferManager.stopReceiveService()
    }
    
    /**
     * 显示文件接收确认对话框
     */
    private fun showFileReceiveDialog(fileName: String, fileSize: Long, senderIp: String) {
        // 验证文件名
        if (fileName.isEmpty()) {
            android.widget.Toast.makeText(this, "错误：文件名为空", android.widget.Toast.LENGTH_SHORT).show()
            fileTransferManager.rejectFile()
            return
        }
        
        val fileSizeStr = formatFileSize(fileSize)
        
        android.app.AlertDialog.Builder(this)
            .setTitle("接收文件")
            .setMessage("收到来自 $senderIp 的文件：\n\n文件名：$fileName\n大小：$fileSizeStr\n\n是否接收？")
            .setPositiveButton("接收") { _, _ ->
                // 获取保存路径 - 使用应用专属目录避免权限问题
                val saveDir = getExternalFilesDir(null)
                val lanShareDir = java.io.File(saveDir, "LanShare")
                if (!lanShareDir.exists()) {
                    lanShareDir.mkdirs()
                }
                
                val saveFile = java.io.File(lanShareDir, fileName)
                
                // 设置接收回调
                fileTransferManager.onReceiveProgress = { progress ->
                    runOnUiThread {
                        // 可以显示进度通知
                    }
                }
                
                fileTransferManager.onReceiveComplete = { success, filePath ->
                    runOnUiThread {
                        // 添加历史记录
                        val record = com.example.local_inter.core.TransferRecord(
                            fileName = fileName,
                            fileSize = fileSize,
                            deviceName = senderIp,
                            deviceIp = senderIp,
                            isSuccess = success,
                            isSent = false,
                            filePath = filePath,
                            errorMessage = if (success) null else "接收失败"
                        )
                        transferHistoryManager.addRecord(record)
                        
                        if (success) {
                            android.app.AlertDialog.Builder(this)
                                .setTitle("接收成功")
                                .setMessage("文件已保存到：\n$filePath")
                                .setPositiveButton("确定", null)
                                .show()
                        } else {
                            android.widget.Toast.makeText(this, "文件接收失败", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                
                // 开始接收
                fileTransferManager.acceptAndReceiveFile(saveFile.absolutePath)
            }
            .setNegativeButton("拒绝") { _, _ ->
                fileTransferManager.rejectFile()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${String.format("%.2f", size / 1024.0)} KB"
            size < 1024 * 1024 * 1024 -> "${String.format("%.2f", size / (1024.0 * 1024.0))} MB"
            else -> "${String.format("%.2f", size / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
}