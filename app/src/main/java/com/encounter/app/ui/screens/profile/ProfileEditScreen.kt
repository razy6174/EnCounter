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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encounter.app.domain.model.UserStatus
import com.encounter.app.ui.theme.EnCounterTheme

/**
 * プロフィール編集画面（外側）
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
fun ProfileEditScreen(
    onNavigateBack: () -> Unit,
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
    // 久米実装: 変更禁止（初期読み込み）
    // ========================================
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }
    
    // ========================================
    // 久米実装: 変更禁止（UIイベント監視）
    // ========================================
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ProfileUiEvent.NavigateBack -> onNavigateBack()
                is ProfileUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> { /* 他のイベントはこの画面では処理しない */ }
            }
        }
    }
    
    // 内側のContent関数を呼び出す
    ProfileEditScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onDisplayNameChanged = { viewModel.onDisplayNameChanged(it) },
        onCommentChanged = { viewModel.onCommentChanged(it) },
        onStatusChanged = { viewModel.onStatusChanged(it) },
        onTagToggled = { viewModel.onTagToggled(it) },
        onSaveProfile = { viewModel.saveProfile() }
    )
}

/**
 * プロフィール編集画面のコンテンツ（内側）
 * 状態を引数で受け取るため、Previewが可能
 * 
 * 昆野担当: 以下は自由に編集可能
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("プロフィール編集") },
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
        // 久米実装: isLoading条件は変更禁止
        // 昆野担当: ローディング表示のデザインは変更可能
        // ========================================
        if (uiState.isLoading && !uiState.isEditMode) {
            // TODO: 昆野 - ローディング表示を改善
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("読み込み中...")
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
                    }
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // TODO: 昆野 - ステータス選択のデザインを改善
                Text(
                    text = "ステータス",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // ========================================
                // 久米実装: selected, onClickは変更禁止
                // 昆野担当: RadioButtonのデザインは変更可能
                // ========================================
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
                                text = when (status) {
                                    UserStatus.WANTED -> "話したい"
                                    UserStatus.BUSY -> "忙しい"
                                    UserStatus.OFFLINE -> "オフライン"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // TODO: 昆野 - タグ選択のデザインを改善
                Text(
                    text = "興味タグ（${ProfileUiState.MIN_TAGS}〜${ProfileUiState.MAX_TAGS}個）",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // ========================================
                // 久米実装: AvailableTags.list, selectedTags, onTagToggledは変更禁止
                // 昆野担当: FilterChipのデザインは変更可能
                // ========================================
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
                                // TODO: 昆野 - タグラベルのデザインを改善
                                Text("#${tag.label}") 
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // TODO: 昆野 - 選択数表示のデザインを改善
                Text(
                    text = "選択中: ${uiState.selectedTags.size}/${ProfileUiState.MAX_TAGS}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // ========================================
                // 久米実装: onClick, enabledは変更禁止
                // 昆野担当: ボタンのデザインは変更可能
                // ========================================
                Button(
                    onClick = onSaveProfile,
                    enabled = uiState.isSaveEnabled && !uiState.isLoading,
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
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileEditScreenPreview() {
    EnCounterTheme {
        ProfileEditScreenContent(
            uiState = ProfileUiState(
                displayName = "久米",
                comment = "よろしくお願いします",
                selectedTags = setOf("it", "game", "music"),
                status = UserStatus.WANTED,
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

@Preview(showBackground = true)
@Composable
private fun ProfileEditScreenLoadingPreview() {
    EnCounterTheme {
        ProfileEditScreenContent(
            uiState = ProfileUiState(isLoading = true),
            onNavigateBack = {},
            onDisplayNameChanged = {},
            onCommentChanged = {},
            onStatusChanged = {},
            onTagToggled = {},
            onSaveProfile = {}
        )
    }
}
