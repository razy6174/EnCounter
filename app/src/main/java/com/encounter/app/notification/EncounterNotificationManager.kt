package com.encounter.app.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.encounter.app.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 検知時の通知を管理するクラス
 * バイブレーションと音声再生を担当
 * 
 * 担当: 久米（Backend）
 */
@Singleton
class EncounterNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "EncounterNotificationManager"
        
        // 鼓動風バイブレーションパターン（弱・強・弱）
        // タイミング: 0ms開始, 100ms振動, 50ms休止, 300ms振動, 50ms休止, 100ms振動
        private val HEARTBEAT_PATTERN = longArrayOf(0, 100, 50, 300, 50, 100)
        // 振幅パターン（0=休止, 255=最大強度）
        private val HEARTBEAT_AMPLITUDES = intArrayOf(0, 120, 0, 255, 0, 120)
        
        // シンプルな単発振動
        private const val SIMPLE_VIBRATION_DURATION = 200L
        private const val SIMPLE_VIBRATION_AMPLITUDE = 180
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Vibrator
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    // SoundPool（短い効果音用・軽量）
    private var soundPool: SoundPool? = null
    private var matchSoundId: Int = 0
    private var isSoundLoaded = false
    
    // MediaPlayer（長い音声用）
    private var mediaPlayer: MediaPlayer? = null
    
    init {
        initializeSoundPool()
    }
    
    /**
     * SoundPoolを初期化
     * 短い効果音を事前にロード
     */
    private fun initializeSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            soundPool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build()
                .apply {
                    setOnLoadCompleteListener { _, _, status ->
                        isSoundLoaded = status == 0
                        Log.d(TAG, "Sound loaded: $isSoundLoaded")
                    }
                }
            
            // マッチング音をロード（res/raw/notification_match.mp3 が必要）
            // リソースを動的に取得（ファイルがなくてもコンパイルエラーにならない）
            val resId = context.resources.getIdentifier(
                "notification_match", "raw", context.packageName
            )
            matchSoundId = if (resId != 0) {
                soundPool?.load(context, resId, 1) ?: 0
            } else {
                Log.w(TAG, "notification_match sound not found in res/raw/, will use system default")
                0
            }
            
            Log.d(TAG, "SoundPool initialized, matchSoundId: $matchSoundId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SoundPool", e)
        }
    }
    
    /**
     * 検知時の通知を実行
     * 設定に応じてバイブレーションと音声を再生
     */
    fun notifyDetection() {
        scope.launch {
            val vibrationEnabled = settingsRepository.vibrationEnabled.first()
            val soundEnabled = settingsRepository.soundEnabled.first()
            val volume = settingsRepository.soundVolume.first()
            
            Log.d(TAG, "notifyDetection: vibration=$vibrationEnabled, sound=$soundEnabled, volume=$volume")
            
            if (vibrationEnabled) {
                vibrateHeartbeat()
            }
            
            if (soundEnabled) {
                playMatchSound(volume)
            }
        }
    }
    
    /**
     * シンプルな短い振動を実行
     */
    fun vibrateSimple() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(
                        VibrationEffect.createOneShot(
                            SIMPLE_VIBRATION_DURATION,
                            SIMPLE_VIBRATION_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(SIMPLE_VIBRATION_DURATION)
                }
                Log.d(TAG, "Simple vibration executed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate", e)
        }
    }
    
    /**
     * 鼓動風の振動パターンを実行（検知時のメイン通知）
     */
    fun vibrateHeartbeat() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(
                        VibrationEffect.createWaveform(
                            HEARTBEAT_PATTERN,
                            HEARTBEAT_AMPLITUDES,
                            -1 // リピートなし
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(HEARTBEAT_PATTERN, -1)
                }
                Log.d(TAG, "Heartbeat vibration executed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate heartbeat", e)
        }
    }
    
    /**
     * マッチング音を再生
     * @param volume 音量（0.0f ~ 1.0f）
     */
    fun playMatchSound(volume: Float = 0.7f) {
        try {
            if (isSoundLoaded && matchSoundId != 0) {
                // SoundPoolで再生（軽量・低遅延）
                soundPool?.play(
                    matchSoundId,
                    volume, // 左チャンネル
                    volume, // 右チャンネル
                    1,      // 優先度
                    0,      // ループなし
                    1.0f    // 再生速度
                )
                Log.d(TAG, "Match sound played via SoundPool, volume: $volume")
            } else {
                // SoundPoolが使えない場合、システムデフォルト音を使用
                playSystemNotificationSound(volume)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play match sound", e)
            // フォールバック: システム音
            playSystemNotificationSound(volume)
        }
    }
    
    /**
     * システムのデフォルト通知音を再生（フォールバック用）
     */
    private fun playSystemNotificationSound(volume: Float) {
        try {
            val notification = android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_NOTIFICATION
            )
            val ringtone = android.media.RingtoneManager.getRingtone(context, notification)
            ringtone?.play()
            Log.d(TAG, "System notification sound played")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play system notification sound", e)
        }
    }
    
    /**
     * MediaPlayerを使用してカスタム音声ファイルを再生
     * 長い音声ファイルの場合に使用
     * @param resourceId res/rawのリソースID
     * @param volume 音量（0.0f ~ 1.0f）
     */
    fun playCustomSound(resourceId: Int, volume: Float = 0.7f) {
        try {
            releaseMediaPlayer()
            
            mediaPlayer = MediaPlayer.create(context, resourceId)?.apply {
                setVolume(volume, volume)
                setOnCompletionListener {
                    releaseMediaPlayer()
                }
                start()
            }
            Log.d(TAG, "Custom sound played, resourceId: $resourceId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play custom sound", e)
        }
    }
    
    /**
     * MediaPlayerを解放
     */
    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release MediaPlayer", e)
        }
    }
    
    /**
     * リソースを解放
     * アプリ終了時に呼び出す
     */
    fun release() {
        try {
            releaseMediaPlayer()
            soundPool?.release()
            soundPool = null
            isSoundLoaded = false
            Log.d(TAG, "Resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release resources", e)
        }
    }
}
