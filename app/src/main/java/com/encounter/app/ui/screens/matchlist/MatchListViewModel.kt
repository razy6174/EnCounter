package com.encounter.app.ui.screens.matchlist

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encounter.app.data.repository.UserRepository
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
 * マッチリスト画面のUI状態
 */
data class MatchListUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val detectedUids: Set<String> = emptySet(),
    val error: String? = null
)

/**
 * マッチリスト画面のUIイベント（一度きりのイベント）
 */
sealed class MatchListUiEvent {
    data class NavigateToUserDetail(val userId: String) : MatchListUiEvent()
    data class ShowError(val message: String) : MatchListUiEvent()
}

/**
 * マッチリスト画面のViewModel
 * BLEで検知したUIDからユーザー情報を取得・表示
 * 
 * 担当: 久米（Backend）
 */
@HiltViewModel
class MatchListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MatchListUiState())
    val uiState: StateFlow<MatchListUiState> = _uiState.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<MatchListUiEvent>()
    val uiEvent: SharedFlow<MatchListUiEvent> = _uiEvent.asSharedFlow()
    
    init {
        // Navigation引数から検知UIDリストを取得
        val detectedUidsArg: String? = savedStateHandle["detectedUids"]
        Log.d(TAG, "Received detectedUids arg: $detectedUidsArg")
        
        if (!detectedUidsArg.isNullOrEmpty()) {
            val uids = detectedUidsArg.split(",").filter { it.isNotBlank() }.toSet()
            Log.d(TAG, "Parsed UIDs (${uids.size}件): $uids")
            _uiState.update { it.copy(detectedUids = uids) }
            loadMatchedUsers(uids.toList())
        } else {
            Log.w(TAG, "detectedUidsArg is null or empty")
        }
    }
    
    /**
     * 検知UIDリストを設定してユーザー情報を読み込む
     * RadarViewModelからの連携用
     */
    fun setDetectedUids(uids: Set<String>) {
        _uiState.update { it.copy(detectedUids = uids) }
        loadMatchedUsers(uids.toList())
    }
    
    /**
     * 検知UIDからユーザー情報を取得
     */
    private fun loadMatchedUsers(uids: List<String>) {
        Log.d(TAG, "loadMatchedUsers called with ${uids.size} UIDs: $uids")
        
        if (uids.isEmpty()) {
            Log.w(TAG, "UIDs list is empty, showing empty list")
            _uiState.update { it.copy(users = emptyList(), isLoading = false) }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                userRepository.getUsersByIds(uids).collect { users ->
                    Log.d(TAG, "Fetched ${users.size} users from repository")
                    users.forEach { user ->
                        Log.d(TAG, "  - ${user.uid}: ${user.displayName}")
                    }
                    _uiState.update { 
                        it.copy(
                            users = users,
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
        loadMatchedUsers(_uiState.value.detectedUids.toList())
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
     * エラーメッセージをクリア
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
