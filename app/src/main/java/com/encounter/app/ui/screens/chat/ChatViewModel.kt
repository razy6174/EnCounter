package com.encounter.app.ui.screens.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encounter.app.data.repository.ChatRepository
import com.encounter.app.data.repository.UserRepository
import com.encounter.app.domain.model.Message
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

private const val TAG = "ChatViewModel"

/**
 * チャット画面のUI状態
 */
data class ChatUiState(
    val isLoading: Boolean = false,
    val roomId: String = "",
    val messages: List<Message> = emptyList(),
    val partnerUser: User? = null,
    val inputText: String = "",
    val isSending: Boolean = false,
    val myUserId: String = "",
    val error: String? = null
)

/**
 * チャット画面のUIイベント（一度きりのイベント）
 */
sealed class ChatUiEvent {
    data class ShowError(val message: String) : ChatUiEvent()
    data object ScrollToBottom : ChatUiEvent()
}

/**
 * チャット画面のViewModel
 * リアルタイムメッセージ表示・送信
 * 
 * 担当: 久米（Backend）
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<ChatUiEvent>()
    val uiEvent: SharedFlow<ChatUiEvent> = _uiEvent.asSharedFlow()
    
    /** チャットルームID */
    private val roomId: String = savedStateHandle["roomId"] ?: ""
    
    init {
        Log.d(TAG, "ChatViewModel initialized with roomId: $roomId")
        if (roomId.isNotEmpty()) {
            initChat()
        } else {
            Log.e(TAG, "roomId is empty!")
        }
    }
    
    /**
     * チャット初期化
     * 自分のUID取得、相手のユーザー情報取得、メッセージ監視開始
     */
    private fun initChat() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, roomId = roomId, error = null) }
            
            // 自分のUIDを取得
            val myUserId = userRepository.getCurrentUserId()
            if (myUserId == null) {
                Log.e(TAG, "Current user ID is null")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "ユーザー情報の取得に失敗しました"
                    ) 
                }
                _uiEvent.emit(ChatUiEvent.ShowError("ユーザー情報の取得に失敗しました"))
                return@launch
            }
            
            Log.d(TAG, "Current user ID: $myUserId")
            _uiState.update { it.copy(myUserId = myUserId) }
            
            // チャットルーム情報を取得して相手のユーザー情報を読み込む
            loadPartnerUser(myUserId)
            
            // メッセージの監視を開始
            observeMessages()
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    /**
     * 相手のユーザー情報を取得
     */
    private fun loadPartnerUser(myUserId: String) {
        viewModelScope.launch {
            try {
                val roomResult = chatRepository.getChatRoom(roomId)
                roomResult.fold(
                    onSuccess = { chatRoom ->
                        // 参加者から自分以外のユーザーIDを取得
                        val partnerUserId = chatRoom.participants.firstOrNull { it != myUserId }
                        
                        if (partnerUserId == null) {
                            Log.e(TAG, "Partner user not found in chatRoom participants")
                            return@launch
                        }
                        
                        Log.d(TAG, "Partner user ID: $partnerUserId")
                        
                        // 相手のユーザー情報を取得
                        val userResult = userRepository.getUser(partnerUserId)
                        userResult.fold(
                            onSuccess = { user ->
                                Log.d(TAG, "Partner user loaded: ${user.displayName}")
                                _uiState.update { it.copy(partnerUser = user) }
                            },
                            onFailure = { e ->
                                Log.e(TAG, "Failed to load partner user", e)
                            }
                        )
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Failed to get chat room", e)
                        _uiState.update { 
                            it.copy(
                                error = "チャットルーム情報の取得に失敗しました"
                            ) 
                        }
                        _uiEvent.emit(ChatUiEvent.ShowError("チャットルーム情報の取得に失敗しました"))
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadPartnerUser", e)
            }
        }
    }
    
    /**
     * メッセージをリアルタイムで監視
     */
    private fun observeMessages() {
        viewModelScope.launch {
            try {
                chatRepository.observeMessages(roomId).collect { messages ->
                    Log.d(TAG, "Messages updated: ${messages.size} messages")
                    
                    val hadMessages = _uiState.value.messages.isNotEmpty()
                    _uiState.update { it.copy(messages = messages) }
                    
                    // 新しいメッセージが追加されたら下にスクロール
                    if (hadMessages && messages.size > _uiState.value.messages.size) {
                        _uiEvent.emit(ChatUiEvent.ScrollToBottom)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to observe messages", e)
                _uiState.update { 
                    it.copy(error = "メッセージの取得に失敗しました") 
                }
                _uiEvent.emit(ChatUiEvent.ShowError("メッセージの取得に失敗しました"))
            }
        }
    }
    
    /**
     * 入力テキストを更新
     */
    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }
    
    /**
     * メッセージを送信
     */
    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) {
            Log.w(TAG, "Cannot send empty message")
            return
        }
        
        val myUserId = _uiState.value.myUserId
        if (myUserId.isEmpty()) {
            Log.e(TAG, "myUserId is empty, cannot send message")
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            
            Log.d(TAG, "Sending message: $text")
            val result = chatRepository.sendMessage(roomId, myUserId, text)
            
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Message sent successfully")
                    // 送信成功したら入力欄をクリア
                    _uiState.update { 
                        it.copy(
                            inputText = "",
                            isSending = false
                        ) 
                    }
                    // 送信後にスクロール
                    _uiEvent.emit(ChatUiEvent.ScrollToBottom)
                },
                onFailure = { e ->
                    Log.e(TAG, "Failed to send message", e)
                    _uiState.update { 
                        it.copy(
                            isSending = false,
                            error = "メッセージの送信に失敗しました"
                        ) 
                    }
                    _uiEvent.emit(ChatUiEvent.ShowError("メッセージの送信に失敗しました"))
                }
            )
        }
    }
    
    /**
     * エラーメッセージをクリア
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
