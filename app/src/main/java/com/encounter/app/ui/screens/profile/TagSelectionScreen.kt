package com.encounter.app.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encounter.app.R
import com.encounter.app.ui.theme.EnCounterTheme

/**
 * タグ選択画面（外側）
 */
@Composable
fun TagSelectionScreen(
    onNavigateToRadar: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    TagSelectionScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onTagToggled = { viewModel.onTagToggled(it) },
        onSaveProfile = { viewModel.saveProfile() }
    )
}

/**
 * タグ選択画面のコンテンツ（内側）
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagSelectionScreenContent(
    uiState: ProfileUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onTagToggled: (String) -> Unit,
    onSaveProfile: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // スクロール可能なコンテンツエリア
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // === 看板エリア（タイトル ＋ サブタイトル） ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp), // 2行入るように高さを確保
                    contentAlignment = Alignment.Center
                ) {
                    // 1. 背景画像 (吊り下げ看板)
                    Image(
                        painter = painterResource(id = R.drawable.header_board),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds // 枠に合わせて引き伸ばす
                    )

                    // 2. テキスト部分（Columnで縦に並べる）
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        // 看板の「紐」や「枠」に文字が被らないよう、パディングで位置調整
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    ) {
                        // タイトル
                        Text(
                            text = "興味タグを選択",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily(Font(R.font.dot_font))
                            ),
                            // ▼ 修正: Theme.kt の onSurface を使用
                            color = Color(0xFF3E2723),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp)) // 間隔を少し詰める

                        // サブタイトル
                        Text(
                            text = "あなたの興味ある趣味を選んでください",
                            style = MaterialTheme.typography.bodySmall,
                            // ▼ 修正: Theme.kt の onSurface を使用（不透明度はお好みで）
                            color = Color(0xFF3E2723),
                            textAlign = TextAlign.Center
                        )
                    }
                }


                // === 掲示板エリア（タグリスト） ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 掲示板の角を少し丸くする
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    // 背景：木の板（縦）
                    Image(
                        painter = painterResource(id = R.drawable.wood_background),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(), // Boxのサイズに合わせる
                        contentScale = ContentScale.FillBounds // 隙間なく埋める
                    )

                    // タグリスト本体
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 画像の「枠」や「釘」に被らないよう、パディングを大きめに取る
                            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 28.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AvailableTags.list.forEach { tag ->
                            val isSelected = tag.id in uiState.selectedTags

                            FilterChip(
                                selected = isSelected,
                                onClick = { onTagToggled(tag.id) },
                                label = {
                                    Text(
                                        text = "#${tag.label}",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        // 選択時は白系、未選択時は焦げ茶色（紙の上のインク）
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            Color(0xFF3E2723)
                                    )
                                },
                                leadingIcon = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    // 未選択時は「羊皮紙」のようなクリーム色
                                    containerColor = Color(0xFFFFF3E0),
                                    labelColor = Color(0xFF3E2723)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // フッターエリア（ボタンなど）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isCountValid = uiState.selectedTags.size in ProfileUiState.MIN_TAGS..ProfileUiState.MAX_TAGS

                // カウンター表示
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    if (isCountValid) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                    }

                    Text(
                        text = "選択中: ${uiState.selectedTags.size} / ${ProfileUiState.MAX_TAGS}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCountValid) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCountValid) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Text(
                    text = "${ProfileUiState.MIN_TAGS}個以上選択すると次に進めます",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (!isCountValid && uiState.selectedTags.isNotEmpty())
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 保存ボタン
                Button(
                    onClick = onSaveProfile,
                    enabled = uiState.isSaveEnabled && !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSecondary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "はじめる",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

// プレビュー用
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun TagSelectionScreenPreview() {
    EnCounterTheme {
        TagSelectionScreenContent(
            uiState = ProfileUiState(
                displayName = "久米",
                selectedTags = setOf("it"),
                isSaveEnabled = true
            ),
            onTagToggled = {},
            onSaveProfile = {}
        )
    }
}