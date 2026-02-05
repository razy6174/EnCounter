package com.encounter.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encounter.app.R
import com.encounter.app.domain.model.UserStatus
import com.encounter.app.ui.theme.EnCounterTheme
import com.encounter.app.ui.utils.rememberSafeNavigateBack

/**
 * プロフィール編集画面（外側）
 * * 担当: 昆野（Frontend）- UI実装
 * ViewModel連携: 久米（Backend）- 実装済み
 */
@Composable
fun ProfileEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val safeNavigateBack = rememberSafeNavigateBack(onNavigateBack)

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ProfileUiEvent.NavigateBack -> safeNavigateBack()
                is ProfileUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> { }
            }
        }
    }

    ProfileEditScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = safeNavigateBack,
        onDisplayNameChanged = { viewModel.onDisplayNameChanged(it) },
        onCommentChanged = { viewModel.onCommentChanged(it) },
        onStatusChanged = { viewModel.onStatusChanged(it) },
        onTagToggled = { viewModel.onTagToggled(it) },
        onSaveProfile = { viewModel.saveProfile() }
    )
}

/**
 * プロフィール編集画面のコンテンツ（内側）
 * 昆野担当: フォント適用済み
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreenContent(
    uiState: ProfileUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onCommentChanged: (String) -> Unit,
    onStatusChanged: (UserStatus) -> Unit,
    onTagToggled: (String) -> Unit,
    onSaveProfile: () -> Unit
) {
    // ドットフォントの定義
    val dotFont = FontFamily(Font(R.font.dot_font))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "プロフィール編集",
                        fontFamily = dotFont // ★フォント適用
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading && !uiState.isEditMode) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "読み込み中...",
                    fontFamily = dotFont // ★フォント適用
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ニックネーム入力
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
                    // ★入力文字自体にフォント適用
                    textStyle = TextStyle(
                        fontFamily = dotFont,
                        fontSize = 16.sp
                    ),
                    supportingText = {
                        Text(
                            "${uiState.displayName.length}/${ProfileUiState.MAX_DISPLAY_NAME_LENGTH}",
                            fontFamily = dotFont // ★文字数カウントにフォント適用
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ひとこと入力
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
                    // ★入力文字自体にフォント適用
                    textStyle = TextStyle(
                        fontFamily = dotFont,
                        fontSize = 16.sp
                    ),
                    supportingText = {
                        Text(
                            "${uiState.comment.length}/${ProfileUiState.MAX_COMMENT_LENGTH}",
                            fontFamily = dotFont // ★文字数カウントにフォント適用
                        )
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "ステータス",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = dotFont, // ★フォント適用
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ステータス選択
                Column(modifier = Modifier.fillMaxWidth()) {
                    UserStatus.entries.forEach { status ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.status == status,
                                onClick = { onStatusChanged(status) }
                            )
                            Text(
                                text = "${status.emoji} ${status.displayName}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = dotFont // ★フォント適用
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "興味タグ（${ProfileUiState.MIN_TAGS}〜${ProfileUiState.MAX_TAGS}個）",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = dotFont, // ★フォント適用
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // タグ選択
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvailableTags.list.forEach { tag ->
                        FilterChip(
                            selected = tag.id in uiState.selectedTags,
                            onClick = { onTagToggled(tag.id) },
                            label = {
                                Text(
                                    "#${tag.label}",
                                    fontFamily = dotFont // ★フォント適用
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 選択数表示
                Text(
                    text = "選択中: ${uiState.selectedTags.size}/${ProfileUiState.MAX_TAGS}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = dotFont // ★フォント適用
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 保存ボタン
                Button(
                    onClick = onSaveProfile,
                    enabled = uiState.isSaveEnabled && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            "保存",
                            fontFamily = dotFont, // ★フォント適用
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// プレビュー等は変更なしのため省略可能ですが、必要であれば元のコードのまま維持してください
@Preview(showBackground = true)
@Composable
private fun ProfileEditScreenPreview() {
    EnCounterTheme {
        ProfileEditScreenContent(
            uiState = ProfileUiState(
                displayName = "久米",
                comment = "よろしくお願いします",
                selectedTags = setOf("it", "game", "music"),
                status = UserStatus.OPEN,
                isEditMode = true,
                isSaveEnabled = true
            ),
            onNavigateBack = {},
            onDisplayNameChanged = {},
            onCommentChanged = {},
            onStatusChanged = {},
            onTagToggled = {},
            onSaveProfile = {}
        )
    }
}