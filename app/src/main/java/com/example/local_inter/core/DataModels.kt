package com.example.local_inter.core

import java.io.Serializable

/**
 * 设备信息数据类
 */
data class DeviceInfo(
    val name: String,
    val ipAddress: String,
    val lastSeen: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true
) : Serializable {
    companion object {
        private const val ONLINE_TIMEOUT = 10000L // 10秒超时判定为离线
    }
    
    fun checkOnlineStatus(): Boolean {
        return System.currentTimeMillis() - lastSeen < ONLINE_TIMEOUT
    }
}

/**
 * 文件传输记录
 */
data class TransferRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val fileSize: Long,
    val deviceName: String,
    val deviceIp: String,
    val transferTime: Long = System.currentTimeMillis(),
    val isSuccess: Boolean,
    val isSent: Boolean, // true=发送，false=接收
    val filePath: String? = null,
    val errorMessage: String? = null
) : Serializable

/**
 * 传输状态枚举
 */
enum class TransferStatus {
    PREPARING,      // 准备中
    TRANSFERRING,   // 传输中
    COMPLETED,      // 完成
    FAILED,         // 失败
    CANCELLED       // 取消
}

/**
 * 传输进度信息
 */
data class TransferProgress(
    val status: TransferStatus = TransferStatus.PREPARING,
    val transferredBytes: Long = 0,
    val totalBytes: Long = 0,
    val speed: Double = 0.0, // bytes per second
    val errorMessage: String? = null
) {
    val percentage: Int
        get() = if (totalBytes > 0) ((transferredBytes * 100) / totalBytes).toInt() else 0
}
