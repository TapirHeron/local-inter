package com.example.local_inter.core

import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream

class NasServer(port: Int) : NanoHTTPD(port) {
    private val shareFolder = File("/sdcard/LanShare/")
    var isRunning = false

    init {
        if (!shareFolder.exists()) shareFolder.mkdirs()
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri.trim('/')
        val target = if (uri.isEmpty()) shareFolder else File(shareFolder, uri)

        return when {
            target.isDirectory -> getFolderHtml(target)
            target.isFile -> newChunkedResponse(
                Response.Status.OK, getMimeTypeForFile(target.name), FileInputStream(target)
            )
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "文件不存在")
        }
    }

    private fun getFolderHtml(folder: File): Response {
        val html = StringBuilder()
        html.append("""
            <html>
            <head><meta charset="utf-8"><title>我的随身NAS</title></head>
            <body style="font-size:20px;padding:20px">
            <h2>📁 局域网共享文件夹</h2>
            <a href="/">🏠 根目录</a><br><br>
        """.trimIndent())

        folder.listFiles()?.forEach { file ->
            val name = file.name
            val url = "/$name"
            html.append("<p><a href='$url'>${if(file.isDirectory) "📂" else "📄"} $name</a></p>")
        }

        html.append("</body></html>")
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString())
    }

    fun startServer() {
        if (!isRunning) {
            start()
            isRunning = true
        }
    }

    fun stopServer() {
        stop()
        isRunning = false
    }
}