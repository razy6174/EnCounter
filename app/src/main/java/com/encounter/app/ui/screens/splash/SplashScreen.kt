package com.encounter.app.ui.screens.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.encounter.app.ui.theme.EnCounterTheme
import kotlinx.coroutines.delay

/**
 * スプラッシュ画面
 * ログイン状態をチェックして適切な画面へ遷移
 * 
 * 担当: 昆野（Frontend）- UI実装
 */
@Composable
fun SplashScreen(
    onNavigateToSetup: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    // TODO: 実際のログイン状態チェックを実装
    LaunchedEffect(Unit) {
        delay(1500)
        // 仮: 常にセットアップへ遷移
        onNavigateToSetup()
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // TODO: ロゴとアニメーションを追加
        Text(
            text = "EnCounter",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    EnCounterTheme {
        SplashScreen(
            onNavigateToSetup = {},
            onNavigateToHome = {}
        )
    }
}
