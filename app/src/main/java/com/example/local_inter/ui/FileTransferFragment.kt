package com.example.local_inter.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.local_inter.MainActivity
import com.example.local_inter.R
import com.example.local_inter.core.FileTransferManager
import com.example.local_inter.core.TransferProgress
import com.example.local_inter.core.TransferStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileTransferFragment : Fragment() {
    private lateinit var tvDeviceName: TextView
    private lateinit var tvDeviceIp: TextView
    private lateinit var btnSelectFile: Button
    private lateinit var tvFileName: TextView
    private lateinit var tvFileSize: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnCancel: Button
    
    private var selectedFileUri: Uri? = null
    private var selectedFilePath: String? = null
    private var deviceName: String = ""
    private var deviceIp: String = ""
    
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            handleFileSelection(it)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_file_transfer, container, false)
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化视图
        tvDeviceName = view.findViewById(R.id.tv_device_name)
        tvDeviceIp = view.findViewById(R.id.tv_device_ip)
        btnSelectFile = view.findViewById(R.id.btn_select_file)
        tvFileName = view.findViewById(R.id.tv_file_name)
        tvFileSize = view.findViewById(R.id.tv_file_size)
        tvProgress = view.findViewById(R.id.tv_progress)
        tvSpeed = view.findViewById(R.id.tv_speed)
        progressBar = view.findViewById(R.id.progress_bar)
        btnCancel = view.findViewById(R.id.btn_cancel)
        
        // 获取设备信息
        deviceName = arguments?.getString("device_name") ?: "未知设备"
        deviceIp = arguments?.getString("device_ip") ?: ""
        
        tvDeviceName.text = deviceName
        tvDeviceIp.text = deviceIp
        
        // 选择文件按钮
        btnSelectFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }
        
        // 取消传输按钮
        btnCancel.setOnClickListener {
            showCancelDialog()
        }
        
        // 初始状态
        resetUI()
    }
    
    /**
     * 处理文件选择
     */
    private fun handleFileSelection(uri: Uri) {
        selectedFileUri = uri
        
        try {
            // 获取文件信息
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    
                    if (displayNameIndex != -1 && sizeIndex != -1) {
                        var fileName = it.getString(displayNameIndex)
                        val fileSize = it.getLong(sizeIndex)
                        
                        // 如果文件名为空，使用Uri最后一段
                        if (fileName.isNullOrEmpty()) {
                            fileName = uri.lastPathSegment ?: "unknown_file"
                        }
                        
                        android.util.Log.d("FileTransfer", "文件名: '$fileName', 大小: $fileSize")
                        
                        if (fileName.isEmpty()) {
                            Toast.makeText(context, "无法获取文件名", Toast.LENGTH_SHORT).show()
                            return
                        }
                        
                        tvFileName.text = fileName
                        tvFileSize.text = formatFileSize(fileSize)
                        
                        // 保存临时文件路径
                        val tempFile = File(requireContext().cacheDir, fileName)
                        android.util.Log.d("FileTransfer", "临时文件路径: ${tempFile.absolutePath}")
                        
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        if (inputStream == null) {
                            Toast.makeText(context, "无法读取文件", Toast.LENGTH_SHORT).show()
                            return
                        }
                        
                        inputStream.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        selectedFilePath = tempFile.absolutePath
                        android.util.Log.d("FileTransfer", "文件复制完成，路径: $selectedFilePath")
                        
                        // 显示确认对话框
                        showSendConfirmDialog()
                    }
                }
            }
            
        } catch (e: Exception) {
            Toast.makeText(context, "文件读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    /**
     * 显示发送确认对话框
     */
    private fun showSendConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("确认发送")
            .setMessage("确定要发送文件到 $deviceName 吗？")
            .setPositiveButton("发送") { _, _ ->
                startFileTransfer()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 开始文件传输
     */
    private fun startFileTransfer() {
        val filePath = selectedFilePath
        if (filePath.isNullOrEmpty()) {
            Toast.makeText(context, "文件路径无效", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 更新UI为传输中状态
        updateUIForTransferring()
        
        // 获取Activity的引用
        val activity = requireActivity() as? FileTransferActivity
        if (activity == null) {
            Toast.makeText(context, "错误：无法获取活动", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 设置传输回调
        activity.fileTransferManager.onSendProgress = { progress ->
            activity.runOnUiThread {
                updateTransferProgress(progress)
            }
        }
        
        activity.fileTransferManager.onSendComplete = { success, errorMessage ->
            activity.runOnUiThread {
                transferComplete(success, errorMessage, true)
            }
        }
        
        // 开始发送
        activity.fileTransferManager.sendFile(filePath, deviceIp)
    }
    
    /**
     * 更新传输进度
     */
    private fun updateTransferProgress(progress: TransferProgress) {
        when (progress.status) {
            TransferStatus.TRANSFERRING -> {
                progressBar.progress = progress.percentage
                tvProgress.text = "${progress.percentage}%"
                tvSpeed.text = "${formatSpeed(progress.speed)}"
            }
            TransferStatus.COMPLETED -> {
                progressBar.progress = 100
                tvProgress.text = "100%"
                tvSpeed.text = "传输完成"
            }
            TransferStatus.FAILED -> {
                tvSpeed.text = "传输失败"
            }
            else -> {}
        }
    }
    
    /**
     * 传输完成
     */
    private fun transferComplete(success: Boolean, errorMessage: String?, isSent: Boolean) {
        val activity = requireActivity() as? FileTransferActivity
        if (activity == null) {
            Toast.makeText(context, "错误：无法获取活动", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 添加历史记录
        val record = com.example.local_inter.core.TransferRecord(
            fileName = tvFileName.text.toString(),
            fileSize = parseFileSize(tvFileSize.text.toString()),
            deviceName = deviceName,
            deviceIp = deviceIp,
            isSuccess = success,
            isSent = isSent,
            filePath = selectedFilePath,
            errorMessage = errorMessage
        )
        activity.transferHistoryManager.addRecord(record)
        
        if (success) {
            AlertDialog.Builder(requireContext())
                .setTitle("传输成功")
                .setMessage("文件已成功${if (isSent) "发送" else "接收"}")
                .setPositiveButton("确定") { _, _ ->
                    resetUI()
                }
                .show()
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle("传输失败")
                .setMessage(errorMessage ?: "未知错误")
                .setPositiveButton("确定") { _, _ ->
                    resetUI()
                }
                .show()
        }
    }
    
    /**
     * 更新UI为传输中状态
     */
    private fun updateUIForTransferring() {
        btnSelectFile.isEnabled = false
        btnCancel.visibility = View.VISIBLE
        progressBar.progress = 0
        tvProgress.text = "0%"
        tvSpeed.text = "准备中..."
    }
    
    /**
     * 重置UI
     */
    private fun resetUI() {
        btnSelectFile.isEnabled = true
        btnCancel.visibility = View.GONE
        tvFileName.text = "未选择文件"
        tvFileSize.text = ""
        tvProgress.text = ""
        tvSpeed.text = ""
        progressBar.progress = 0
        selectedFileUri = null
        selectedFilePath = null
    }
    
    /**
     * 显示取消对话框
     */
    private fun showCancelDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("取消传输")
            .setMessage("确定要取消当前传输吗？")
            .setPositiveButton("取消") { _, _ ->
                resetUI()
            }
            .setNegativeButton("继续", null)
            .show()
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${String.format("%.2f", size / 1024.0)} KB"
            size < 1024 * 1024 * 1024 -> "${String.format("%.2f", size / (1024.0 * 1024.0))} MB"
            else -> "${String.format("%.2f", size / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
    
    /**
     * 解析文件大小字符串
     */
    private fun parseFileSize(sizeStr: String): Long {
        return try {
            val parts = sizeStr.split(" ")
            if (parts.size == 2) {
                val value = parts[0].toDouble()
                val unit = parts[1]
                when (unit) {
                    "B" -> value.toLong()
                    "KB" -> (value * 1024).toLong()
                    "MB" -> (value * 1024 * 1024).toLong()
                    "GB" -> (value * 1024 * 1024 * 1024).toLong()
                    else -> 0
                }
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 格式化传输速度
     */
    private fun formatSpeed(speed: Double): String {
        return when {
            speed < 1024 -> "${String.format("%.2f", speed)} B/s"
            speed < 1024 * 1024 -> "${String.format("%.2f", speed / 1024.0)} KB/s"
            speed < 1024 * 1024 * 1024 -> "${String.format("%.2f", speed / (1024.0 * 1024.0))} MB/s"
            else -> "${String.format("%.2f", speed / (1024.0 * 1024.0 * 1024.0))} GB/s"
        }
    }
}
