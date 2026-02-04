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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        
        /**
         * RSSI閾値（この値以上の信号強度でのみ検知）
         * 目安:
         *   -50 dBm: 約1m以内
         *   -60 dBm: 約2-3m
         *   -70 dBm: 約5m
         *   -80 dBm: 約10m以上
         * 
         * 2~5m を目標とする場合: -70 dBm 程度
         */
        const val RSSI_THRESHOLD = -70
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
     * @param uid ユーザーのUID（短縮版16文字）
     */
    fun startAdvertising(uid: String) {
        val advertiser = this.advertiser ?: run {
            Log.e(TAG, "Advertiser not available")
            return
        }
        
        // 送信電力の設定
        // ADVERTISE_TX_POWER_ULTRA_LOW: 最小（約1m）
        // ADVERTISE_TX_POWER_LOW: 低（約3m）
        // ADVERTISE_TX_POWER_MEDIUM: 中（約7m）
        // ADVERTISE_TX_POWER_HIGH: 高（約10m以上）
        // ※ 実際の距離は端末のハードウェアにより異なる
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)  // 中程度の送信電力
            .setConnectable(false)
            .build()
        
        val advertiseData = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .setIncludeDeviceName(false)
            .build()
        
        // Scan ResponseにUIDを含める（16文字に短縮）
        val shortUid = uid.take(16)
        val scanResponse = AdvertiseData.Builder()
            .addServiceData(ParcelUuid(SERVICE_UUID), shortUid.toByteArray())
            .build()
        
        try {
            advertiser.startAdvertising(settings, advertiseData, scanResponse, advertiseCallback)
            Log.d(TAG, "Advertising started with UID: $shortUid")
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
     * @param uid ユーザーのUID（省略時は自動生成）
     * @param useFilter スキャン時にService UUIDフィルタを使用するか（デバッグ時はfalse推奨）
     */
    fun startEncounter(uid: String? = null, useFilter: Boolean = false) {
        val actualUid = uid ?: generateDebugUid()
        startAdvertising(actualUid)
        startScanning(useFilter)
        Log.d(TAG, "Encounter started with UID: $actualUid (filter: $useFilter)")
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
        
        // RSSIによる距離フィルタリング（2~5mを目標）
        if (rssi < RSSI_THRESHOLD) {
            return
        }
        
        // Service UUIDでフィルタリング
        val hasTargetServiceUuid = scanRecord?.serviceUuids?.contains(ParcelUuid(SERVICE_UUID)) == true
        
        // Service Data取得
        val serviceData = scanRecord?.getServiceData(ParcelUuid(SERVICE_UUID))
        
        if (serviceData != null) {
            val uid = String(serviceData)
            Log.d(TAG, "Detected: $uid (RSSI: $rssi dBm)")
            _detectedDevices.value = _detectedDevices.value + uid
        } else if (hasTargetServiceUuid) {
            Log.d(TAG, "Detected: $deviceAddress (RSSI: $rssi dBm)")
            _detectedDevices.value = _detectedDevices.value + deviceAddress
        }
    }
}
