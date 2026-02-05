package com.encounter.app.ui.screens.matchlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encounter.app.data.repository.EncounterHistoryRepository
import com.encounter.app.data.repository.UserRepository
import com.encounter.app.domain.model.EncounterRecord
import com.encounter.app.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MatchListViewModel"

/**
 * すれちがい図鑑画面のUI状態
 */
data class MatchListUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val filteredUsers: List<User> = emptyList(),
    val encounterRecords: List<EncounterRecord> = emptyList(),
    val showDeleteConfirmDialog: Boolean = false,
    val error: String? = null
)

/**
 * すれちがい図鑑画面のUIイベント（一度きりのイベント）
 */
sealed class MatchListUiEvent {
    data class NavigateToUserDetail(val userId: String) : MatchListUiEvent()
    data class ShowError(val message: String) : MatchListUiEvent()
}

/**
 * すれちがい図鑑画面のViewModel
 * 永続化されたすれ違い履歴からユーザー情報を取得・表示
 * 
 * 担当: 久米（Backend）
 */
@HiltViewModel
class MatchListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val encounterHistoryRepository: EncounterHistoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MatchListUiState())
    val uiState: StateFlow<MatchListUiState> = _uiState.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<MatchListUiEvent>()
    val uiEvent: SharedFlow<MatchListUiEvent> = _uiEvent.asSharedFlow()
    
    init {
        // 履歴リポジトリから読み込み
        observeEncounterHistory()
    }
    
    /**
     * 永続化された履歴を監視
     */
    private fun observeEncounterHistory() {
        viewModelScope.launch {
            encounterHistoryRepository.history.collect { records ->
                Log.d(TAG, "Encounter history updated: ${records.size} records")
                _uiState.update { it.copy(encounterRecords = records) }
                
                // uidPrefixリストを取得してユーザー情報をFirebaseから取得
                if (records.isNotEmpty()) {
                    loadUsersFromHistory(records)
                } else {
                    _uiState.update { 
                        it.copy(
                            users = emptyList(),
                            filteredUsers = emptyList(),
                            isLoading = false
                        )
                    }
                }
            }
        }
    }
    
    /**
     * 履歴からユーザー情報を読み込み
     */
    private fun loadUsersFromHistory(records: List<EncounterRecord>) {
        val uidPrefixes = records.map { it.uidPrefix }
        Log.d(TAG, "Loading users from history: ${uidPrefixes.size} uidPrefixes")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                userRepository.getUsersByIds(uidPrefixes).collect { users ->
                    Log.d(TAG, "Fetched ${users.size} users from repository")
                    
                    // 履歴の順序（最新が上）を維持してソート
                    val sortedUsers = uidPrefixes.mapNotNull { prefix ->
                        users.find { it.uidPrefix == prefix }
                    }
                    
                    _uiState.update { 
                        it.copy(
                            users = sortedUsers,
                            filteredUsers = sortedUsers,
                            isLoading = false,
                            error = null
                        ) 
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch users", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "ユーザー情報の取得に失敗しました"
                    ) 
                }
                _uiEvent.emit(MatchListUiEvent.ShowError("ユーザー情報の取得に失敗しました"))
            }
        }
    }
    
    /**
     * ユーザー情報を最新化
     */
    fun refreshUsers() {
        val records = _uiState.value.encounterRecords
        if (records.isNotEmpty()) {
            loadUsersFromHistory(records)
        }
    }
    
    /**
     * ユーザーカードクリック時の処理
     */
    fun onUserClick(userId: String) {
        viewModelScope.launch {
            _uiEvent.emit(MatchListUiEvent.NavigateToUserDetail(userId))
        }
    }
    
    /**
     * 履歴から単一削除
     */
    fun deleteEncounter(uidPrefix: String) {
        Log.d(TAG, "Deleting encounter: $uidPrefix")
        encounterHistoryRepository.removeEncounter(uidPrefix)
    }
    
    /**
     * 全削除確認ダイアログを表示
     */
    fun showDeleteAllConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }
    
    /**
     * 全削除確認ダイアログを非表示
     */
    fun hideDeleteAllConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }
    
    /**
     * 履歴を全削除
     */
    fun clearAllHistory() {
        Log.d(TAG, "Clearing all history")
        encounterHistoryRepository.clearHistory()
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }
    
    /**
     * エラーメッセージをクリア
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
