package com.encounter.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.encounter.app.domain.model.EncounterRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "EncounterHistoryRepo"

/**
 * すれ違い履歴を管理するリポジトリ
 * SharedPreferencesを使用して永続化
 * 
 * 設計意図:
 * - Firebase Firestoreではなくローカル保存を採用
 * - 理由: データ量が多くなる可能性、オフライン動作、コスト最適化
 * - SharedPreferencesで十分な規模（最大1000件程度を想定）
 * 
 * 担当: 久米（Backend）
 */
@Singleton
class EncounterHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "encounter_history"
        private const val KEY_HISTORY = "history_json"
        private const val MAX_HISTORY_SIZE = 1000  // 最大保存件数
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // StateFlowで履歴を公開
    private val _history = MutableStateFlow<List<EncounterRecord>>(emptyList())
    val history: StateFlow<List<EncounterRecord>> = _history.asStateFlow()
    
    init {
        // 初期化時に履歴を読み込み
        _history.value = loadHistory()
        Log.d(TAG, "Initialized with ${_history.value.size} records")
    }
    
    /**
     * 履歴にすれ違い記録を追加
     * 重複は上書き（同じuidPrefixがあれば最新のタイムスタンプで更新）
     * 
     * @param uidPrefix BLEで検知したUID（16文字）
     * @param displayName ユーザー名（キャッシュ用）
     */
    fun addEncounter(uidPrefix: String, displayName: String? = null) {
        val currentHistory = _history.value.toMutableList()
        val now = System.currentTimeMillis()
        
        // 既存のレコードを検索
        val existingIndex = currentHistory.indexOfFirst { it.uidPrefix == uidPrefix }
        
        if (existingIndex >= 0) {
            // 既存レコードを更新（タイムスタンプと名前を最新に）
            val existing = currentHistory[existingIndex]
            currentHistory[existingIndex] = existing.copy(
                timestamp = now,
                displayName = displayName ?: existing.displayName
            )
            Log.d(TAG, "Updated existing record: $uidPrefix -> $displayName")
        } else {
            // 新規レコードを追加
            val newRecord = EncounterRecord(
                uidPrefix = uidPrefix,
                timestamp = now,
                displayName = displayName
            )
            currentHistory.add(0, newRecord)  // 先頭に追加（最新が上）
            Log.d(TAG, "Added new record: $uidPrefix -> $displayName")
        }
        
        // 最大件数を超えた場合は古いものを削除
        val trimmedHistory = if (currentHistory.size > MAX_HISTORY_SIZE) {
            Log.d(TAG, "Trimming history from ${currentHistory.size} to $MAX_HISTORY_SIZE")
            currentHistory
                .sortedByDescending { it.timestamp }
                .take(MAX_HISTORY_SIZE)
        } else {
            currentHistory.sortedByDescending { it.timestamp }
        }
        
        // 保存と通知
        saveHistory(trimmedHistory)
        _history.value = trimmedHistory
    }
    
    /**
     * 履歴から特定のレコードを削除
     * 
     * @param uidPrefix 削除するレコードのuidPrefix
     */
    fun removeEncounter(uidPrefix: String) {
        val currentHistory = _history.value.toMutableList()
        val removed = currentHistory.removeAll { it.uidPrefix == uidPrefix }
        
        if (removed) {
            Log.d(TAG, "Removed record: $uidPrefix")
            saveHistory(currentHistory)
            _history.value = currentHistory
        } else {
            Log.w(TAG, "Record not found for removal: $uidPrefix")
        }
    }
    
    /**
     * 履歴を全削除
     */
    fun clearHistory() {
        Log.d(TAG, "Clearing all history (${_history.value.size} records)")
        saveHistory(emptyList())
        _history.value = emptyList()
    }
    
    /**
     * 履歴を読み込み
     * 
     * @return 保存されていた履歴リスト（タイムスタンプ降順）
     */
    private fun loadHistory(): List<EncounterRecord> {
        return try {
            val json = prefs.getString(KEY_HISTORY, null)
            if (json.isNullOrEmpty()) {
                emptyList()
            } else {
                val jsonArray = JSONArray(json)
                val list = mutableListOf<EncounterRecord>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val displayName = if (obj.has("displayName") && !obj.isNull("displayName")) {
                        obj.getString("displayName")
                    } else {
                        null
                    }
                    list.add(
                        EncounterRecord(
                            uidPrefix = obj.getString("uidPrefix"),
                            timestamp = obj.getLong("timestamp"),
                            displayName = displayName
                        )
                    )
                }
                list.sortedByDescending { it.timestamp }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load history, returning empty list", e)
            emptyList()
        }
    }
    
    /**
     * 履歴を保存
     * 
     * @param history 保存する履歴リスト
     */
    private fun saveHistory(history: List<EncounterRecord>) {
        try {
            val jsonArray = JSONArray()
            for (record in history) {
                val obj = JSONObject().apply {
                    put("uidPrefix", record.uidPrefix)
                    put("timestamp", record.timestamp)
                    if (record.displayName != null) {
                        put("displayName", record.displayName)
                    }
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
            Log.d(TAG, "Saved ${history.size} records")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save history", e)
        }
    }
    
    /**
     * 履歴の件数を取得
     */
    fun getHistoryCount(): Int = _history.value.size
    
    /**
     * 特定のuidPrefixが履歴に存在するか確認
     * 
     * @param uidPrefix 確認するuidPrefix
     * @return 存在する場合true
     */
    fun hasEncounter(uidPrefix: String): Boolean {
        return _history.value.any { it.uidPrefix == uidPrefix }
    }
}
