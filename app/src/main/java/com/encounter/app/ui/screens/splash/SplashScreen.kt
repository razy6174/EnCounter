package com.encounter.app.ui.screens.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encounter.app.ui.theme.EnCounterTheme

/**
 * スプラッシュ画面（外側）
 * ログイン状態をチェックして適切な画面へ遷移
 * 
 * 担当: 昆野（Frontend）- UI実装
 * ViewModel連携: 久米（Backend）- 実装済み
 * 
 * 注意: 以下のセクションは久米が実装済みのため変更禁止
 * - ViewModelの取得（hiltViewModel）
 * - uiStateの監視
 * - ナビゲーション判定ロジック
 */
@Composable
fun SplashScreen(
    onNavigateToSetup: () -> Unit,
    onNavigateToHome: () -> Unit,
    // ========================================
    // 久米実装: 変更禁止
    // ========================================
    viewModel: SplashViewModel = hiltViewModel()
) {
    // ========================================
    // 久米実装: 変更禁止（状態監視）
    // ========================================
    val uiState by viewModel.uiState.collectAsState()
    
    // ========================================
    // 久米実装: 変更禁止（ナビゲーション処理）
    // ========================================
    LaunchedEffect(uiState.navigationTarget) {
        when (uiState.navigationTarget) {
            is NavigationTarget.ProfileSetup -> onNavigateToSetup()
            is NavigationTarget.Radar -> onNavigateToHome()
            null -> { /* まだ判定中 */ }
        }
    }
    
    // 内側のContent関数を呼び出す
    SplashScreenContent(
        uiState = uiState,
        onRetry = { viewModel.retry() }
    )
}

/**
 * スプラッシュ画面のコンテンツ（内側）
 * 状態を引数で受け取るため、Previewが可能
 * 
 * 昆野担当: 以下は自由に編集可能
 */
@Composable
fun SplashScreenContent(
    uiState: SplashUiState,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // TODO: 昆野 - ロゴ画像を追加
            Text(
                text = "EnCounter",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // ========================================
            // 久米実装: 条件分岐は変更禁止
            // 昆野担当: 各状態のUIデザインは変更可能
            // ========================================
            when {
                uiState.isLoading -> {
                    // TODO: 昆野 - ローディングアニメーションを改善
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "読み込み中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                uiState.error != null -> {
                    // TODO: 昆野 - エラー表示のデザインを改善
                    Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        // TODO: 昆野 - ボタンデザインを改善
                        Text("再試行")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    EnCounterTheme {
        SplashScreenContent(
            uiState = SplashUiState(),
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenErrorPreview() {
    EnCounterTheme {
        SplashScreenContent(
            uiState = SplashUiState(
                isLoading = false,
                error = "認証に失敗しました"
            ),
            onRetry = {}
        )
    }
}
