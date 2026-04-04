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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)

            // 初始化服务
            nasServer = NasServer(NAS_PORT)
            p2pManager = P2pManager(this)
            
            try {
                SecurityGuard(this, nasServer).startMonitor()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 底部导航
            val navView = findViewById<BottomNavigationView>(R.id.nav_view)
            val navController = findNavController(R.id.nav_host)
            navView.setupWithNavController(navController)

            // 异步启动服务
            startServices()
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果初始化失败，至少显示一个空白界面避免闪退
            setContentView(R.layout.activity_main)
        }
    }

    private fun startServices() {
        Thread {
            try {
                nasServer.startServer()
                p2pManager.createGroup()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        nasServer.stopServer()
        p2pManager.removeGroup()
    }
}