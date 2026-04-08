package com.example.local_inter.core

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.example.local_inter.core.NasServer
import javax.crypto.SecretKey

class SecurityGuard(
    private val context: Context,
    private val nasServer: NasServer
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_ENCRYPTION_ENABLED = "encryption_enabled"
        private const val KEY_ENCRYPTION_KEY = "encryption_key"
        private const val KEY_AUTO_ENCRYPT = "auto_encrypt"
    }
    
    /**
     * 是否启用加密
     */
    var isEncryptionEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENCRYPTION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENCRYPTION_ENABLED, value).apply()
    
    /**
     * 是否自动加密上传的文件
     */
    var autoEncrypt: Boolean
        get() = prefs.getBoolean(KEY_AUTO_ENCRYPT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_ENCRYPT, value).apply()
    
    /**
     * 获取或生成加密密钥
     */
    fun getOrCreateEncryptionKey(): SecretKey {
        val keyBase64 = prefs.getString(KEY_ENCRYPTION_KEY, null)
        return if (keyBase64 != null) {
            FileEncryptor.restoreKeyFromBase64(keyBase64)
        } else {
            val newKey = FileEncryptor.generateKey()
            saveEncryptionKey(newKey)
            newKey
        }
    }
    
    /**
     * 保存加密密钥
     */
    fun saveEncryptionKey(key: SecretKey) {
        val keyBase64 = FileEncryptor.keyToBase64(key)
        prefs.edit().putString(KEY_ENCRYPTION_KEY, keyBase64).apply()
    }
    
    /**
     * 设置加密密钥（从其他设备同步）
     */
    fun setEncryptionKey(base64Key: String) {
        prefs.edit().putString(KEY_ENCRYPTION_KEY, base64Key).apply()
    }
    
    /**
     * 获取密钥的 Base64 字符串（用于分享到其他设备）
     */
    fun getEncryptionKeyBase64(): String {
        val key = getOrCreateEncryptionKey()
        return FileEncryptor.keyToBase64(key)
    }
    
    /**
     * 重置密钥（会生成新密钥，旧加密文件将无法解密）
     */
    fun resetEncryptionKey() {
        val newKey = FileEncryptor.generateKey()
        saveEncryptionKey(newKey)
    }
    
    fun startMonitor() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder().build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    nasServer.stopServer()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}