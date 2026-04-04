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

class DevicesFragment : Fragment() {
    private lateinit var recyclerDevices: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_devices, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            recyclerDevices = view.findViewById(R.id.recycler_devices)
            tvEmpty = view.findViewById(R.id.tv_empty)
            
            recyclerDevices.layoutManager = LinearLayoutManager(context)
            updateEmptyState(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerDevices.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}