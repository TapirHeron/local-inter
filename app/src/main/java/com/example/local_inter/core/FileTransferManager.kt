package com.example.local_inter.core

import android.util.Log
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * TCP文件传输管理器
 */
class FileTransferManager {
    companion object {
        private const val TAG = "FileTransferManager"
        const val FILE_RECEIVE_PORT = 8888  // 接收端口
        const val FILE_SEND_PORT = 8889     // 发送端口
        private const val BUFFER_SIZE = 8192
    }
    
    private var serverSocket: ServerSocket? = null
    private var isReceiving = false
    
    // Pending receive information
    private var pendingReceiveSocket: Socket? = null
    private var pendingFileName: String = ""
    private var pendingFileSize: Long = 0
    private var pendingSenderIp: String = ""
    
    /**
     * 接收文件回调
     */
    var onFileReceiveRequest: ((String, Long, String) -> Unit)? = null // fileName, fileSize, senderIp
    var onReceiveProgress: ((TransferProgress) -> Unit)? = null
    var onReceiveComplete: ((Boolean, String?) -> Unit)? = null // success, filePath
    
    /**
     * 发送文件进度回调
     */
    var onSendProgress: ((TransferProgress) -> Unit)? = null
    var onSendComplete: ((Boolean, String?) -> Unit)? = null // success, errorMessage
    
    /**
     * 启动文件接收服务
     */
    fun startReceiveService() {
        if (isReceiving) {
            Log.w(TAG, "接收服务已在运行")
            return
        }
        
        thread(name = "FileReceiveServer") {
            try {
                serverSocket = ServerSocket(FILE_RECEIVE_PORT).apply {
                    soTimeout = 0
                    reuseAddress = true
                }
                isReceiving = true
                Log.d(TAG, "文件接收服务已启动，端口: $FILE_RECEIVE_PORT")
                
                while (isReceiving) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        handleIncomingFile(clientSocket)
                    } catch (e: IOException) {
                        if (isReceiving) {
                            Log.e(TAG, "接受连接失败: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "接收服务异常: ${e.message}")
            } finally {
                isReceiving = false
            }
        }
    }
    
    /**
     * 停止接收服务
     */
    fun stopReceiveService() {
        isReceiving = false
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "关闭接收服务失败: ${e.message}")
        }
        Log.d(TAG, "文件接收服务已停止")
    }
    
    /**
     * 处理 incoming 文件
     */
    private fun handleIncomingFile(clientSocket: Socket) {
        thread(name = "FileReceiver") {
            var inputStream: InputStream? = null
            
            try {
                Log.d(TAG, "新连接来自: ${clientSocket.inetAddress.hostAddress}")
                inputStream = clientSocket.getInputStream()
                
                // 读取文件信息
                val fileName = readString(inputStream)
                val fileSize = readLong(inputStream)
                val senderIp = clientSocket.inetAddress.hostAddress ?: "unknown"
                
                Log.d(TAG, "收到文件请求: $fileName, 大小: $fileSize, 来自: $senderIp")
                
                // 保存socket和文件信息，等待用户确认
                pendingReceiveSocket = clientSocket
                pendingFileName = fileName
                pendingFileSize = fileSize
                pendingSenderIp = senderIp
                
                // 通过回调通知UI
                onFileReceiveRequest?.invoke(fileName, fileSize, senderIp)
                
            } catch (e: Exception) {
                Log.e(TAG, "处理文件请求失败: ${e.message}", e)
                e.printStackTrace()
                try {
                    clientSocket.close()
                } catch (ex: Exception) {}
                onReceiveComplete?.invoke(false, e.message)
            }
        }
    }
    
    /**
     * 接收文件（用户确认后调用）
     */
    fun acceptAndReceiveFile(savePath: String) {
        val socket = pendingReceiveSocket
        if (socket == null || socket.isClosed) {
            onReceiveComplete?.invoke(false, "连接已关闭")
            return
        }
        
        receiveFileInternal(socket, savePath)
        
        // 清空pending
        pendingReceiveSocket = null
        pendingFileName = ""
        pendingFileSize = 0
        pendingSenderIp = ""
    }
    
