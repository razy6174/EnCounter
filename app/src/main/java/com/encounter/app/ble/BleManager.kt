package com.encounter.app.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 権限状態
 */
enum class PermissionState {
    UNKNOWN,
    GRANTED,
    DENIED,
    DENIED_PERMANENTLY
}

/**
 * 受信感度（RSSI閾値）プリセット
 * 
 * 担当: 久米（Backend）
 */
enum class ScanSensitivity(val rssiThreshold: Int, val displayName: String, val description: String) {
    HIGH(-85, "高感度", "約10m以上 - 広いエリアをカバー"),
    MEDIUM(-70, "標準", "約5m - バランスの良い設定"),
    LOW(-50, "低感度", "約1-2m - すぐ近くの人だけ")
}

/**
 * 送信電力プリセット
 * 
 * 担当: 久米（Backend）
 */
enum class TxPowerLevel(val advertiseLevel: Int, val displayName: String, val description: String) {
    HIGH(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH, "強", "約10m以上 - 広範囲に届く"),
    MEDIUM(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM, "中", "約5-8m - 標準"),
    LOW(AdvertiseSettings.ADVERTISE_TX_POWER_LOW, "弱", "約1-3m - 近距離のみ"),
    ULTRA_LOW(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW, "極弱", "約0.5-1m - 省電力")
}

/**
 * 新規検知イベント（通知用）
 */
data class NewDetectionEvent(
    val uidPrefix: String,
    val rssi: Int
)

/**
 * BLE通信を管理するクラス
 * Advertise（発信）とScan（受信）を担当
 * 
 * 担当: 久米（Backend）
 */
