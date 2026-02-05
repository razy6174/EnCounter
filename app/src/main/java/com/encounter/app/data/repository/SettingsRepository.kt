package com.encounter.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.encounter.app.ble.BleManager
import com.encounter.app.ble.ScanSensitivity
import com.encounter.app.ble.TxPowerLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * アプリ設定を管理するリポジトリ
 * SharedPreferencesを使用して設定を永続化
 * 
 * 担当: 久米（Backend）
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bleManager: BleManager
) {
    companion object {
        private const val PREFS_NAME = "encounter_settings"
        
        // BLE設定キー
        private const val KEY_SCAN_SENSITIVITY = "scan_sensitivity"
        private const val KEY_TX_POWER_LEVEL = "tx_power_level"
        
        // 通知設定キー
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_SOUND_VOLUME = "sound_volume"
        
        // ステルスモード
        private const val KEY_STEALTH_MODE = "stealth_mode"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 設定値のStateFlow
    private val _scanSensitivity = MutableStateFlow(loadScanSensitivity())
    val scanSensitivity: StateFlow<ScanSensitivity> = _scanSensitivity.asStateFlow()
    
    private val _txPowerLevel = MutableStateFlow(loadTxPowerLevel())
    val txPowerLevel: StateFlow<TxPowerLevel> = _txPowerLevel.asStateFlow()
    
    private val _vibrationEnabled = MutableStateFlow(loadVibrationEnabled())
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()
    
    private val _soundEnabled = MutableStateFlow(loadSoundEnabled())
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()
    
    private val _soundVolume = MutableStateFlow(loadSoundVolume())
    val soundVolume: StateFlow<Float> = _soundVolume.asStateFlow()
    
    private val _stealthMode = MutableStateFlow(loadStealthMode())
    val stealthMode: StateFlow<Boolean> = _stealthMode.asStateFlow()
    
    init {
        // 初回起動時にBleManagerに設定を適用
        applyBleSensitivity(_scanSensitivity.value)
        applyTxPowerLevel(_txPowerLevel.value)
        // ステルスモードが有効なら初期化時にアドバタイズを無効化
        applyStealthMode(_stealthMode.value)
    }
    
    // ========================================
    // BLE設定
    // ========================================
    
    /**
     * 受信感度（RSSI閾値）を設定
     */
    fun setScanSensitivity(sensitivity: ScanSensitivity) {
        prefs.edit().putString(KEY_SCAN_SENSITIVITY, sensitivity.name).apply()
        _scanSensitivity.value = sensitivity
        applyBleSensitivity(sensitivity)
    }
    
    /**
     * 送信電力を設定
     */
    fun setTxPowerLevel(level: TxPowerLevel) {
        prefs.edit().putString(KEY_TX_POWER_LEVEL, level.name).apply()
        _txPowerLevel.value = level
        applyTxPowerLevel(level)
    }
    
    private fun applyBleSensitivity(sensitivity: ScanSensitivity) {
        bleManager.setScanSensitivity(sensitivity)
    }
    
    private fun applyTxPowerLevel(level: TxPowerLevel) {
        bleManager.setTxPowerLevel(level)
    }
    
    private fun loadScanSensitivity(): ScanSensitivity {
        val name = prefs.getString(KEY_SCAN_SENSITIVITY, ScanSensitivity.MEDIUM.name)
        return ScanSensitivity.entries.find { it.name == name } ?: ScanSensitivity.MEDIUM
    }
    
    private fun loadTxPowerLevel(): TxPowerLevel {
        val name = prefs.getString(KEY_TX_POWER_LEVEL, TxPowerLevel.MEDIUM.name)
        return TxPowerLevel.entries.find { it.name == name } ?: TxPowerLevel.MEDIUM
    }
    
    // ========================================
    // 通知設定
    // ========================================
    
    /**
     * バイブレーション設定
     */
    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
        _vibrationEnabled.value = enabled
    }
    
    /**
     * 音声通知設定
     */
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        _soundEnabled.value = enabled
    }
    
    /**
     * 音量設定（0.0 ~ 1.0）
     */
    fun setSoundVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_SOUND_VOLUME, clampedVolume).apply()
        _soundVolume.value = clampedVolume
    }
    
    private fun loadVibrationEnabled(): Boolean {
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, true) // デフォルトON
    }
    
    private fun loadSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true) // デフォルトON
    }
    
    private fun loadSoundVolume(): Float {
        return prefs.getFloat(KEY_SOUND_VOLUME, 0.7f) // デフォルト70%
    }
    
    // ========================================
    // ステルスモード
    // ========================================
    
    /**
     * ステルスモード設定
     * ONにすると発信を停止し、受信のみになる
     */
    fun setStealthMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STEALTH_MODE, enabled).apply()
        _stealthMode.value = enabled
        applyStealthMode(enabled)
    }
    
    /**
     * ステルスモードをBleManagerに適用
     * ONの場合: アドバタイズを停止（スキャンは継続）
     * OFFの場合: アドバタイズを許可（RadarViewModelで開始可能に）
     */
    private fun applyStealthMode(enabled: Boolean) {
        bleManager.setStealthMode(enabled)
    }
    
    /**
     * ステルスモードが有効かどうか（外部から確認用）
     */
    fun isStealthModeEnabled(): Boolean = _stealthMode.value
    
    private fun loadStealthMode(): Boolean {
        return prefs.getBoolean(KEY_STEALTH_MODE, false) // デフォルトOFF
    }
    
    // ========================================
    // 距離説明テキスト
    // ========================================
    
    /**
     * 現在の設定に基づく距離説明を取得
     */
    fun getDistanceDescription(): String {
        val rxDistance = when (_scanSensitivity.value) {
            ScanSensitivity.HIGH -> "約10m"
            ScanSensitivity.MEDIUM -> "約5m"
            ScanSensitivity.LOW -> "約1-2m"
        }
        val txDistance = when (_txPowerLevel.value) {
            TxPowerLevel.HIGH -> "約10m"
            TxPowerLevel.MEDIUM -> "約5-8m"
            TxPowerLevel.LOW -> "約1-3m"
            TxPowerLevel.ULTRA_LOW -> "約0.5-1m"
        }
        return "あなたは${txDistance}まで届き、${rxDistance}まで検知します"
    }
}
