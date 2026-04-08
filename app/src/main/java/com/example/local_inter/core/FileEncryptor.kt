package com.example.local_inter.core

import android.util.Base64
import java.io.*
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 文件加密工具类 - 使用 AES-256-GCM 加密算法
 * 提供文件级别的加密和解密功能
 */
object FileEncryptor {
    
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256 // AES-256
    private const val GCM_IV_LENGTH = 12 // GCM 标准 IV 长度
    private const val GCM_TAG_LENGTH = 128 // GCM 认证标签长度（位）
    private const val BUFFER_SIZE = 8192
    
    /**
     * 生成随机密钥
     */
    fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(KEY_SIZE, SecureRandom())
        return keyGenerator.generateKey()
    }
    
    /**
     * 从字节数组恢复密钥
     */
    fun restoreKey(keyBytes: ByteArray): SecretKey {
        return SecretKeySpec(keyBytes, ALGORITHM)
    }
    
    /**
     * 将密钥转换为字节数组（用于存储或传输）
     */
    fun keyToBytes(key: SecretKey): ByteArray {
        return key.encoded
    }
    
    /**
     * 将密钥编码为 Base64 字符串（便于存储和分享）
     */
    fun keyToBase64(key: SecretKey): String {
        return Base64.encodeToString(key.encoded, Base64.DEFAULT)
    }
    
    /**
     * 从 Base64 字符串恢复密钥
     */
    fun restoreKeyFromBase64(base64Key: String): SecretKey {
        val keyBytes = Base64.decode(base64Key, Base64.DEFAULT)
        return restoreKey(keyBytes)
    }
    
    /**
     * 加密文件
     * @param inputFile 原始文件
     * @param outputFile 加密后的文件
     * @param key 加密密钥
     */
    fun encryptFile(inputFile: File, outputFile: File, key: SecretKey) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val tagLength = GCM_TAG_LENGTH / 8
        
        FileInputStream(inputFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                // 写入 IV（初始化向量）
                fos.write(iv)
                
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val encrypted = cipher.update(buffer, 0, bytesRead)
                    if (encrypted != null) {
                        fos.write(encrypted)
                    }
                }
                
                // 完成加密并写入最后的块
                val finalBlock = cipher.doFinal()
                if (finalBlock != null) {
                    fos.write(finalBlock)
                }
                
                fos.flush()
            }
        }
    }
    
    /**
     * 解密文件
     * @param inputFile 加密的文件
     * @param outputFile 解密后的文件
     * @param key 解密密钥
     */
    fun decryptFile(inputFile: File, outputFile: File, key: SecretKey) {
        FileInputStream(inputFile).use { fis ->
            // 读取 IV
            val iv = ByteArray(GCM_IV_LENGTH)
            fis.read(iv)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
            
            FileOutputStream(outputFile).use { fos ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val decrypted = cipher.update(buffer, 0, bytesRead)
                    if (decrypted != null) {
                        fos.write(decrypted)
                    }
                }
                
                // 完成解密
                val finalBlock = cipher.doFinal()
                if (finalBlock != null) {
                    fos.write(finalBlock)
                }
                
                fos.flush()
            }
        }
    }
    
    /**
     * 加密数据流（用于网络传输）
     * @param inputStream 原始数据流
     * @param outputStream 加密后的数据流
     * @param key 加密密钥
     */
    fun encryptStream(inputStream: InputStream, outputStream: OutputStream, key: SecretKey) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        // 写入 IV
        outputStream.write(cipher.iv)
        
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val encrypted = cipher.update(buffer, 0, bytesRead)
            if (encrypted != null) {
                outputStream.write(encrypted)
            }
        }
        
        // 完成加密
        val finalBlock = cipher.doFinal()
        if (finalBlock != null) {
            outputStream.write(finalBlock)
        }
        
        outputStream.flush()
    }
    
    /**
     * 解密数据流（用于网络接收）
     * @param inputStream 加密的数据流
     * @param outputStream 解密后的数据流
     * @param key 解密密钥
     */
    fun decryptStream(inputStream: InputStream, outputStream: OutputStream, key: SecretKey) {
        // 读取 IV
        val iv = ByteArray(GCM_IV_LENGTH)
        inputStream.read(iv)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
        
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val decrypted = cipher.update(buffer, 0, bytesRead)
            if (decrypted != null) {
                outputStream.write(decrypted)
            }
        }
        
        // 完成解密
        val finalBlock = cipher.doFinal()
        if (finalBlock != null) {
            outputStream.write(finalBlock)
        }
        
        outputStream.flush()
    }
    
    /**
     * 检查文件是否已加密（通过检查文件头部）
     * 注意：这是一个简单的启发式检查，不是 100% 准确
     */
    fun isEncrypted(file: File): Boolean {
        // 对于 GCM 模式，我们无法可靠地判断文件是否加密
        // 这里可以通过文件扩展名或其他元数据来判断
        return file.name.endsWith(".enc")
    }
    
    /**
     * 获取加密文件的原始大小（需要在加密时保存元数据）
     * 当前版本不支持，需要扩展文件格式来存储元数据
     */
    fun getOriginalFileSize(encryptedFile: File): Long {
        // TODO: 实现元数据读取
        return -1
    }
}
