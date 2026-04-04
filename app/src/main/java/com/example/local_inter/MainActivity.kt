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
    lateinit var nasServer: NasServer
    private lateinit var p2pManager: P2pManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化服务
        nasServer = NasServer(9999)
        p2pManager = P2pManager(this)
        SecurityGuard(this, nasServer).startMonitor()

        // 自动启动P2P + NAS
        p2pManager.createGroup()
        nasServer.startServer()

        // 底部导航
        val nav = findViewById<BottomNavigationView>(R.id.nav_view)
        nav.setupWithNavController(findNavController(R.id.nav_host))
    }
}