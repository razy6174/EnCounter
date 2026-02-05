package com.encounter.app

import android.app.Application
import android.util.Log
import com.encounter.app.data.repository.UserRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

private const val TAG = "EnCounterApplication"

/**
 * アプリケーションクラス
 * Hilt DIコンテナの初期化を行う
 * 
 * 担当: 久米（Backend）
 */
@HiltAndroidApp
class EnCounterApplication : Application() {
    
    @Inject
    lateinit var userRepository: UserRepository
    
    override fun onCreate() {
        super.onCreate()
        
        // アプリ起動時にFirebaseキャッシュをクリア
        // 他ユーザーのステータスや興味タグが変更されている可能性があるため
        userRepository.clearUserCache()
        Log.d(TAG, "Application started - user cache cleared")
    }
}
