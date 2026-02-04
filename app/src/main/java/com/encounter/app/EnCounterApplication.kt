package com.encounter.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * アプリケーションクラス
 * Hilt DIコンテナの初期化を行う
 * 
 * 担当: 久米（Backend）
 */
@HiltAndroidApp
class EnCounterApplication : Application()
