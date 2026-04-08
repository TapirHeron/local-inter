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

class MainActivity : AppCompatActivity() {
    companion object {
        const val NAS_PORT = 9999
    }

    lateinit var nasServer: NasServer
    private lateinit var p2pManager: P2pManager
    internal lateinit var securityGuard: SecurityGuard

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
    }
}