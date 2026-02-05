package com.encounter.app.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encounter.app.domain.model.Message
import com.encounter.app.ui.theme.EnCounterTheme
import com.encounter.app.ui.utils.rememberSafeNavigateBack
import kotlinx.coroutines.flow.collectLatest

/**
 * チャット画面（外側）
 */
@Composable
fun ChatScreen(
    roomId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val safeNavigateBack = rememberSafeNavigateBack(onNavigateBack)

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ChatUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ChatUiEvent.ScrollToBottom -> {
                    // Content内で処理するためここでは何もしない
                }
            }
        }
    }

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
 * 修正: キーボードのアニメーション（Paddingの変化）に追従してスクロールを行うロジックを実装
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreenContent(
    uiState: ChatUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // キーボード（IME）の状態取得
    val isImeVisible = WindowInsets.isImeVisible

    // 「キーボードが開く前、ユーザーは一番下にいたか？」を記録するフラグ
    var wasAtBottomBeforeIme by remember { mutableStateOf(true) }

    // 1. 常時監視: ユーザーが現在一番下を見ているかどうかを判定
    // キーボードが閉じている時だけフラグを更新し、開いている間は「開く前の状態」を維持する
    LaunchedEffect(listState, uiState.messages.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@snapshotFlow true

            // 最後のアイテムが表示領域に入っているかチェック
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index == totalItems - 1
        }.collectLatest { isAtBottom ->
            if (!isImeVisible) {
                wasAtBottomBeforeIme = isAtBottom
            }
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
            // 入力欄エリア
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        // ここでimePaddingを入れることでScaffoldのpaddingValuesにIMEの高さが反映される
                        .imePadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = onInputTextChanged,
                        placeholder = { Text("メッセージを入力") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !uiState.isSending,
                        shape = CircleShape,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    IconButton(
                        onClick = onSendMessage,
                        enabled = uiState.inputText.isNotBlank() && !uiState.isSending,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "送信",
                                tint = if (uiState.inputText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // paddingValues.calculateBottomPadding() はキーボードのアニメーション中、
        // 0dp -> 300dp のように連続的に変化します。
        val bottomPadding = paddingValues.calculateBottomPadding()

        // 2. 核心のアルゴリズム実装:
        // キーボードが表示中（isImeVisible）かつ、元々一番下にいた（wasAtBottomBeforeIme）場合、
        // パディングが変化するたびに（bottomPaddingをキーにして）最下部へスクロールし続ける。
        // これにより、キーボードの動きにチャットが完全に同期して押し上がります。
        LaunchedEffect(bottomPadding) {
            if (isImeVisible && wasAtBottomBeforeIme && uiState.messages.isNotEmpty()) {
                listState.scrollToItem(uiState.messages.size - 1)
            }
        }

        // 3. メッセージ送信時の自動スクロール（通常通り）
        LaunchedEffect(uiState.messages.size) {
            if (uiState.messages.isNotEmpty()) {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }

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
                // ScaffoldのpaddingValues（IMEの高さ含む）を適用
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
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

// ... 以下 MessageBubble, Preview 等は既存通り ...
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
                    Message("1", "user2", "こんにちは！", System.currentTimeMillis()),
                    Message("2", "user1", "こんにちは！よろしくお願いします", System.currentTimeMillis())
                )
            ),
            onInputTextChanged = {},
            onSendMessage = {},
            onNavigateBack = {}
        )
    }
}