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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encounter.app.ui.theme.EnCounterTheme

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
            // TODO: 昆野 - タイトルのデザインを改善
            Text(
                text = "プロフィール設定",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // ========================================
            // 久米実装: value, onValueChangeは変更禁止
            // 昆野担当: TextFieldのデザインは変更可能
            // ========================================
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = onDisplayNameChanged,
                label = { Text("ニックネーム") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    // TODO: 昆野 - 文字数表示のデザインを改善
                    Text("${uiState.displayName.length}/${ProfileUiState.MAX_DISPLAY_NAME_LENGTH}")
                },
                isError = uiState.displayName.length > ProfileUiState.MAX_DISPLAY_NAME_LENGTH
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ========================================
            // 久米実装: value, onValueChangeは変更禁止
            // 昆野担当: TextFieldのデザインは変更可能
            // ========================================
            OutlinedTextField(
                value = uiState.comment,
                onValueChange = onCommentChanged,
                label = { Text("ひとこと（任意）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    // TODO: 昆野 - 文字数表示のデザインを改善
                    Text("${uiState.comment.length}/${ProfileUiState.MAX_COMMENT_LENGTH}")
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // ========================================
            // 久米実装: onClick, enabledは変更禁止
            // 昆野担当: ボタンのデザインは変更可能
            // ========================================
            Button(
                onClick = onNavigateToTags,
                enabled = uiState.displayName.isNotBlank() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    // TODO: 昆野 - ローディング表示を改善
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    // TODO: 昆野 - ボタンテキストのデザインを改善
                    Text("次へ")
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
