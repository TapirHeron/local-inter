package com.example.local_inter.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.local_inter.R
import java.io.File

class FilesFragment : Fragment() {
    private lateinit var tvPath: TextView
    private lateinit var recyclerFiles: RecyclerView
    private lateinit var tvEmpty: TextView

    private val shareFolder = File("/sdcard/LanShare/")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_files, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            tvPath = view.findViewById(R.id.tv_path)
            recyclerFiles = view.findViewById(R.id.recycler_files)
            tvEmpty = view.findViewById(R.id.tv_empty)
            
            tvPath.text = shareFolder.absolutePath
            recyclerFiles.layoutManager = LinearLayoutManager(context)
            
            loadFiles()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFiles() {
        if (!shareFolder.exists()) {
            shareFolder.mkdirs()
        }
        
        val files = shareFolder.listFiles()
        val isEmpty = files.isNullOrEmpty()
        
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerFiles.visibility = if (isEmpty) View.GONE else View.VISIBLE
        
        // TODO: 实现文件列表适配器
    }
}