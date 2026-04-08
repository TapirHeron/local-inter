package com.example.local_inter.core

import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import javax.crypto.SecretKey

class NasServer(
    port: Int,
    private val securityGuard: SecurityGuard? = null
) : NanoHTTPD(port) {
    private val shareFolder = File("/sdcard/LanShare/")
    var isRunning = false
    
    // 加密文件夹（存储加密后的文件）
    private val encryptedFolder = File("/sdcard/LanShare/.encrypted/")

    init {
        if (!shareFolder.exists()) shareFolder.mkdirs()
        if (!encryptedFolder.exists()) encryptedFolder.mkdirs()
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri.trim('/')
        val params = session.parameters
        
        // 检查是否请求加密传输
        val requestEncryption = params["encrypt"]?.firstOrNull() == "true"
        val useEncryption = (securityGuard?.isEncryptionEnabled == true) || requestEncryption
        
        val target = if (uri.isEmpty()) shareFolder else File(shareFolder, uri)

        return when {
            target.isDirectory -> getFolderHtml(target, useEncryption)
            target.isFile -> {
                if (useEncryption) {
                    serveEncryptedFile(target, securityGuard?.getOrCreateEncryptionKey())
                } else {
                    serveUnencryptedFile(target)
                }
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "文件不存在")
        }
    }
    
    /**
     * 提供未加密的文件
     */
    private fun serveUnencryptedFile(file: File): Response {
        return newChunkedResponse(
            Response.Status.OK,
            getMimeTypeForFile(file.name),
            FileInputStream(file)
        )
    }
    
    /**
     * 提供加密的文件（实时加密传输）
     */
    private fun serveEncryptedFile(file: File, key: SecretKey?): Response {
        if (key == null) {
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/plain",
                "加密密钥未配置"
            )
        }
        
        return try {
            // 读取文件并加密
            val outputStream = ByteArrayOutputStream()
            FileInputStream(file).use { fis ->
                FileEncryptor.encryptStream(fis, outputStream, key)
            }
            
            val encryptedData = outputStream.toByteArray()
            newFixedLengthResponse(
                Response.Status.OK,
                "application/octet-stream",
                ByteArrayInputStream(encryptedData),
                encryptedData.size.toLong()
            ).apply {
                addHeader("X-Encrypted", "true")
                addHeader("X-Original-Filename", file.name)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/plain",
                "加密失败: ${e.message}"
            )
        }
    }

    private fun getFolderHtml(folder: File, encryptionEnabled: Boolean): Response {
        val html = StringBuilder()
        val encStatus = if (encryptionEnabled) "🔒 已启用" else "🔓 未启用"
        html.append("""
            <html>
            <head><meta charset="utf-8"><title>我的随身NAS</title></head>
            <body style="font-size:20px;padding:20px">
            <h2>📁 局域网共享文件夹</h2>
            <p>加密状态: $encStatus</p>
            <a href="/">🏠 根目录</a><br><br>
        """.trimIndent())

        folder.listFiles()?.forEach { file ->
            val name = file.name
            val url = "/$name"
            val encUrl = "/$name?encrypt=true"
            val icon = if(file.isDirectory) "📂" else "📄"
            html.append("<p><a href='$url'>$icon $name</a> ")
            if (file.isFile) {
                html.append("<a href='$encUrl' style='color:blue'>[下载加密版]</a>")
            }
            html.append("</p>")
        }

        html.append("</body></html>")
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString())
    }

    fun startServer() {
        if (!isRunning) {
            try {
                start(SOCKET_READ_TIMEOUT, false)
                isRunning = true
            } catch (e: Exception) {
                e.printStackTrace()
                isRunning = false
            }
        }
    }

    fun stopServer() {
        if (isRunning) {
            try {
                stop()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRunning = false
            }
        }
    }
}