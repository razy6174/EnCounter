package com.encounter.app.ui.screens.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * デバッグ画面
 * デモ・プレゼン用の隠し機能を提供
 * 
 * 担当: 久米（Backend）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // UIイベントを監視
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DebugUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is DebugUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar("エラー: ${event.message}")
                }
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🔧 デバッグメニュー") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ログ設定セクション
            DebugSection(title = "📝 ログ設定") {
                SwitchRow(
                    label = "デバッグログ出力",
                    checked = uiState.isDebugLogEnabled,
                    onCheckedChange = { viewModel.toggleDebugLog() }
                )
                Text(
                    text = "Logcatへのデバッグログ出力を制御します",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 強制検知モードセクション
            DebugSection(title = "📡 強制検知モード") {
                SwitchRow(
                    label = "強制検知モード",
                    checked = uiState.isForceDetectionMode,
                    onCheckedChange = { viewModel.toggleForceDetectionMode() }
                )
                Text(
                    text = "BLE通信なしでUID検知をシミュレートします。" +
                            "プレゼン時にBLEが不安定な場合に使用します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (uiState.isForceDetectionMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "検知中のデバイス: ${uiState.forceDetectedDevices.size}件",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (uiState.forceDetectedDevices.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { viewModel.clearForceDetectedDevices() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("すべてクリア")
                        }
                    }
                }
            }
            
            // ダミーユーザー生成セクション
            DebugSection(title = "👥 ダミーユーザー") {
                Text(
                    text = "デモ・プレゼン用のダミーユーザーを生成します。" +
                            "5人のテストユーザーがFirestoreに作成されます。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { viewModel.createDummyUsers() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isCreatingDummyUsers
                ) {
                    if (uiState.isCreatingDummyUsers) {
                        CircularProgressIndicator()
                    } else {
                        Text("ダミーユーザーを生成")
                    }
                }
                
                if (uiState.createdDummyUserIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✅ ${uiState.createdDummyUserIds.size}人のダミーユーザーを作成済み",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // 強制検知モードが有効な場合のみ表示
                    if (uiState.isForceDetectionMode) {
                        Button(
                            onClick = { viewModel.addAllDummyUsers() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ダミーユーザーを検知リストに追加")
                        }
                    }
                }
            }
            
            // 使い方セクション
            DebugSection(title = "📖 使い方") {
                Text(
                    text = """
                        1. 「ダミーユーザーを生成」でテストユーザーを作成
                        2. 「強制検知モード」を有効化
                        3. 「ダミーユーザーを検知リストに追加」をタップ
                        4. レーダー画面に戻ると、ダミーユーザーが検知済みとして表示されます
                        
                        ⚠️ この機能は開発・デモ専用です。
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DebugSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