@Singleton
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BleManager"
        // アプリ固有のService UUID
        val SERVICE_UUID: UUID = UUID.fromString("0000EC00-0000-1000-8000-00805F9B34FB")
        
        // デフォルト値
        const val DEFAULT_RSSI_THRESHOLD = -70
        val DEFAULT_TX_POWER = TxPowerLevel.MEDIUM
        val DEFAULT_SENSITIVITY = ScanSensitivity.MEDIUM
    }
    
    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }
    
    private val advertiser: BluetoothLeAdvertiser? by lazy {
        bluetoothAdapter?.bluetoothLeAdvertiser
    }
    
    private val scanner: BluetoothLeScanner? by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }
    
    // 検知したデバイスのUID一覧
    private val _detectedDevices = MutableStateFlow<Set<String>>(emptySet())
    val detectedDevices: StateFlow<Set<String>> = _detectedDevices.asStateFlow()
    
    // スキャン状態
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    // アドバタイズ状態
    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    // 権限状態
    private val _permissionState = MutableStateFlow(PermissionState.UNKNOWN)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    // 設定値
    private var rssiThreshold: Int = DEFAULT_RSSI_THRESHOLD
    private var txPowerLevel: TxPowerLevel = DEFAULT_TX_POWER
    private var currentUidPrefix: String? = null
    
    // 新規検知イベント（通知用）
    private val _newDetectionEvent = MutableSharedFlow<NewDetectionEvent>(extraBufferCapacity = 10)
    val newDetectionEvent: SharedFlow<NewDetectionEvent> = _newDetectionEvent.asSharedFlow()
    
    /**
     * RSSI閾値（受信感度）を設定
     * 再スキャンは自動では行われないため、必要に応じてrestartScanningを呼び出す
     */
    fun setRssiThreshold(threshold: Int) {
        rssiThreshold = threshold
        Log.d(TAG, "RSSI threshold updated: $threshold dBm")
    }
    
    /**
     * 受信感度をプリセットから設定
     */
    fun setScanSensitivity(sensitivity: ScanSensitivity) {
        setRssiThreshold(sensitivity.rssiThreshold)
    }
    
    /**
     * 送信電力を設定（アドバタイズ再起動が必要）
     */
    fun setTxPowerLevel(level: TxPowerLevel) {
        txPowerLevel = level
        Log.d(TAG, "TxPower level updated: ${level.displayName}")
        
        // アドバタイズ中なら再起動
        if (_isAdvertising.value && currentUidPrefix != null) {
            restartAdvertising()
        }
    }
    
    /**
     * アドバタイズを再起動（設定変更反映用）
     */
    private fun restartAdvertising() {
        val uidPrefix = currentUidPrefix ?: return
        stopAdvertising()
        startAdvertising(uidPrefix)
    }
    
    /**
     * 現在のRSSI閾値を取得
     */
    fun getRssiThreshold(): Int = rssiThreshold
    
    /**
     * 現在の送信電力を取得
     */
    fun getTxPowerLevel(): TxPowerLevel = txPowerLevel
    
    /**
     * Bluetoothが有効かどうか
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * 位置情報サービスが有効かどうか（Android 11以下でBLEスキャンに必要）
     */
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager?.isLocationEnabled == true
        } else {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }
    }
    
    /**
     * アドバタイズを開始
     * @param uidPrefix ユーザーのUID（短縮版16文字）
     */
    fun startAdvertising(uidPrefix: String) {
        val advertiser = this.advertiser ?: run {
            Log.e(TAG, "Advertiser not available")
            return
        }
        
        currentUidPrefix = uidPrefix
        
        // 設定された送信電力を使用
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(txPowerLevel.advertiseLevel)
            .setConnectable(false)
            .build()
        
        val advertiseData = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .setIncludeDeviceName(false)
            .build()
        
        // Scan ResponseにuidPrefixをそのまま含める
        val scanResponse = AdvertiseData.Builder()
            .addServiceData(ParcelUuid(SERVICE_UUID), uidPrefix.toByteArray())
            .build()
        
        try {
            advertiser.startAdvertising(settings, advertiseData, scanResponse, advertiseCallback)
            Log.d(TAG, "Advertising started with uidPrefix: $uidPrefix, txPower: ${txPowerLevel.displayName}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for advertising", e)
        }
    }
    
    /**
     * アドバタイズを停止
     */
    fun stopAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            _isAdvertising.value = false
            Log.d(TAG, "Advertising stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for stopping advertising", e)
        }
    }
    
    /**
     * スキャンを開始
     * @param useFilter trueの場合Service UUIDでフィルタリング、falseの場合全デバイスをスキャン
     */
    fun startScanning(useFilter: Boolean = true) {
        val scanner = this.scanner ?: run {
            Log.e(TAG, "Scanner not available")
            return
        }
        
        val filters = if (useFilter) {
            listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(SERVICE_UUID))
                    .build()
            )
        } else {
            // デバッグ用: フィルタなし（全BLEデバイスをスキャン）
            emptyList()
        }
        
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()
        
        try {
            scanner.startScan(filters, settings, scanCallback)
            _isScanning.value = true
            Log.d(TAG, "Scan started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for scanning", e)
        } catch (e: Exception) {
            Log.e(TAG, "Scan start failed", e)
        }
    }
    
    /**
     * スキャンを停止
     */
    fun stopScanning() {
        try {
            scanner?.stopScan(scanCallback)
            _isScanning.value = false
            Log.d(TAG, "Scanning stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for stopping scan", e)
        }
    }
    
    /**
     * 検知デバイスリストをクリア
     */
    fun clearDetectedDevices() {
        _detectedDevices.value = emptySet()
    }
    
    /**
     * デバッグ用のUIDを生成
     * Firebase不要で動作確認できるよう、ランダムなUIDを生成
     */
    fun generateDebugUid(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "DBG${timestamp % 100000}$random"
    }
    
    /**
     * すれ違い通信を開始（スキャン + アドバタイズ同時）
     * Firebase不要でBLE通信のみをテストする場合に使用
     * 
     * @param uidPrefix ユーザーのuidPrefix（16文字、省略時は自動生成）
     * @param useFilter スキャン時にService UUIDフィルタを使用するか（デバッグ時はfalse推奨）
     */
    fun startEncounter(uidPrefix: String? = null, useFilter: Boolean = false) {
        val actualUidPrefix = uidPrefix ?: generateDebugUid().take(16)
        startAdvertising(actualUidPrefix)
        startScanning(useFilter)
        Log.d(TAG, "Encounter started with uidPrefix: $actualUidPrefix (filter: $useFilter)")
    }
    
    /**
     * すれ違い通信を停止（スキャン + アドバタイズ同時）
     */
    fun stopEncounter() {
        stopAdvertising()
        stopScanning()
        Log.d(TAG, "Encounter stopped")
    }
    
    /**
     * すれ違い通信がアクティブかどうか
     */
    fun isEncounterActive(): Boolean {
        return _isScanning.value || _isAdvertising.value
    }
    
    /**
     * 必要なBLE権限が付与されているかチェック
     */
    fun checkPermissions(): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        
        val allGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == 
                PackageManager.PERMISSION_GRANTED
        }
        
        _permissionState.value = if (allGranted) {
            PermissionState.GRANTED
        } else {
            PermissionState.DENIED
        }
        
        return allGranted
    }
    
    /**
     * 権限の状態を更新（Activity側から呼び出す）
     */
    fun updatePermissionState(granted: Boolean, permanentlyDenied: Boolean = false) {
        _permissionState.value = when {
            granted -> PermissionState.GRANTED
            permanentlyDenied -> PermissionState.DENIED_PERMANENTLY
            else -> PermissionState.DENIED
        }
    }
    
    /**
     * 必要なBLE権限のリストを取得
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
    
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            _isAdvertising.value = true
            Log.d(TAG, "Advertise started successfully")
        }
        
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            _isAdvertising.value = false
            Log.e(TAG, "Advertise failed with error: $errorCode")
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            processScanResult(result)
        }
        
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            results.forEach { processScanResult(it) }
        }
        
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            _isScanning.value = false
            Log.e(TAG, "Scan failed with error: $errorCode")
        }
    }
    
    private fun processScanResult(result: ScanResult) {
        val scanRecord = result.scanRecord
        val deviceAddress = result.device?.address ?: "unknown"
        val rssi = result.rssi
        
        // 動的なRSSI閾値によるフィルタリング
        if (rssi < rssiThreshold) {
            return
        }
        
        // Service UUIDでフィルタリング
        val hasTargetServiceUuid = scanRecord?.serviceUuids?.contains(ParcelUuid(SERVICE_UUID)) == true
        
        // Service Data取得
        val serviceData = scanRecord?.getServiceData(ParcelUuid(SERVICE_UUID))
        
        if (serviceData != null) {
            val uid = String(serviceData)
            val isNewDetection = uid !in _detectedDevices.value
            Log.d(TAG, "Detected: $uid (RSSI: $rssi dBm, new: $isNewDetection)")
            
            if (isNewDetection) {
                _detectedDevices.value = _detectedDevices.value + uid
                // 新規検知イベントを発火（通知用）
                _newDetectionEvent.tryEmit(NewDetectionEvent(uid, rssi))
            }
        } else if (hasTargetServiceUuid) {
            val isNewDetection = deviceAddress !in _detectedDevices.value
            Log.d(TAG, "Detected: $deviceAddress (RSSI: $rssi dBm, new: $isNewDetection)")
            
            if (isNewDetection) {
                _detectedDevices.value = _detectedDevices.value + deviceAddress
                _newDetectionEvent.tryEmit(NewDetectionEvent(deviceAddress, rssi))
            }
        }
    }
}
