package com.encounter.app.debug

import android.util.Log
import com.encounter.app.data.repository.UserRepository
import com.encounter.app.domain.model.User
import com.encounter.app.domain.model.UserStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * デモ・デバッグ用ヘルパークラス
 * プレゼン時の検証やデモ用の機能を提供
 * 
 * 担当: 久米（Backend）
 */
@Singleton
class DebugHelper @Inject constructor(
    private val userRepository: UserRepository
) {
    
    companion object {
        private const val TAG = "DebugHelper"
        
        /** デバッグログのON/OFF */
        var isDebugLogEnabled = true
        
        /** ダミーユーザーのリスト */
        private val dummyUsers = listOf(
            DummyUser(
                displayName = "田中太郎",
                comment = "エンジニアです。よろしくお願いします！",
                tags = listOf("エンジニア", "音楽", "ゲーム"),
                status = UserStatus.WANTED
            ),
            DummyUser(
                displayName = "佐藤花子",
                comment = "デザイナーやってます",
                tags = listOf("デザイン", "アート", "カフェ"),
                status = UserStatus.WANTED
            ),
            DummyUser(
                displayName = "鈴木健",
                comment = "プログラマー兼ゲーマー",
                tags = listOf("エンジニア", "ゲーム", "アニメ"),
                status = UserStatus.BUSY
            ),
            DummyUser(
                displayName = "高橋美咲",
                comment = "マーケティング担当",
                tags = listOf("マーケティング", "旅行", "カフェ"),
                status = UserStatus.WANTED
            ),
            DummyUser(
                displayName = "伊藤誠",
                comment = "フロントエンドエンジニア",
                tags = listOf("エンジニア", "デザイン", "音楽"),
                status = UserStatus.WANTED
            )
        )
    }
    
    private val _forceDetectedDevices = MutableStateFlow<Set<String>>(emptySet())
    val forceDetectedDevices: StateFlow<Set<String>> = _forceDetectedDevices.asStateFlow()
    
    private val _isForceDetectionMode = MutableStateFlow(false)
    val isForceDetectionMode: StateFlow<Boolean> = _isForceDetectionMode.asStateFlow()
    
    /**
     * ダミーユーザーデータ
     */
    data class DummyUser(
        val displayName: String,
        val comment: String,
        val tags: List<String>,
        val status: UserStatus
    )
    
    /**
     * デバッグログを出力
     */
    fun d(tag: String, message: String) {
        if (isDebugLogEnabled) {
            Log.d(tag, message)
        }
    }
    
    /**
     * デバッグエラーログを出力
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isDebugLogEnabled) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
    
    /**
     * デバッグログの有効/無効を切り替え
     */
    fun toggleDebugLog() {
        isDebugLogEnabled = !isDebugLogEnabled
        Log.i(TAG, "Debug log is now: ${if (isDebugLogEnabled) "ENABLED" else "DISABLED"}")
    }
    
    /**
     * ダミーユーザーをFirestoreに作成
     * デモ・プレゼン用に使用
     * 
     * @return 作成したユーザーのUIDリスト
     */
    suspend fun createDummyUsers(): Result<List<String>> {
        return try {
            val createdUids = mutableListOf<String>()
            
            // 匿名認証でダミーユーザーを作成
            dummyUsers.forEach { dummyUser ->
                val signInResult = userRepository.signInAnonymously()
                
                signInResult.fold(
                    onSuccess = { uid ->
                        // ユーザープロフィールを作成
                        val user = User(
                            uid = uid,
                            uidPrefix = uid.take(16),
                            displayName = dummyUser.displayName,
                            comment = dummyUser.comment,
                            status = dummyUser.status,
                            tags = dummyUser.tags
                        )
                        
                        val saveResult = userRepository.saveUserProfile(user)
                        saveResult.fold(
                            onSuccess = {
                                createdUids.add(uid)
                                Log.d(TAG, "Created dummy user: ${user.displayName} (uid: $uid, uidPrefix: ${user.uidPrefix})")
                            },
                            onFailure = { e ->
                                Log.e(TAG, "Failed to save dummy user: ${dummyUser.displayName}", e)
                            }
                        )
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Failed to sign in for dummy user: ${dummyUser.displayName}", e)
                    }
                )
            }
            
            if (createdUids.isNotEmpty()) {
                Result.success(createdUids)
            } else {
                Result.failure(Exception("No dummy users were created"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating dummy users", e)
            Result.failure(e)
        }
    }
    
    /**
     * 強制検知モードを有効/無効にする
     * BLE通信なしでUID検知をシミュレート
     */
    fun toggleForceDetectionMode() {
        _isForceDetectionMode.value = !_isForceDetectionMode.value
        Log.i(TAG, "Force detection mode: ${if (_isForceDetectionMode.value) "ENABLED" else "DISABLED"}")
        
        // 無効にした場合は検知デバイスをクリア
        if (!_isForceDetectionMode.value) {
            _forceDetectedDevices.value = emptySet()
        }
    }
    
    /**
     * 強制検知モードで検知するUIDを追加
     * 
     * @param uidPrefix 検知するUIDプレフィックス（16文字）
     */
    fun addForceDetectedDevice(uidPrefix: String) {
        if (!_isForceDetectionMode.value) {
            Log.w(TAG, "Force detection mode is not enabled")
            return
        }
        
        val currentDevices = _forceDetectedDevices.value.toMutableSet()
        currentDevices.add(uidPrefix)
        _forceDetectedDevices.value = currentDevices
        
        Log.d(TAG, "Added force detected device: $uidPrefix (total: ${currentDevices.size})")
    }
    
    /**
     * 強制検知モードで検知するUIDを削除
     */
    fun removeForceDetectedDevice(uidPrefix: String) {
        val currentDevices = _forceDetectedDevices.value.toMutableSet()
        currentDevices.remove(uidPrefix)
        _forceDetectedDevices.value = currentDevices
        
        Log.d(TAG, "Removed force detected device: $uidPrefix")
    }
    
    /**
     * 強制検知モードで複数のUIDを一度に追加
     */
    fun setForceDetectedDevices(uidPrefixes: Set<String>) {
        if (!_isForceDetectionMode.value) {
            Log.w(TAG, "Force detection mode is not enabled")
            return
        }
        
        _forceDetectedDevices.value = uidPrefixes
        Log.d(TAG, "Set force detected devices: ${uidPrefixes.size} devices")
    }
    
    /**
     * 強制検知モードをクリア
     */
    fun clearForceDetectedDevices() {
        _forceDetectedDevices.value = emptySet()
        Log.d(TAG, "Cleared all force detected devices")
    }
    
    /**
     * ダミーユーザー情報を取得
     */
    fun getDummyUsers(): List<DummyUser> = dummyUsers
}
