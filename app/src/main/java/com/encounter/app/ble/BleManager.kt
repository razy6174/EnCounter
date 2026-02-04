package com.encounter.app.ble

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
import android.os.ParcelUuid
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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
    
    /**
     * Bluetoothが有効かどうか
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
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
        
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
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
     */
    fun startScanning() {
        val scanner = this.scanner ?: run {
            Log.e(TAG, "Scanner not available")
            return
        }
        
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()
        )
        
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        try {
            scanner.startScan(filters, settings, scanCallback)
            _isScanning.value = true
            Log.d(TAG, "Scanning started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for scanning", e)
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
        val serviceData = result.scanRecord?.getServiceData(ParcelUuid(SERVICE_UUID))
        if (serviceData != null) {
            val uid = String(serviceData)
            Log.d(TAG, "Detected device with UID: $uid")
            _detectedDevices.value = _detectedDevices.value + uid
        }
    }
}
