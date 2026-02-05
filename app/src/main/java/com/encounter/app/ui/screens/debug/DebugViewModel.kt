package com.encounter.app.ui.screens.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encounter.app.debug.DebugHelper
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
 * デバッグ画面のUI状態
 */
data class DebugUiState(
    val isDebugLogEnabled: Boolean = true,
    val isForceDetectionMode: Boolean = false,
    val forceDetectedDevices: Set<String> = emptySet(),
    val isCreatingDummyUsers: Boolean = false,
    val createdDummyUserIds: List<String> = emptyList(),
    val error: String? = null
)

/**
 * デバッグ画面のUIイベント
 */
sealed class DebugUiEvent {
    data class ShowMessage(val message: String) : DebugUiEvent()
    data class ShowError(val message: String) : DebugUiEvent()
}

/**
 * デバッグ画面のViewModel
 * デモ・デバッグ機能を管理
 * 
 * 担当: 久米（Backend）
 */
@HiltViewModel
class DebugViewModel @Inject constructor(
    private val debugHelper: DebugHelper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<DebugUiEvent>()
    val uiEvent: SharedFlow<DebugUiEvent> = _uiEvent.asSharedFlow()
    
    init {
        // DebugHelperの状態を監視
        viewModelScope.launch {
            debugHelper.isForceDetectionMode.collect { isEnabled ->
                _uiState.update { it.copy(isForceDetectionMode = isEnabled) }
            }
        }
        
        viewModelScope.launch {
            debugHelper.forceDetectedDevices.collect { devices ->
                _uiState.update { it.copy(forceDetectedDevices = devices) }
            }
        }
        
        // 初期状態を設定
        _uiState.update { 
            it.copy(isDebugLogEnabled = DebugHelper.isDebugLogEnabled) 
        }
    }
    
    /**
     * デバッグログのON/OFF切り替え
     */
    fun toggleDebugLog() {
        debugHelper.toggleDebugLog()
        _uiState.update { it.copy(isDebugLogEnabled = DebugHelper.isDebugLogEnabled) }
        
        viewModelScope.launch {
            val message = if (DebugHelper.isDebugLogEnabled) {
                "デバッグログを有効にしました"
            } else {
                "デバッグログを無効にしました"
            }
            _uiEvent.emit(DebugUiEvent.ShowMessage(message))
        }
    }
    
    /**
     * 強制検知モードのON/OFF切り替え
     */
    fun toggleForceDetectionMode() {
        debugHelper.toggleForceDetectionMode()
        
        viewModelScope.launch {
            val message = if (_uiState.value.isForceDetectionMode) {
                "強制検知モードを有効にしました（BLE通信なしで検知をシミュレート）"
            } else {
                "強制検知モードを無効にしました"
            }
            _uiEvent.emit(DebugUiEvent.ShowMessage(message))
        }
    }
    
    /**
     * ダミーユーザーを生成
     */
    fun createDummyUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingDummyUsers = true, error = null) }
            
            val result = debugHelper.createDummyUsers()
            
            result.fold(
                onSuccess = { uids ->
                    _uiState.update { 
                        it.copy(
                            isCreatingDummyUsers = false,
                            createdDummyUserIds = uids
                        ) 
                    }
                    _uiEvent.emit(
                        DebugUiEvent.ShowMessage("${uids.size}人のダミーユーザーを作成しました")
                    )
                },
                onFailure = { e ->
                    _uiState.update { 
                        it.copy(
                            isCreatingDummyUsers = false,
                            error = "ダミーユーザーの作成に失敗しました"
                        ) 
                    }
                    _uiEvent.emit(
                        DebugUiEvent.ShowError("ダミーユーザーの作成に失敗: ${e.message}")
                    )
                }
            )
        }
    }
    
    /**
     * 強制検知デバイスを追加
     */
    fun addForceDetectedDevice(uidPrefix: String) {
        if (uidPrefix.length != 16) {
            viewModelScope.launch {
                _uiEvent.emit(DebugUiEvent.ShowError("UIDプレフィックスは16文字である必要があります"))
            }
            return
        }
        
        debugHelper.addForceDetectedDevice(uidPrefix)
        
        viewModelScope.launch {
            _uiEvent.emit(DebugUiEvent.ShowMessage("デバイスを追加しました: $uidPrefix"))
        }
    }
    
    /**
     * 強制検知デバイスを削除
     */
    fun removeForceDetectedDevice(uidPrefix: String) {
        debugHelper.removeForceDetectedDevice(uidPrefix)
        
        viewModelScope.launch {
            _uiEvent.emit(DebugUiEvent.ShowMessage("デバイスを削除しました"))
        }
    }
    
    /**
     * 強制検知デバイスをすべてクリア
     */
    fun clearForceDetectedDevices() {
        debugHelper.clearForceDetectedDevices()
        
        viewModelScope.launch {
            _uiEvent.emit(DebugUiEvent.ShowMessage("すべてのデバイスをクリアしました"))
        }
    }
    
    /**
     * ダミーユーザーのUIDプレフィックスを一括追加
     */
    fun addAllDummyUsers() {
        if (_uiState.value.createdDummyUserIds.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(
                    DebugUiEvent.ShowError("先にダミーユーザーを作成してください")
                )
            }
            return
        }
        
        // 作成済みダミーユーザーのuidPrefixを取得（16文字に切り詰め）
        val uidPrefixes = _uiState.value.createdDummyUserIds.map { it.take(16) }.toSet()
        debugHelper.setForceDetectedDevices(uidPrefixes)
        
        viewModelScope.launch {
            _uiEvent.emit(
                DebugUiEvent.ShowMessage("${uidPrefixes.size}人のダミーユーザーを検知リストに追加しました")
            )
        }
    }
    
    /**
     * エラーをクリア
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
