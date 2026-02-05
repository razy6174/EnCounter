package com.encounter.app.ui.screens.radar

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.hilt.navigation.compose.hiltViewModel
import com.encounter.app.ui.theme.EnCounterTheme

/**
 * レーダー画面（外側）
 * 周囲のユーザーをレーダー表示
 * 
 * 担当: 昆野（Frontend）- UI実装
 * ViewModel連携: 久米（Backend）- 実装済み
 * 
 * 注意: 以下のセクションは久米が実装済みのため変更禁止
 * - ViewModelの取得（hiltViewModel）
 * - uiState/uiEventの監視
 * - 権限リクエストの実装
 * - ViewModel関数の呼び出し（onClick内）
 */
@Composable
fun RadarScreen(
    onNavigateToMatchList: (detectedUids: String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    // ========================================
    // 久米実装: 変更禁止
    // ========================================
    viewModel: RadarViewModel = hiltViewModel()
) {
    // ========================================
    // 久米実装: 変更禁止（状態監視）
    // ========================================
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // ========================================
    // 久米実装: 変更禁止（権限リクエスト）
    // ========================================
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        val permanentlyDenied = permissions.entries.any { (permission, granted) ->
            !granted && !shouldShowRequestPermissionRationale(context as Activity, permission)
        }
        viewModel.onPermissionResult(allGranted, permanentlyDenied)
    }
    
    // ========================================
    // 久米実装: 変更禁止（UIイベント監視）
    // ========================================
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is RadarUiEvent.RequestPermissions -> {
                    permissionLauncher.launch(viewModel.getRequiredPermissions())
                }
                is RadarUiEvent.NavigateToSettings -> {
                    // TODO: 昆野 - 設定画面への誘導ダイアログを実装
                    snackbarHostState.showSnackbar("設定画面から権限を許可してください")
                }
                is RadarUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
    
    // 内側のContent関数を呼び出す
    RadarScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateToMatchList = { 
            // detectedDevicesをカンマ区切り文字列に変換して渡す
            val uidsString = uiState.detectedDevices.joinToString(",")
            onNavigateToMatchList(uidsString)
        },
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToHelp = onNavigateToHelp,
        onToggleEncounter = { viewModel.toggleEncounter() },
        onClearDetectedDevices = { viewModel.clearDetectedDevices() }
    )
}

/**
 * レーダー画面のコンテンツ（内側）
 * 状態を引数で受け取るため、Previewが可能
 * 
 * 昆野担当: 以下は自由に編集可能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreenContent(
    uiState: RadarUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateToMatchList: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onToggleEncounter: () -> Unit,
    onClearDetectedDevices: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EnCounter") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "プロフィール")
                    }
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(Icons.Default.Menu, contentDescription = "メニュー")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToMatchList) {
                // TODO: 昆野 - リストアイコンに変更
                Text("📋")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TODO: 昆野 - レーダーアニメーションを実装
                // 円形のレーダーアニメーション（回転するスキャンライン）
                Text(
                    text = "🎯",
                    style = MaterialTheme.typography.displayLarge
                )
                
                // TODO: 昆野 - デザインを調整（色、サイズ、フォントなど）
                Text(
                    text = "検知数: ${uiState.detectedDeviceCount}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // TODO: 昆野 - ステータス表示のデザインを改善
                Text(
                    text = when {
                        !uiState.isBluetoothEnabled -> "Bluetoothをオンにしてください"
                        uiState.isEncounterActive -> "すれ違い通信中..."
                        else -> "待機中"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // ========================================
                // 久米実装: ボタンのonClickは変更禁止
                // 昆野担当: ボタンのデザイン（色、形、サイズ）は変更可能
                // すれ違い通信ボタン（スキャン+アドバタイズ同時実行）
                // ========================================
                Button(
                    onClick = onToggleEncounter
                ) {
                    // TODO: 昆野 - ボタンデザインを改善（アイコン追加など）
                    Text(if (uiState.isEncounterActive) "すれ違い通信停止" else "すれ違い通信開始")
                }
                
                // ========================================
                // 久米実装: ボタンのonClickは変更禁止
                // 昆野担当: ボタンのデザイン（色、形、サイズ）は変更可能
                // ========================================
                Button(
                    onClick = onClearDetectedDevices
                ) {
                    // TODO: 昆野 - ボタンデザインを改善
                    Text("リストクリア")
                }
                
                // TODO: 昆野 - 検知したデバイスのリスト表示を実装
                // uiState.detectedDevices を使用してリスト表示
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RadarScreenPreview() {
    EnCounterTheme {
        RadarScreenContent(
            uiState = RadarUiState(
                isBluetoothEnabled = true,
                isScanning = true,
                isAdvertising = true,
                detectedDeviceCount = 3
            ),
            onNavigateToMatchList = {},
            onNavigateToProfile = {},
            onNavigateToHelp = {},
            onToggleEncounter = {},
            onClearDetectedDevices = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RadarScreenIdlePreview() {
    EnCounterTheme {
        RadarScreenContent(
            uiState = RadarUiState(
                isBluetoothEnabled = true,
                isScanning = false,
                isAdvertising = false,
                detectedDeviceCount = 0
            ),
            onNavigateToMatchList = {},
            onNavigateToProfile = {},
            onNavigateToHelp = {},
            onToggleEncounter = {},
            onClearDetectedDevices = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RadarScreenBluetoothOffPreview() {
    EnCounterTheme {
        RadarScreenContent(
            uiState = RadarUiState(
                isBluetoothEnabled = false
            ),
            onNavigateToMatchList = {},
            onNavigateToProfile = {},
            onNavigateToHelp = {},
            onToggleEncounter = {},
            onClearDetectedDevices = {}
        )
    }
}
