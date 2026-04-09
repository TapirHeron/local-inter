package com.example.local_inter.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.local_inter.R
import com.example.local_inter.core.FileTransferManager
import com.example.local_inter.core.TransferHistoryManager

class FileTransferActivity : AppCompatActivity() {
    lateinit var fileTransferManager: FileTransferManager
    lateinit var transferHistoryManager: TransferHistoryManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_transfer)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "文件传输"
        
        // 初始化传输管理器
        fileTransferManager = FileTransferManager()
        transferHistoryManager = TransferHistoryManager(this)
        
        if (savedInstanceState == null) {
            val deviceName = intent.getStringExtra("device_name") ?: "未知设备"
            val deviceIp = intent.getStringExtra("device_ip") ?: ""
            
            val fragment = FileTransferFragment().apply {
                arguments = Bundle().apply {
                    putString("device_name", deviceName)
                    putString("device_ip", deviceIp)
                }
            }
            
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
