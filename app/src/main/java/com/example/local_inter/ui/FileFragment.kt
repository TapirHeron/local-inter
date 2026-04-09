package com.example.local_inter.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.local_inter.MainActivity
import com.example.local_inter.R
import com.example.local_inter.core.FileEncryptor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.util.*

class FilesFragment : Fragment() {
    private lateinit var tvPath: TextView
    private lateinit var recyclerFiles: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnAddFile: ImageButton
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val shareFolder by lazy {
        // 使用正确的外部存储目录
        val externalDir = requireContext().getExternalFilesDir(null)
        File(externalDir, "LanShare").apply {
            if (!exists()) mkdirs()
        }
    }
    private val fileList = mutableListOf<File>()
    private lateinit var adapter: FileAdapter
    
    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            copyFileToShareFolder(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_files, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tvPath = view.findViewById(R.id.tv_path)
        recyclerFiles = view.findViewById(R.id.recycler_files)
        tvEmpty = view.findViewById(R.id.tv_empty)
        btnAddFile = view.findViewById(R.id.btn_add_file)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        
        tvPath.text = shareFolder.absolutePath
        recyclerFiles.layoutManager = LinearLayoutManager(context)
        adapter = FileAdapter(fileList)
        recyclerFiles.adapter = adapter
        
        // 下拉刷新
        swipeRefresh.setOnRefreshListener {
            loadFiles()
            swipeRefresh.isRefreshing = false
        }
        
        // 添加文件按钮
        btnAddFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }
        
        loadFiles()
    }
    
    override fun onResume() {
        super.onResume()
        loadFiles()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // 清理缓存中的解密文件
        try {
            val cacheDir = requireContext().cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("dec_")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun copyFileToShareFolder(uri: Uri) {
        var inputStream: java.io.InputStream? = null
        try {
            inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Toast.makeText(context, "无法读取文件", Toast.LENGTH_SHORT).show()
                return
            }
            
            val fileName = getFileNameFromUri(uri) ?: "file_${System.currentTimeMillis()}"
            
            // 检查是否启用加密
            val activity = requireActivity() as? MainActivity
            val securityGuard = activity?.securityGuard
            val shouldEncrypt = securityGuard?.isEncryptionEnabled == true && securityGuard.autoEncrypt
            
            val destFile = if (shouldEncrypt) {
                // 加密文件，添加 .enc 扩展名
                File(shareFolder, "$fileName.enc")
            } else {
                File(shareFolder, fileName)
            }
            
            if (shouldEncrypt && securityGuard != null) {
                // 加密保存
                val tempFile = File(requireContext().cacheDir, "temp_${System.currentTimeMillis()}")
                FileOutputStream(tempFile).use { output ->
                    inputStream.copyTo(output)
                }
                
                val key = securityGuard.getOrCreateEncryptionKey()
                FileEncryptor.encryptFile(tempFile, destFile, key)
                tempFile.delete()
                
                Toast.makeText(context, "文件已加密添加", Toast.LENGTH_SHORT).show()
            } else {
                // 普通保存
                FileOutputStream(destFile).use { output ->
                    inputStream.copyTo(output)
                }
                Toast.makeText(context, "文件已添加", Toast.LENGTH_SHORT).show()
            }
            
            loadFiles()
        } catch (e: Exception) {
            Toast.makeText(context, "添加失败: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
    }
    
    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        return it.getString(displayNameIndex)
                    }
                }
            }
            uri.lastPathSegment
        } catch (e: Exception) {
            null
        }
    }

    private fun loadFiles() {
        if (!shareFolder.exists()) {
            shareFolder.mkdirs()
        }
        
        fileList.clear()
        val files = shareFolder.listFiles()
        if (!files.isNullOrEmpty()) {
            fileList.addAll(files)
        }
        
        adapter.notifyDataSetChanged()
        updateEmptyState(fileList.isEmpty())
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerFiles.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${String.format("%.2f", size / 1024.0)} KB"
            size < 1024 * 1024 * 1024 -> "${String.format("%.2f", size / (1024.0 * 1024.0))} MB"
            else -> "${String.format("%.2f", size / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
    
    // 文件列表适配器
    private inner class FileAdapter(private val files: List<File>) :
        RecyclerView.Adapter<FileAdapter.FileViewHolder>() {
        
        inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.iv_icon)
            val tvName: TextView = view.findViewById(R.id.tv_file_name)
            val tvSize: TextView = view.findViewById(R.id.tv_file_size)
            val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
            val btnShare: ImageButton = view.findViewById(R.id.btn_share)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_file, parent, false)
            return FileViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            val file = files[position]
            holder.tvName.text = file.name
            holder.tvSize.text = formatFileSize(file.length())
            
            // 设置图标
            holder.ivIcon.setImageResource(
                if (file.isDirectory) android.R.drawable.ic_menu_agenda
                else android.R.drawable.ic_menu_save
            )
            
            // 点击文件打开
            holder.itemView.setOnClickListener {
                openFile(file)
            }
            
            // 删除按钮
            holder.btnDelete.setOnClickListener {
                showDeleteConfirm(file, position)
            }
            
            // 分享按钮
            holder.btnShare.setOnClickListener {
                shareFile(file)
            }
        }
        
        override fun getItemCount() = files.size
    }
    
    private fun openFile(file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show()
                return
            }
            
            val activity = requireActivity() as? MainActivity
            val securityGuard = activity?.securityGuard
            
            // 如果是加密文件，先解密
            val fileToOpen = if (file.name.endsWith(".enc") && securityGuard != null) {
                val decryptedFile = File(requireContext().cacheDir, "dec_${file.nameWithoutExtension}")
                val key = securityGuard.getOrCreateEncryptionKey()
                FileEncryptor.decryptFile(file, decryptedFile, key)
                decryptedFile
            } else {
                file
            }
            
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                // Android 7.0+ 使用 FileProvider
                androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    fileToOpen
                )
            } else {
                Uri.fromFile(fileToOpen)
            }
            
            val intent = Intent(Intent.ACTION_VIEW)
            val mimeType = getMimeType(fileToOpen)
            intent.setDataAndType(uri, mimeType)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开此文件: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "jpg", "jpeg", "png", "gif", "bmp" -> "image/*"
            "mp4", "avi", "mkv", "mov" -> "video/*"
            "mp3", "wav", "flac" -> "audio/*"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            else -> "*/*"
        }
    }
    
    private fun shareFile(file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show()
                return
            }
            
            val activity = requireActivity() as? MainActivity
            val securityGuard = activity?.securityGuard
            
            // 如果是加密文件，先解密
            val fileToShare = if (file.name.endsWith(".enc") && securityGuard != null) {
                val decryptedFile = File(requireContext().cacheDir, "dec_${file.nameWithoutExtension}")
                val key = securityGuard.getOrCreateEncryptionKey()
                FileEncryptor.decryptFile(file, decryptedFile, key)
                decryptedFile
            } else {
                file
            }
            
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    fileToShare
                )
            } else {
                Uri.fromFile(fileToShare)
            }
            
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = getMimeType(fileToShare)
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "分享文件"))
        } catch (e: Exception) {
            Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun showDeleteConfirm(file: File, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("确定要删除 ${file.name} 吗？")
            .setPositiveButton("删除") { _, _ ->
                if (file.delete()) {
                    fileList.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    updateEmptyState(fileList.isEmpty())
                    Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}