    /**
     * 拒绝接收文件
     */
    fun rejectFile() {
        try {
            pendingReceiveSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "关闭socket失败: ${e.message}")
        }
        pendingReceiveSocket = null
        pendingFileName = ""
        pendingFileSize = 0
        pendingSenderIp = ""
    }
    
    /**
     * 获取待接收文件信息
     */
    fun getPendingFileInfo(): Triple<String, Long, String>? {
        return if (pendingReceiveSocket != null && !pendingReceiveSocket!!.isClosed) {
            Triple(pendingFileName, pendingFileSize, pendingSenderIp)
        } else {
            null
        }
    }
    
    /**
     * 内部接收文件方法
     */
    private fun receiveFileInternal(clientSocket: Socket, savePath: String) {
        thread(name = "FileReceiver") {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            
            try {
                inputStream = clientSocket.getInputStream()
                
                // 注意：文件信息（fileName, fileSize）已在 handleIncomingFile 中读取
                // 这里直接开始接收文件数据
                
                Log.d(TAG, "开始接收文件: $pendingFileName, 大小: $pendingFileSize")
                
                // 创建保存目录
                val saveFile = File(savePath)
                val saveDir = saveFile.parentFile
                if (saveDir != null && !saveDir.exists()) {
                    val created = saveDir.mkdirs()
                    Log.d(TAG, "创建目录: ${saveDir.absolutePath}, 结果: $created")
                }
                
                outputStream = FileOutputStream(savePath)
                
                // 接收文件数据
                val buffer = ByteArray(BUFFER_SIZE)
                var totalReceived = 0L
                var bytesRead: Int
                val startTime = System.currentTimeMillis()
                
                onReceiveProgress?.invoke(
                    TransferProgress(
                        status = TransferStatus.TRANSFERRING,
                        transferredBytes = 0,
                        totalBytes = pendingFileSize
                    )
                )
                
                while (totalReceived < pendingFileSize) {
                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    
                    outputStream.write(buffer, 0, bytesRead)
                    totalReceived += bytesRead
                    
                    // 计算速度
                    val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                    val speed = if (elapsedTime > 0) totalReceived / elapsedTime else 0.0
                    
                    onReceiveProgress?.invoke(
                        TransferProgress(
                            status = TransferStatus.TRANSFERRING,
                            transferredBytes = totalReceived,
                            totalBytes = pendingFileSize,
                            speed = speed
                        )
                    )
                }
                
                outputStream.flush()
                Log.d(TAG, "文件接收完成: $savePath")
                
                onReceiveProgress?.invoke(
                    TransferProgress(
                        status = TransferStatus.COMPLETED,
                        transferredBytes = totalReceived,
                        totalBytes = pendingFileSize
                    )
                )
                
                onReceiveComplete?.invoke(true, savePath)
                
            } catch (e: Exception) {
                Log.e(TAG, "接收文件失败: ${e.message}")
                onReceiveProgress?.invoke(
                    TransferProgress(
                        status = TransferStatus.FAILED,
                        errorMessage = e.message
                    )
                )
                onReceiveComplete?.invoke(false, e.message)
            } finally {
                try {
                    inputStream?.close()
                    outputStream?.close()
                    clientSocket.close()
                } catch (e: Exception) {
                    Log.e(TAG, "关闭资源失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 发送文件到远程设备
     */
    fun sendFile(filePath: String, remoteIp: String, remotePort: Int = FILE_RECEIVE_PORT) {
        thread(name = "FileSender") {
            var socket: Socket? = null
            var outputStream: OutputStream? = null
            var inputStream: FileInputStream? = null
            
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    throw FileNotFoundException("文件不存在: $filePath")
                }
                
                Log.d(TAG, "开始发送文件: ${file.name}, 大小: ${file.length()}, 目标: $remoteIp:$remotePort")
                Log.d(TAG, "文件绝对路径: ${file.absolutePath}")
                
                // 建立连接
                socket = Socket()
                socket.soTimeout = 30000 // 30秒超时
                socket.reuseAddress = true
                socket.connect(InetSocketAddress(remoteIp, remotePort), 10000)
                Log.d(TAG, "已连接到: $remoteIp:$remotePort")
                
                outputStream = socket.getOutputStream()
                
                // 发送文件信息
                val fileName = file.name
                val fileSize = file.length()
                Log.d(TAG, "准备发送文件名: '$fileName', 大小: $fileSize")
                
                if (fileName.isEmpty()) {
                    throw Exception("文件名为空")
                }
                
                writeString(outputStream, fileName)
                writeLong(outputStream, fileSize)
                Log.d(TAG, "已发送文件头信息")
                outputStream.flush()
                
                // 发送文件数据
                inputStream = FileInputStream(file)
                val buffer = ByteArray(BUFFER_SIZE)
                var totalSent = 0L
                var bytesRead: Int
                val startTime = System.currentTimeMillis()
                
                onSendProgress?.invoke(
                    TransferProgress(
                        status = TransferStatus.TRANSFERRING,
                        transferredBytes = 0,
                        totalBytes = file.length()
                    )
                )
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalSent += bytesRead
                    
                    // 每发送1MB输出一次日志
                    if (totalSent % (1024 * 1024) == 0L) {
                        Log.d(TAG, "已发送: ${totalSent / 1024}KB")
                    }
                    
                    // 计算速度
                    val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                    val speed = if (elapsedTime > 0) totalSent / elapsedTime else 0.0
                    
                    onSendProgress?.invoke(
                        TransferProgress(
                            status = TransferStatus.TRANSFERRING,
                            transferredBytes = totalSent,
                            totalBytes = file.length(),
                            speed = speed
                        )
                    )
                }
                
                outputStream.flush()
                Log.d(TAG, "文件发送完成: ${file.name}, 共发送: ${totalSent}字节")
                
                onSendProgress?.invoke(
                    TransferProgress(
                        status = TransferStatus.COMPLETED,
                        transferredBytes = totalSent,
                        totalBytes = file.length()
                    )
                )
                
                onSendComplete?.invoke(true, null)
                
            } catch (e: Exception) {
                Log.e(TAG, "发送文件失败: ${e.message}", e)
                e.printStackTrace()
                onSendProgress?.invoke(
                    TransferProgress(
                        status = TransferStatus.FAILED,
                        errorMessage = e.message
                    )
                )
                onSendComplete?.invoke(false, e.message)
            } finally {
                try {
                    inputStream?.close()
                    outputStream?.close()
                    socket?.close()
                    Log.d(TAG, "资源已释放")
                } catch (e: Exception) {
                    Log.e(TAG, "关闭资源失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 写入字符串（先写长度，再写内容）
     */
    private fun writeString(outputStream: OutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        writeLong(outputStream, bytes.size.toLong())
        outputStream.write(bytes)
    }
    
    /**
     * 读取字符串
     */
    private fun readString(inputStream: InputStream): String {
        val length = readLong(inputStream).toInt()
        val bytes = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val bytesRead = inputStream.read(bytes, offset, length - offset)
            if (bytesRead == -1) throw EOFException("读取字符串失败")
            offset += bytesRead
        }
        return String(bytes, Charsets.UTF_8)
    }
    
    /**
     * 写入长整型
     */
    private fun writeLong(outputStream: OutputStream, value: Long) {
        val buffer = ByteArray(8)
        for (i in 7 downTo 0) {
            buffer[i] = (value and (0xFFL shl (i * 8))).toByte()
        }
        outputStream.write(buffer)
    }
    
    /**
     * 读取长整型
     */
    private fun readLong(inputStream: InputStream): Long {
        val bytes = ByteArray(8)
        var offset = 0
        while (offset < 8) {
            val bytesRead = inputStream.read(bytes, offset, 8 - offset)
            if (bytesRead == -1) throw EOFException("读取长整型失败")
            offset += bytesRead
        }
        var value = 0L
        for (i in 0 until 8) {
            value = value or ((bytes[i].toLong() and 0xFF) shl ((7 - i) * 8))
        }
        return value
    }
}
