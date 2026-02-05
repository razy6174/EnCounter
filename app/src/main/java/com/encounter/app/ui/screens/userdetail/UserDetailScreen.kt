package com.encounter.app.ui.screens.userdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.encounter.app.domain.model.User
import com.encounter.app.domain.model.UserStatus
import com.encounter.app.ui.theme.EnCounterTheme

/**
 * ユーザー詳細画面（外側）
 * 
 * 担当: 昆野（Frontend）- UI実装
 * ViewModel連携: 久米（Backend）- 実装済み
 * 
 * 注意: 以下のセクションは久米が実装済みのため変更禁止
 * - ViewModelの取得（hiltViewModel）
 * - uiState/uiEventの監視
 * - ViewModel関数の呼び出し（onClick内）
 */
@Composable
fun UserDetailScreen(
    userId: String,
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit,
    // ========================================
    // 久米実装: 変更禁止
    // ========================================
    viewModel: UserDetailViewModel = hiltViewModel()
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
                is UserDetailUiEvent.NavigateToChat -> {
                    onNavigateToChat(event.roomId)
                }
                is UserDetailUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
    
    // 内側のContent関数を呼び出す
    UserDetailScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onStartChat = { viewModel.startChat() },
        onNavigateBack = onNavigateBack
    )
}

/**
 * ユーザー詳細画面のコンテンツ（内側）
 * 状態を引数で受け取るため、Previewが可能
 * 
 * 昆野担当: 以下は自由に編集可能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreenContent(
    uiState: UserDetailUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onStartChat: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                // TODO: 昆野 - タイトルにユーザー名を表示
                title = { Text(uiState.user?.displayName ?: "ユーザー詳細") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // ========================================
        // 久米実装: 条件分岐は変更禁止
        // 昆野担当: 各状態のUIデザインは変更可能
        // ========================================
        when {
            uiState.isLoading -> {
                // TODO: 昆野 - ローディング表示のデザインを改善
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.user == null -> {
                // TODO: 昆野 - エラー状態のデザインを改善
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ユーザー情報を取得できませんでした",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                val user = uiState.user
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // TODO: 昆野 - アバター画像を表示（将来的に実装）
                    Text(
                        text = "👤",
                        style = MaterialTheme.typography.displayLarge
                    )
                    
                    // TODO: 昆野 - ユーザー名のデザインを改善
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    // TODO: 昆野 - ステータスバッジを表示
                    Text(
                        text = "${user.status.emoji} ${user.status.displayName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // TODO: 昆野 - タグのデザインを改善（Chip等）
                    if (user.tags.isNotEmpty()) {
                        Text(
                            text = user.tags.joinToString(" ") { "#$it" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // TODO: 昆野 - コメントのデザインを改善
                    if (user.comment.isNotEmpty()) {
                        Text(
                            text = user.comment,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // ========================================
                    // 久米実装: onClickは変更禁止
                    // 昆野担当: ボタンのデザインは変更可能
                    // ========================================
                    Button(
                        onClick = onStartChat,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isStartingChat
                    ) {
                        if (uiState.isStartingChat) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            // TODO: 昆野 - ボタンのデザインを改善（アイコン追加など）
                            Text("話しかける")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserDetailScreenPreview() {
    EnCounterTheme {
        UserDetailScreenContent(
            uiState = UserDetailUiState(
                user = User(
                    uid = "1",
                    displayName = "山田太郎",
                    comment = "よろしくお願いします！",
                    tags = listOf("Android", "Kotlin", "サウナ"),
                    status = UserStatus.OPEN
                )
            ),
            onStartChat = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserDetailScreenLoadingPreview() {
    EnCounterTheme {
        UserDetailScreenContent(
            uiState = UserDetailUiState(isLoading = true),
            onStartChat = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserDetailScreenErrorPreview() {
    EnCounterTheme {
        UserDetailScreenContent(
            uiState = UserDetailUiState(
                user = null,
                error = "ユーザー情報を取得できませんでした"
            ),
            onStartChat = {},
            onNavigateBack = {}
        )
    }
}
