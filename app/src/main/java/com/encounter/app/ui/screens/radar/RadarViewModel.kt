package com.encounter.app.ui.screens.radar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encounter.app.ble.BleManager
import com.encounter.app.ble.PermissionState
import com.encounter.app.data.repository.SettingsRepository
import com.encounter.app.data.repository.UserRepository
import com.encounter.app.debug.DebugHelper
import com.encounter.app.domain.model.User
import com.encounter.app.domain.model.UserStatus
import com.encounter.app.domain.model.UserStatus.Companion.isActive
import com.encounter.app.notification.EncounterNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val isStealthMode: Boolean = false,
    val permissionState: PermissionState = PermissionState.UNKNOWN,
    val detectedDeviceCount: Int = 0,
    val detectedDevices: Set<String> = emptySet(),
    val isForceDetectionMode: Boolean = false,
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
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val debugHelper: DebugHelper,
    private val notificationManager: EncounterNotificationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadarUiState())
    val uiState: StateFlow<RadarUiState> = _uiState.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<RadarUiEvent>()
    val uiEvent: SharedFlow<RadarUiEvent> = _uiEvent.asSharedFlow()
    
    // 現在のユーザー情報をキャッシュ（フィルタリング用）
    private var currentUser: User? = null
    private var currentUserUidPrefix: String? = null
    
    init {
        observeBleState()
        observeCurrentUser()
        observeDebugState()
        observeNewDetections()
        observeRawDetections()    // 新規: 未フィルタリングの検知監視
        observeStealthMode()
        checkInitialState()
    }
    
    /**
     * 現在のユーザー情報を監視（フィルタリング用にstatus, tagsも保持）
     * 
     * 担当: 久米（Backend）
     */
    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.observeCurrentUser().collect { user ->
                currentUser = user
                currentUserUidPrefix = user?.uidPrefix
                Log.d("RadarViewModel", "Current user updated: name=${user?.displayName}, status=${user?.status}, tags=${user?.tags}, uidPrefix=$currentUserUidPrefix")
                
                // OFFLINEの場合は発信を停止
                if (user?.status == UserStatus.OFFLINE && _uiState.value.isAdvertising) {
                    bleManager.stopAdvertising()
                    Log.d("RadarViewModel", "Stopped advertising due to OFFLINE status")
                }
            }
        }
    }
    
    /**
     * ステルスモードを監視
     * ステルスモードON時はUIにステータスを表示
     */
    private fun observeStealthMode() {
        viewModelScope.launch {
            settingsRepository.stealthMode.collect { isStealthMode ->
                _uiState.update { it.copy(isStealthMode = isStealthMode) }
                Log.d("RadarViewModel", "Stealth mode: $isStealthMode")
            }
        }
    }
    
    /**
     * デバッグ状態を監視
     * 強制検知モードが有効な場合は、BLEの検知デバイスをオーバーライド
     */
    private fun observeDebugState() {
        viewModelScope.launch {
            // 強制検知モードの状態とデバイスリストを組み合わせて監視
            combine(
                debugHelper.isForceDetectionMode,
                debugHelper.forceDetectedDevices,
                bleManager.detectedDevices
            ) { isForceMode, forceDevices, bleDevices ->
                Triple(isForceMode, forceDevices, bleDevices)
            }.collect { (isForceMode, forceDevices, bleDevices) ->
                _uiState.update { 
                    it.copy(
                        isForceDetectionMode = isForceMode,
                        detectedDevices = if (isForceMode) forceDevices else bleDevices,
                        detectedDeviceCount = if (isForceMode) forceDevices.size else bleDevices.size
                    ) 
                }
            }
        }
    }
    
    /**
     * 新規検知イベントを監視し、通知を実行
     * Phase 5.1: バイブレーション
     * Phase 5.2: 検知時の音声再生
     */
    private fun observeNewDetections() {
        viewModelScope.launch {
            bleManager.newDetectionEvent.collect { event ->
                Log.d("RadarViewModel", "New detection: ${event.uidPrefix} (RSSI: ${event.rssi})")
                // 検知時のバイブレーション・音声通知（設定に応じて）
                notificationManager.notifyDetection()
            }
        }
    }
    
    /**
     * 未フィルタリングのBLE検知イベントを監視
     * 各検知に対してFirebase照会 → フィルタリング → 追加判断
     * 
     * Coroutine Context: viewModelScope
     * → ViewModel破棄時に自動的にキャンセルされる
     * 
     * 担当: 久米（Backend）
     */
    private fun observeRawDetections() {
        viewModelScope.launch {
            bleManager.rawDetectionEvent.collect { event ->
                // 非同期処理（ブロックしない）
                launch {
                    processNewDetection(event.uidPrefix, event.rssi)
                }
            }
        }
    }
    
    /**
     * 新規検知を処理（Firebase照会 → フィルタリング → 追加）
     * 
     * フィルタリング条件:
     * 1. 自分がOFFLINEでない
     * 2. 相手がOFFLINEでない
     * 3. ステータスが一致する
     * 4. 興味タグが1つ以上一致する
     * 
     * @param uidPrefix BLEで検知したuidPrefix
     * @param rssi 受信信号強度（ログ用）
     * 
     * 担当: 久米（Backend）
     */
    private suspend fun processNewDetection(uidPrefix: String, rssi: Int) {
        Log.d("RadarViewModel", "Processing new detection: $uidPrefix (RSSI: $rssi dBm)")
        
        // 0. 自分の情報を確認
        val myUser = currentUser
        if (myUser == null) {
            Log.d("RadarViewModel", "Current user is null, skipping detection")
            return
        }
        
        // 1. 自分がOFFLINEの場合はスキップ
        if (!myUser.status.isActive()) {
            Log.d("RadarViewModel", "Current user is OFFLINE, skipping detection")
            return
        }
        
        // 2. Firebase照会（相手の情報取得）
        val detectedUser = userRepository.getUserByUidPrefix(uidPrefix)
        
        if (detectedUser == null) {
            Log.d("RadarViewModel", "User not found for uidPrefix: $uidPrefix, skipping")
            return
        }
        
        Log.d("RadarViewModel", "User found: ${detectedUser.displayName} (${detectedUser.uid})")
        Log.d("RadarViewModel", "  - Detected status: ${detectedUser.status.displayName}, My status: ${myUser.status.displayName}")
        Log.d("RadarViewModel", "  - Detected tags: ${detectedUser.tags}, My tags: ${myUser.tags}")
        
        // 3. 相手がOFFLINEの場合はスキップ
        if (!detectedUser.status.isActive()) {
            Log.d("RadarViewModel", "User ${detectedUser.displayName} is OFFLINE, filtered out")
            return
        }
        
        // 4. ステータスが一致するか確認
        if (myUser.status != detectedUser.status) {
            Log.d("RadarViewModel", "Status mismatch: my=${myUser.status.displayName}, detected=${detectedUser.status.displayName}, filtered out")
            return
        }
        
        // 5. 興味タグが1つ以上一致するか確認
        val commonTags = myUser.tags.intersect(detectedUser.tags.toSet())
        if (commonTags.isEmpty()) {
            Log.d("RadarViewModel", "No matching tags: my=${myUser.tags}, detected=${detectedUser.tags}, filtered out")
            return
        }
        
        // フィルター通過 → 検知リストに追加
        Log.d("RadarViewModel", "Filter passed! Status: ${myUser.status.displayName}, Common tags: $commonTags")
        Log.d("RadarViewModel", "Adding ${detectedUser.displayName} to detected devices")
        bleManager.addDetectedDevice(uidPrefix)
        
        // 通知（バイブ・音声）
        // BleManager.addDetectedDevice()内で newDetectionEvent が発火され、
        // observeNewDetections()で通知が実行される
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
     * 
     * - ステルスモード中は発信不可
     * - OFFLINEステータスの場合は発信不可
     * 
     * 担当: 久米（Backend）
     */
    fun toggleAdvertising() {
        // ステルスモード中は発信を開始できない
        if (_uiState.value.isStealthMode && !_uiState.value.isAdvertising) {
            viewModelScope.launch {
                _uiEvent.emit(RadarUiEvent.ShowError("ステルスモード中は発信できません。設定からOFFにしてください。"))
            }
            return
        }
        
        // OFFLINEステータスの場合は発信を開始できない
        val user = currentUser
        if (user?.status == UserStatus.OFFLINE && !_uiState.value.isAdvertising) {
            viewModelScope.launch {
                _uiEvent.emit(RadarUiEvent.ShowError("オフライン中は発信できません。ステータスを変更してください。"))
            }
            return
        }
        
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
     * 
     * - ステルスモード中はスキャンのみ開始（発信停止）
     * - OFFLINEステータスの場合はスキャンのみ開始（発信停止）
     * 
     * 担当: 久米（Backend）
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
            
            val user = currentUser
            val isOffline = user?.status == UserStatus.OFFLINE
            
            // ステルスモード中またはOFFLINEの場合はスキャンのみ開始（アドバタイズ停止）
            if (_uiState.value.isStealthMode || isOffline) {
                bleManager.startScanning()
                val reason = if (_uiState.value.isStealthMode) "stealth mode" else "OFFLINE status"
                Log.d("RadarViewModel", "Started scanning only ($reason)")
            } else {
                bleManager.startEncounter(uidPrefix)
            }
        }
    }
}
