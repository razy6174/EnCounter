package com.encounter.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle // 追加
import androidx.compose.ui.text.font.Font // 追加
import androidx.compose.ui.text.font.FontFamily // 追加
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // 文字サイズ調整用に追加
import androidx.hilt.navigation.compose.hiltViewModel
import com.encounter.app.R // リソースIDのために必要（パッケージ名は環境に合わせてください）
import com.encounter.app.ui.theme.EnCounterTheme
import androidx.compose.material3.ButtonDefaults

/**
 * プロフィール設定画面（外側）
 * 
 * 担当: 昆野（Frontend）- UI実装
 * ViewModel連携: 久米（Backend）- 実装済み
 * 
 * 注意: 以下のセクションは久米が実装済みのため変更禁止
 * - ViewModelの取得（hiltViewModel）
 * - uiState/uiEventの監視
 * - ViewModel関数の呼び出し（onChange, onClick内）
 */
@Composable
fun ProfileSetupScreen(
    onNavigateToTags: () -> Unit,
    // ========================================
    // 久米実装: 変更禁止
    // ========================================
    viewModel: ProfileViewModel = hiltViewModel()
) {
    // ========================================
    // 久米実装: 変更禁止（状態監視）
    // ========================================
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // ========================================
    // 久米実装: 変更禁止（UIイベント監視）
    // ========================================
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ProfileUiEvent.NavigateToTagSelection -> onNavigateToTags()
                is ProfileUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> { /* 他のイベントはこの画面では処理しない */ }
            }
        }
    }
    
    // 内側のContent関数を呼び出す
    ProfileSetupScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onDisplayNameChanged = { viewModel.onDisplayNameChanged(it) },
        onCommentChanged = { viewModel.onCommentChanged(it) },
        onNavigateToTags = { viewModel.navigateToTagSelection() }
    )
}

/**
 * プロフィール設定画面のコンテンツ（内側）
 * 状態を引数で受け取るため、Previewが可能
 * 
 * 昆野担当: 以下は自由に編集可能
 */
@Composable
fun ProfileSetupScreenContent(
    uiState: ProfileUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onDisplayNameChanged: (String) -> Unit,
    onCommentChanged: (String) -> Unit,
    onNavigateToTags: () -> Unit
) {
    // フォントファミリーを定義
    // 注意: res/font/dot_font.ttf が存在すること
    val dotFont = FontFamily(Font(R.font.dot_font))

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // タイトル
            Text(
                text = "プロフィール設定",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = dotFont // ★フォント適用
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ニックネーム入力欄
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = onDisplayNameChanged,
                label = {
                    Text(
                        "ニックネーム",
                        fontFamily = dotFont // ★ラベルにフォント適用
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                // 入力文字自体のスタイル
                textStyle = TextStyle(
                    fontFamily = dotFont, // ★入力文字にフォント適用
                    fontSize = 18.sp
                ),
                supportingText = {
                    Text(
                        "${uiState.displayName.length}/${ProfileUiState.MAX_DISPLAY_NAME_LENGTH}",
                        fontFamily = dotFont // ★文字数カウントにフォント適用
                    )
                },
                isError = uiState.displayName.length > ProfileUiState.MAX_DISPLAY_NAME_LENGTH
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ひとこと入力欄
            OutlinedTextField(
                value = uiState.comment,
                onValueChange = onCommentChanged,
                label = {
                    Text(
                        "ひとこと（任意）",
                        fontFamily = dotFont // ★ラベルにフォント適用
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                // 入力文字自体のスタイル
                textStyle = TextStyle(
                    fontFamily = dotFont, // ★入力文字にフォント適用
                    fontSize = 18.sp
                ),
                supportingText = {
                    Text(
                        "${uiState.comment.length}/${ProfileUiState.MAX_COMMENT_LENGTH}",
                        fontFamily = dotFont // ★文字数カウントにフォント適用
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 次へボタン
            Button(
                onClick = onNavigateToTags,
                enabled = uiState.displayName.isNotBlank() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                // ★ ここに追加！RadarScreenと同じ書き方です
                colors = ButtonDefaults.buttonColors(
                    // 背景を secondary (緑) にする
                    containerColor = MaterialTheme.colorScheme.secondary,
                    // 中身（文字）を onSecondary (白) にする
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    // 無効時の色も指定しておくと丁寧
                    disabledContainerColor = androidx.compose.ui.graphics.Color.Gray,
                    disabledContentColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                if (uiState.isLoading) {
                    // 背景が緑になるので、グルグルは白 (onSecondary) にする
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                } else {
                    Text(
                        "次へ",
                        fontFamily = dotFont,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileSetupScreenPreview() {
    EnCounterTheme {
        ProfileSetupScreenContent(
            uiState = ProfileUiState(),
            onDisplayNameChanged = {},
            onCommentChanged = {},
            onNavigateToTags = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileSetupScreenFilledPreview() {
    EnCounterTheme {
        ProfileSetupScreenContent(
            uiState = ProfileUiState(
                displayName = "久米",
                comment = "よろしくお願いします"
            ),
            onDisplayNameChanged = {},
            onCommentChanged = {},
            onNavigateToTags = {}
        )
    }
}
