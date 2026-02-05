package com.encounter.app.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.encounter.app.domain.model.Message
import com.encounter.app.ui.theme.EnCounterTheme
import com.encounter.app.ui.utils.rememberSafeNavigateBack

/**
 * チャット画面（外側）
 * ViewModelを使用してStateを取得し、Content関数に渡す
 * 
 * 担当: 昆野（Frontend）- UI実装
 * ViewModel連携: 久米（Backend）- 実装済み
 * 
 * 注意: 以下のセクションは久米が実装済みのため変更禁止
 * - ViewModelの取得（hiltViewModel）
 * - uiState/uiEventの監視
 * - ViewModel関数の呼び出し
 */
@Composable
fun ChatScreen(
    roomId: String,
    onNavigateBack: () -> Unit,
    // ========================================
    // 久米実装: 変更禁止
    // ========================================
    viewModel: ChatViewModel = hiltViewModel()
) {
    // ========================================
    // 久米実装: 変更禁止（状態監視）
    // ========================================
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 二重タップ防止付きの安全な戻るナビゲーション
    val safeNavigateBack = rememberSafeNavigateBack(onNavigateBack)
    
    // ========================================
    // 久米実装: 変更禁止（UIイベント監視）
    // ========================================
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ChatUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ChatUiEvent.ScrollToBottom -> {
                    // ScrollToBottomイベントはContent内で処理
                }
            }
        }
    }
    
    // 内側のContent関数を呼び出す
    ChatScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onInputTextChanged = { viewModel.onInputTextChanged(it) },
        onSendMessage = { viewModel.sendMessage() },
        onNavigateBack = safeNavigateBack
    )
}

/**
 * チャット画面のコンテンツ（内側）
 * 状態を引数で受け取るため、Previewが可能
 * 
 * 昆野担当: 以下は自由に編集可能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenContent(
    uiState: ChatUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val listState = rememberLazyListState()
    
    // メッセージが更新されたら最下部にスクロール
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(uiState.partnerUser?.displayName ?: "チャット") 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = onInputTextChanged,
                    placeholder = { Text("メッセージを入力") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !uiState.isSending
                )
                IconButton(
                    onClick = onSendMessage,
                    enabled = uiState.inputText.isNotBlank() && !uiState.isSending
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator()
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "送信")
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { message -> message.messageId }
                ) { message ->
                    MessageBubble(
                        message = message,
                        isMe = message.senderId == uiState.myUserId
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isMe) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    EnCounterTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                roomId = "room123",
                myUserId = "user1",
                partnerUser = com.encounter.app.domain.model.User(
                    uid = "user2",
                    displayName = "山田太郎"
                ),
                messages = listOf(
                    Message(
                        messageId = "1",
                        senderId = "user2",
                        text = "こんにちは！",
                        createdAt = System.currentTimeMillis()
                    ),
                    Message(
                        messageId = "2",
                        senderId = "user1",
                        text = "こんにちは！よろしくお願いします",
                        createdAt = System.currentTimeMillis()
                    ),
                    Message(
                        messageId = "3",
                        senderId = "user2",
                        text = "Kotlinお使いなんですね！",
                        createdAt = System.currentTimeMillis()
                    )
                )
            ),
            onInputTextChanged = {},
            onSendMessage = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenLoadingPreview() {
    EnCounterTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                isLoading = true
            ),
            onInputTextChanged = {},
            onSendMessage = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenEmptyPreview() {
    EnCounterTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                roomId = "room123",
                myUserId = "user1",
                partnerUser = com.encounter.app.domain.model.User(
                    uid = "user2",
                    displayName = "佐藤花子"
                ),
                messages = emptyList()
            ),
            onInputTextChanged = {},
            onSendMessage = {},
            onNavigateBack = {}
        )
    }
}
