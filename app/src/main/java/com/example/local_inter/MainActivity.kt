package com.example.local_inter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.local_inter.core.NasServer
import com.example.local_inter.core.P2pManager
import com.example.local_inter.core.SecurityGuard

class MainActivity : AppCompatActivity() {
    companion object {
        const val NAS_PORT = 9999
    }

    lateinit var nasServer: NasServer
    private lateinit var p2pManager: P2pManager
    internal lateinit var securityGuard: SecurityGuard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // 初始化安全模块
            securityGuard = SecurityGuard(this, NasServer(NAS_PORT))
            
            // 初始化服务（传入 securityGuard）
            nasServer = NasServer(NAS_PORT, securityGuard)
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("MainActivity", "NAS Server 初始化失败: ${e.message}")
        }
        
        try {
            p2pManager = P2pManager(this)
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

        // 底部导航
        try {
            val navView = findViewById<BottomNavigationView>(R.id.nav_view)
            val navController = findNavController(R.id.nav_host)
            navView.setupWithNavController(navController)
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("MainActivity", "导航初始化失败: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nasServer.stopServer()
        p2pManager.removeGroup()
    }
}