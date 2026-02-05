package com.encounter.app.ui.screens.userdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encounter.app.data.repository.ChatRepository
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

/**
 * ユーザー詳細画面のUI状態
 */
data class UserDetailUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val isStartingChat: Boolean = false,
    val error: String? = null
)

/**
 * ユーザー詳細画面のUIイベント（一度きりのイベント）
 */
sealed class UserDetailUiEvent {
    data class NavigateToChat(val roomId: String) : UserDetailUiEvent()
    data class ShowError(val message: String) : UserDetailUiEvent()
}

/**
 * ユーザー詳細画面のViewModel
 * ユーザー詳細表示、チャット開始
 * 
 * 担当: 久米（Backend）
 */
@HiltViewModel
class UserDetailViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UserDetailUiState())
    val uiState: StateFlow<UserDetailUiState> = _uiState.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<UserDetailUiEvent>()
    val uiEvent: SharedFlow<UserDetailUiEvent> = _uiEvent.asSharedFlow()
    
    /** 表示対象のユーザーID */
    private val targetUserId: String = savedStateHandle["userId"] ?: ""
    
    init {
        if (targetUserId.isNotEmpty()) {
            loadUser(targetUserId)
        }
    }
    
    /**
     * ユーザー詳細情報を取得
     */
    fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = userRepository.getUser(userId)
            result.fold(
                onSuccess = { user ->
                    _uiState.update { 
                        it.copy(
                            user = user,
                            isLoading = false,
                            error = null
                        ) 
                    }
                },
                onFailure = { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "ユーザー情報の取得に失敗しました"
                        ) 
                    }
                    _uiEvent.emit(UserDetailUiEvent.ShowError("ユーザー情報の取得に失敗しました"))
                }
            )
        }
    }
    
    /**
     * チャットを開始
     * 既存のチャットルームがあればそれを使用、なければ新規作成
     */
    fun startChat() {
        val targetUser = _uiState.value.user ?: return
        val currentUserId = userRepository.getCurrentUserId() ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isStartingChat = true, error = null) }
            
            val result = chatRepository.getOrCreateChatRoom(currentUserId, targetUser.uid)
            result.fold(
                onSuccess = { chatRoom ->
                    _uiState.update { it.copy(isStartingChat = false) }
                    _uiEvent.emit(UserDetailUiEvent.NavigateToChat(chatRoom.roomId))
                },
                onFailure = { e ->
                    _uiState.update { 
                        it.copy(
                            isStartingChat = false,
                            error = "チャットの開始に失敗しました"
                        ) 
                    }
                    _uiEvent.emit(UserDetailUiEvent.ShowError("チャットの開始に失敗しました"))
                }
            )
        }
    }
    
    /**
     * 情報を再読み込み
     */
    fun refresh() {
        if (targetUserId.isNotEmpty()) {
            loadUser(targetUserId)
        }
    }
    
    /**
     * エラーメッセージをクリア
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
