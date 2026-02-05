package com.encounter.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encounter.app.ble.ScanSensitivity
import com.encounter.app.ble.TxPowerLevel
import com.encounter.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 設定画面のUI状態
 */
data class SettingsUiState(
    // BLE設定
    val scanSensitivity: ScanSensitivity = ScanSensitivity.MEDIUM,
    val txPowerLevel: TxPowerLevel = TxPowerLevel.MEDIUM,
    
    // 通知設定
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.7f,
    
    // ステルスモード
    val stealthMode: Boolean = false,
    
    // 距離説明
    val distanceDescription: String = ""
)

/**
 * 設定画面のViewModel
 * BLE検知感度・発信強度、通知設定を管理
 * 
 * 担当: 久米（Backend）
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        observeSettings()
    }
    
    /**
     * 設定値を監視
     * 6つのFlowを個別に監視して状態を更新
     */
    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.scanSensitivity.collect { value ->
                _uiState.value = _uiState.value.copy(
                    scanSensitivity = value,
                    distanceDescription = settingsRepository.getDistanceDescription()
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.txPowerLevel.collect { value ->
                _uiState.value = _uiState.value.copy(
                    txPowerLevel = value,
                    distanceDescription = settingsRepository.getDistanceDescription()
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.vibrationEnabled.collect { value ->
                _uiState.value = _uiState.value.copy(vibrationEnabled = value)
            }
        }
        viewModelScope.launch {
            settingsRepository.soundEnabled.collect { value ->
                _uiState.value = _uiState.value.copy(soundEnabled = value)
            }
        }
        viewModelScope.launch {
            settingsRepository.soundVolume.collect { value ->
                _uiState.value = _uiState.value.copy(soundVolume = value)
            }
        }
        viewModelScope.launch {
            settingsRepository.stealthMode.collect { value ->
                _uiState.value = _uiState.value.copy(stealthMode = value)
            }
        }
    }
    
    // ========================================
    // BLE設定
    // ========================================
    
    /**
     * 受信感度を設定
     */
    fun setScanSensitivity(sensitivity: ScanSensitivity) {
        settingsRepository.setScanSensitivity(sensitivity)
    }
    
    /**
     * 送信電力を設定
     */
    fun setTxPowerLevel(level: TxPowerLevel) {
        settingsRepository.setTxPowerLevel(level)
    }
    
    // ========================================
    // 通知設定
    // ========================================
    
    /**
     * バイブレーション設定
     */
    fun setVibrationEnabled(enabled: Boolean) {
        settingsRepository.setVibrationEnabled(enabled)
    }
    
    /**
     * 音声通知設定
     */
    fun setSoundEnabled(enabled: Boolean) {
        settingsRepository.setSoundEnabled(enabled)
    }
    
    /**
     * 音量設定
     */
    fun setSoundVolume(volume: Float) {
        settingsRepository.setSoundVolume(volume)
    }
    
    // ========================================
    // ステルスモード
    // ========================================
    
    /**
     * ステルスモード設定
     */
    fun setStealthMode(enabled: Boolean) {
        settingsRepository.setStealthMode(enabled)
    }
}
