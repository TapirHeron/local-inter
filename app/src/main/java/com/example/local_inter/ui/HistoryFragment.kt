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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.local_inter.MainActivity
import com.example.local_inter.R
import com.example.local_inter.core.TransferRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {
    private lateinit var recyclerHistory: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnClearAll: ImageButton
    
    private val historyList = mutableListOf<TransferRecord>()
    private lateinit var adapter: HistoryAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_history, container, false)
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerHistory = view.findViewById(R.id.recycler_history)
        tvEmpty = view.findViewById(R.id.tv_empty)
        btnClearAll = view.findViewById(R.id.btn_clear_all)
        
        recyclerHistory.layoutManager = LinearLayoutManager(context)
        adapter = HistoryAdapter(historyList)
        recyclerHistory.adapter = adapter
        
        // 加载历史记录
        loadHistory()
        
        // 清空全部按钮
        btnClearAll.setOnClickListener {
            if (historyList.isNotEmpty()) {
                showClearAllDialog()
            }
        }
        
        updateEmptyState(historyList.isEmpty())
    }
    
    override fun onResume() {
        super.onResume()
        loadHistory()
    }
    
    /**
     * 加载历史记录
     */
    private fun loadHistory() {
        val activity = requireActivity() as MainActivity
        val records = activity.transferHistoryManager.getRecords()
        
        historyList.clear()
        historyList.addAll(records)
        adapter.notifyDataSetChanged()
        updateEmptyState(historyList.isEmpty())
    }
    
    /**
     * 更新空状态显示
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerHistory.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    /**
     * 显示清空全部对话框
     */
    private fun showClearAllDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("清空历史记录")
            .setMessage("确定要删除所有传输记录吗？此操作不可恢复。")
            .setPositiveButton("清空") { _, _ ->
                val activity = requireActivity() as MainActivity
                activity.transferHistoryManager.clearAllRecords()
                loadHistory()
                Toast.makeText(context, "已清空所有记录", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
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
     * 格式化时间
     */
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * 历史记录适配器
     */
    private inner class HistoryAdapter(private val records: List<TransferRecord>) :
        RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
        
        inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.iv_icon)
            val tvFileName: TextView = view.findViewById(R.id.tv_file_name)
            val tvFileSize: TextView = view.findViewById(R.id.tv_file_size)
            val tvDeviceName: TextView = view.findViewById(R.id.tv_device_name)
            val tvTime: TextView = view.findViewById(R.id.tv_time)
            val tvStatus: TextView = view.findViewById(R.id.tv_status)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return HistoryViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val record = records[position]
            
            holder.tvFileName.text = record.fileName
            holder.tvFileSize.text = formatFileSize(record.fileSize)
            holder.tvDeviceName.text = "${if (record.isSent) "发送到" else "接收自"} ${record.deviceName}"
            holder.tvTime.text = formatTime(record.transferTime)
            
            // 设置状态
            if (record.isSuccess) {
                holder.tvStatus.text = "✓ 成功"
                holder.tvStatus.setTextColor(0xFF00C853.toInt())
            } else {
                holder.tvStatus.text = "✗ 失败"
                holder.tvStatus.setTextColor(0xFFFF5722.toInt())
            }
            
            // 设置图标
            holder.ivIcon.setImageResource(
                if (record.isSent) android.R.drawable.ic_menu_send
                else android.R.drawable.ic_menu_save
            )
            
            // 长按删除
            holder.itemView.setOnLongClickListener {
                showDeleteDialog(record, position)
                true
            }
            
            // 点击打开文件
            holder.itemView.setOnClickListener {
                if (record.isSuccess && !record.filePath.isNullOrEmpty()) {
                    openFile(record.filePath!!)
                }
            }
        }
        
        override fun getItemCount() = records.size
    }
    
    /**
     * 显示删除单条记录对话框
     */
    private fun showDeleteDialog(record: TransferRecord, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除记录")
            .setMessage("确定要删除这条传输记录吗？")
            .setPositiveButton("删除") { _, _ ->
                val activity = requireActivity() as MainActivity
                activity.transferHistoryManager.deleteRecord(record.id)
                historyList.removeAt(position)
                adapter.notifyItemRemoved(position)
                updateEmptyState(historyList.isEmpty())
                Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 打开文件
     */
    private fun openFile(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show()
                return
            }
            
            val uri = Uri.fromFile(file)
            val intent = Intent(Intent.ACTION_VIEW)
            val mimeType = getMimeType(file)
            intent.setDataAndType(uri, mimeType)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开文件: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    /**
     * 获取MIME类型
     */
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
}
