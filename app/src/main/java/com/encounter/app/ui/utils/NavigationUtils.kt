package com.encounter.app.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * ナビゲーション関連のユーティリティ
 * 
 * 担当: 久米（Backend）
 */

/**
 * 二重タップを防止する安全な戻るナビゲーション関数を生成
 * 
 * 使用例:
 * ```
 * @Composable
 * fun SomeScreen(onNavigateBack: () -> Unit) {
 *     val safeNavigateBack = rememberSafeNavigateBack(onNavigateBack)
 *     
 *     TopAppBar(
 *         navigationIcon = {
 *             IconButton(onClick = safeNavigateBack) {
 *                 Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
 *             }
 *         }
 *     )
 * }
 * ```
 * 
 * @param onNavigateBack 元のナビゲーションコールバック
 * @return 二重タップ防止付きのコールバック
 */
@Composable
fun rememberSafeNavigateBack(onNavigateBack: () -> Unit): () -> Unit {
    var isNavigating by remember { mutableStateOf(false) }
    
    return remember(onNavigateBack) {
        {
            if (!isNavigating) {
                isNavigating = true
                onNavigateBack()
            }
        }
    }
}

/**
 * 汎用的な一度きり実行ガード
 * 戻るボタン以外にも、送信ボタンなどに使用可能
 * 
 * @param action 実行するアクション
 * @return ガード付きのコールバック
 */
@Composable
fun rememberSingleExecutionAction(action: () -> Unit): () -> Unit {
    var isExecuted by remember { mutableStateOf(false) }
    
    return remember(action) {
        {
            if (!isExecuted) {
                isExecuted = true
                action()
            }
        }
    }
}
