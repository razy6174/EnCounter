package com.encounter.app.ui.screens.matchlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encounter.app.domain.model.User
import com.encounter.app.domain.model.UserStatus
import com.encounter.app.ui.theme.EnCounterTheme

/**
 * すれちがい図鑑画面（外側）
 * 今まですれ違った人の履歴を表示
 * 
 * 担当: 昆野（Frontend）- UI実装
 * ViewModel連携: 久米（Backend）- 実装済み
 */
@Composable
fun MatchListScreen(
    onNavigateToUserDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MatchListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
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
    
    MatchListScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onUserClick = { userId -> viewModel.onUserClick(userId) },
        onDeleteUser = { uidPrefix -> viewModel.deleteEncounter(uidPrefix) },
        onDeleteAllClick = { viewModel.showDeleteAllConfirmDialog() },
        onDeleteAllConfirm = { viewModel.clearAllHistory() },
        onDeleteAllDismiss = { viewModel.hideDeleteAllConfirmDialog() },
        onRefresh = { viewModel.refreshUsers() },
        onNavigateBack = onNavigateBack
    )
}

/**
 * すれちがい図鑑画面のコンテンツ（内側）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchListScreenContent(
    uiState: MatchListUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onUserClick: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    onDeleteAllClick: () -> Unit,
    onDeleteAllConfirm: () -> Unit,
    onDeleteAllDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // 全削除確認ダイアログ
    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDeleteAllDismiss,
            title = { Text("すべて削除しますか？") },
            text = { Text("すれちがい図鑑のすべての記録が削除されます。この操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = onDeleteAllConfirm) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteAllDismiss) {
                    Text("キャンセル")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("すれちがい図鑑") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    if (uiState.filteredUsers.isNotEmpty()) {
                        IconButton(onClick = onDeleteAllClick) {
                            Icon(Icons.Default.Delete, contentDescription = "全削除")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.filteredUsers.isEmpty() -> {
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
                    items(uiState.filteredUsers, key = { it.uid }) { user ->
                        MatchUserCard(
                            user = user,
                            onClick = { onUserClick(user.uid) },
                            onDelete = { onDeleteUser(user.uidPrefix) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * ユーザーカードコンポーネント（削除ボタン付き）
 */
@Composable
fun MatchUserCard(
    user: User,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ステータス絵文字
            Text(
                text = user.status.emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            // ユーザー情報
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (user.tags.isNotEmpty()) {
                    Text(
                        text = user.tags.take(3).joinToString(" ") { "#$it" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (user.comment.isNotEmpty()) {
                    Text(
                        text = user.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            
            // 削除ボタン
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                filteredUsers = listOf(
                    User(
                        uid = "1",
                        uidPrefix = "prefix1",
                        displayName = "山田太郎",
                        comment = "よろしくお願いします！",
                        tags = listOf("Android", "Kotlin"),
                        status = UserStatus.OPEN
                    ),
                    User(
                        uid = "2",
                        uidPrefix = "prefix2",
                        displayName = "佐藤花子",
                        comment = "サウナ好き",
                        tags = listOf("サウナ", "Java"),
                        status = UserStatus.BUSY
                    )
                )
            ),
            onUserClick = {},
            onDeleteUser = {},
            onDeleteAllClick = {},
            onDeleteAllConfirm = {},
            onDeleteAllDismiss = {},
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
            onDeleteUser = {},
            onDeleteAllClick = {},
            onDeleteAllConfirm = {},
            onDeleteAllDismiss = {},
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
            onDeleteUser = {},
            onDeleteAllClick = {},
            onDeleteAllConfirm = {},
            onDeleteAllDismiss = {},
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}
