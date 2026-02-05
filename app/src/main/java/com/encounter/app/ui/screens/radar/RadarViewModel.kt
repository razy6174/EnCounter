package com.encounter.app.ui.screens.radar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encounter.app.ble.BleManager
import com.encounter.app.ble.PermissionState
import com.encounter.app.data.repository.UserRepository
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
 * レーダー画面のUI状態
 */
data class RadarUiState(
    val isBluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val isAdvertising: Boolean = false,
    val permissionState: PermissionState = PermissionState.UNKNOWN,
    val detectedDeviceCount: Int = 0,
    val detectedDevices: Set<String> = emptySet(),
    val errorMessage: String? = null
) {
    /**
     * すれ違い通信がアクティブかどうか
     * スキャンまたはアドバタイズのどちらかが動作中ならtrue
     */
    val isEncounterActive: Boolean
        get() = isScanning || isAdvertising
}

/**
 * レーダー画面のUIイベント（一度きりのイベント）
 */
sealed class RadarUiEvent {
    data object RequestPermissions : RadarUiEvent()
    data object NavigateToSettings : RadarUiEvent()
    data class ShowError(val message: String) : RadarUiEvent()
}

/**
 * レーダー画面のViewModel
 * BLE通信のスキャン・アドバタイズを管理
 * 
 * 担当: 久米（Backend）
 */
@HiltViewModel
class RadarViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadarUiState())
    val uiState: StateFlow<RadarUiState> = _uiState.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<RadarUiEvent>()
    val uiEvent: SharedFlow<RadarUiEvent> = _uiEvent.asSharedFlow()
    
    // 現在のユーザーのuidPrefixをキャッシュ
    private var currentUserUidPrefix: String? = null
    
    init {
        observeBleState()
        observeCurrentUser()
        checkInitialState()
    }
    
    /**
     * 現在のユーザー情報を監視してuidPrefixを取得
     */
    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.observeCurrentUser().collect { user ->
                currentUserUidPrefix = user?.uidPrefix
                Log.d("RadarViewModel", "Current user uidPrefix updated: $currentUserUidPrefix")
            }
        }
    }
    
    /**
     * BLE状態を監視
     */
    private fun observeBleState() {
        // スキャン状態を監視
        viewModelScope.launch {
            bleManager.isScanning.collect { isScanning ->
                _uiState.update { it.copy(isScanning = isScanning) }
            }
        }
        
        // アドバタイズ状態を監視
        viewModelScope.launch {
            bleManager.isAdvertising.collect { isAdvertising ->
                _uiState.update { it.copy(isAdvertising = isAdvertising) }
            }
        }
        
        // 検知デバイスを監視
        viewModelScope.launch {
            bleManager.detectedDevices.collect { devices ->
                _uiState.update { 
                    it.copy(
                        detectedDevices = devices,
                        detectedDeviceCount = devices.size
                    ) 
                }
            }
        }
        
        // 権限状態を監視
        viewModelScope.launch {
            bleManager.permissionState.collect { state ->
                _uiState.update { it.copy(permissionState = state) }
            }
        }
    }
    
    /**
     * 初期状態をチェック
     */
    private fun checkInitialState() {
        _uiState.update { 
            it.copy(
                isBluetoothEnabled = bleManager.isBluetoothEnabled(),
                permissionState = if (bleManager.checkPermissions()) {
                    PermissionState.GRANTED
                } else {
                    PermissionState.DENIED
                }
            )
        }
    }
    
    /**
     * スキャン開始/停止をトグル
     */
    fun toggleScanning() {
        if (!bleManager.checkPermissions()) {
            viewModelScope.launch {
                _uiEvent.emit(RadarUiEvent.RequestPermissions)
            }
            return
        }
        
        if (_uiState.value.isScanning) {
            bleManager.stopScanning()
        } else {
            bleManager.startScanning()
        }
    }
    
    /**
     * アドバタイズ開始/停止をトグル
     */
    fun toggleAdvertising() {
        if (!bleManager.checkPermissions()) {
            viewModelScope.launch {
                _uiEvent.emit(RadarUiEvent.RequestPermissions)
            }
            return
        }
        
        val uidPrefix = currentUserUidPrefix
        if (uidPrefix.isNullOrEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(RadarUiEvent.ShowError("ユーザー情報が取得できません。プロフィールを設定してください。"))
            }
            return
        }
        
        if (_uiState.value.isAdvertising) {
            bleManager.stopAdvertising()
        } else {
            bleManager.startAdvertising(uidPrefix)
        }
    }
    
    /**
     * 権限リクエストの結果を処理
     */
    fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean = false) {
        bleManager.updatePermissionState(granted, permanentlyDenied)
        
        if (!granted && permanentlyDenied) {
            viewModelScope.launch {
                _uiEvent.emit(RadarUiEvent.NavigateToSettings)
            }
        }
    }
    
    /**
     * 必要な権限のリストを取得
     */
    fun getRequiredPermissions(): Array<String> {
        return bleManager.getRequiredPermissions()
    }
    
    /**
     * 検知リストをクリア
     */
    fun clearDetectedDevices() {
        bleManager.clearDetectedDevices()
    }
    
    /**
     * すれ違い通信を開始/停止をトグル
     * スキャンとアドバタイズを同時に制御
     */
    fun toggleEncounter() {
        if (!bleManager.checkPermissions()) {
            viewModelScope.launch {
                _uiEvent.emit(RadarUiEvent.RequestPermissions)
            }
            return
        }
        
        if (_uiState.value.isEncounterActive) {
            bleManager.stopEncounter()
        } else {
            val uidPrefix = currentUserUidPrefix
            if (uidPrefix.isNullOrEmpty()) {
                viewModelScope.launch {
                    _uiEvent.emit(RadarUiEvent.ShowError("ユーザー情報が取得できません。プロフィールを設定してください。"))
                }
                return
            }
            bleManager.startEncounter(uidPrefix)
        }
    }
}
