package com.encounter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.encounter.app.navigation.AppNavGraph
import com.encounter.app.ui.theme.EnCounterTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * メインアクティビティ
 * アプリのエントリーポイント
 * 
 * 担当: 共通（コンフリクト回避のため、変更時は相談）
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnCounterTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}