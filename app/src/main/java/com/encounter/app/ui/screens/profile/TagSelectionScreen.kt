package com.encounter.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
 * タグ選択画面（外側）
 * 
 * 担当: 昆野（Frontend）- UI実装
 * ViewModel連携: 久米（Backend）- 実装済み
 * 
 * 注意: 以下のセクションは久米が実装済みのため変更禁止
 * - ViewModelの取得（hiltViewModel）
 * - uiState/uiEventの監視
 * - ViewModel関数の呼び出し（onClick内）
 * - AvailableTagsの使用
 */
@Composable
fun TagSelectionScreen(
    onNavigateToRadar: () -> Unit,
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
                is ProfileUiEvent.NavigateToRadar -> onNavigateToRadar()
                is ProfileUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> { /* 他のイベントはこの画面では処理しない */ }
            }
        }
    }
    
    // 内側のContent関数を呼び出す
    TagSelectionScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onTagToggled = { viewModel.onTagToggled(it) },
        onSaveProfile = { viewModel.saveProfile() }
    )
}

/**
 * タグ選択画面のコンテンツ（内側）
 * 状態を引数で受け取るため、Previewが可能
 * 
 * 昆野担当: 以下は自由に編集可能
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSelectionScreenContent(
    uiState: ProfileUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onTagToggled: (String) -> Unit,
    onSaveProfile: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TODO: 昆野 - タイトルのデザインを改善
            Text(
                text = "興味タグを選択",
                style = MaterialTheme.typography.headlineMedium
            )
            
            // TODO: 昆野 - サブタイトルのデザインを改善
            Text(
                text = "あなたの興味や属性を選んでください（${ProfileUiState.MIN_TAGS}〜${ProfileUiState.MAX_TAGS}個）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
            
            Spacer(modifier = Modifier.weight(1f))
            
            // TODO: 昆野 - 選択数表示のデザインを改善
            Text(
                text = "選択中: ${uiState.selectedTags.size}/${ProfileUiState.MAX_TAGS}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.selectedTags.size >= ProfileUiState.MIN_TAGS) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
                    Text("はじめる")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagSelectionScreenPreview() {
    EnCounterTheme {
        TagSelectionScreenContent(
            uiState = ProfileUiState(
                displayName = "久米",
                selectedTags = setOf("it", "game")
            ),
            onTagToggled = {},
            onSaveProfile = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagSelectionScreenEmptyPreview() {
    EnCounterTheme {
        TagSelectionScreenContent(
            uiState = ProfileUiState(displayName = "久米"),
            onTagToggled = {},
            onSaveProfile = {}
        )
    }
}
