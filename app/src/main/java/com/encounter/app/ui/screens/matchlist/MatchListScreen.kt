package com.encounter.app.ui.screens.matchlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
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
 * すれちがいリスト画面（外側）
 * 検知したユーザーの一覧を表示
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
fun MatchListScreen(
    onNavigateToUserDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    // ========================================
    // 久米実装: 変更禁止
    // ========================================
    viewModel: MatchListViewModel = hiltViewModel()
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
                is MatchListUiEvent.NavigateToUserDetail -> {
                    onNavigateToUserDetail(event.userId)
                }
                is MatchListUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
    
    // 内側のContent関数を呼び出す
    MatchListScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onUserClick = { userId -> viewModel.onUserClick(userId) },
        onRefresh = { viewModel.refreshUsers() },
        onNavigateBack = onNavigateBack
    )
}

/**
 * すれちがいリスト画面のコンテンツ（内側）
 * 状態を引数で受け取るため、Previewが可能
 * 
 * 昆野担当: 以下は自由に編集可能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchListScreenContent(
    uiState: MatchListUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onUserClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                // TODO: 昆野 - タイトルのデザインを改善
                title = { Text("すれちがいリスト") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
                // TODO: 昆野 - リフレッシュボタンを追加（onRefreshを呼び出す）
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
            uiState.users.isEmpty() -> {
                // TODO: 昆野 - 空状態のデザインを改善（イラストなど）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "まだすれ違った人がいません",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "レーダーをONにして歩いてみましょう",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    items(uiState.users, key = { it.uid }) { user ->
                        // ========================================
                        // 久米実装: onClickは変更禁止
                        // 昆野担当: カードのデザインは変更可能
                        // ========================================
                        MatchUserCard(
                            user = user,
                            onClick = { onUserClick(user.uid) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * ユーザーカードコンポーネント
 * 
 * 昆野担当: デザインは自由に変更可能
 */
@Composable
fun MatchUserCard(
    user: User,
    onClick: () -> Unit
) {
    // TODO: 昆野 - カードのデザインを改善（アバター、ステータスバッジなど）
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleMedium
            )
            if (user.tags.isNotEmpty()) {
                Text(
                    text = user.tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (user.comment.isNotEmpty()) {
                Text(
                    text = user.comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MatchListScreenPreview() {
    EnCounterTheme {
        MatchListScreenContent(
            uiState = MatchListUiState(
                users = listOf(
                    User(
                        uid = "1",
                        displayName = "山田太郎",
                        comment = "よろしくお願いします！",
                        tags = listOf("Android", "Kotlin"),
                        status = UserStatus.OPEN
                    ),
                    User(
                        uid = "2",
                        displayName = "佐藤花子",
                        comment = "サウナ好き",
                        tags = listOf("サウナ", "Java"),
                        status = UserStatus.BUSY
                    )
                )
            ),
            onUserClick = {},
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MatchListScreenEmptyPreview() {
    EnCounterTheme {
        MatchListScreenContent(
            uiState = MatchListUiState(),
            onUserClick = {},
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MatchListScreenLoadingPreview() {
    EnCounterTheme {
        MatchListScreenContent(
            uiState = MatchListUiState(isLoading = true),
            onUserClick = {},
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}
