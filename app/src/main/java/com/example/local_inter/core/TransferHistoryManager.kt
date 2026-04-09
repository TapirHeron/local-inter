package com.example.local_inter.core

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 传输历史记录管理器
 */
class TransferHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("transfer_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_HISTORY = "history_records"
    }
    
    /**
     * 添加传输记录
     */
    fun addRecord(record: TransferRecord) {
        val records = getRecords().toMutableList()
        records.add(0, record) // 最新的在前面
        
        // 最多保存100条记录
        if (records.size > 100) {
            records.removeAt(records.lastIndex)
        }
        
        saveRecords(records)
    }
    
    /**
     * 获取所有记录
     */
    fun getRecords(): List<TransferRecord> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TransferRecord>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 删除单条记录
     */
    fun deleteRecord(recordId: String) {
        val records = getRecords().toMutableList()
        records.removeAll { it.id == recordId }
        saveRecords(records)
    }
    
    /**
     * 清空所有记录
     */
    fun clearAllRecords() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }
    
    /**
     * 保存记录列表
     */
    private fun saveRecords(records: List<TransferRecord>) {
        val json = gson.toJson(records)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
}
