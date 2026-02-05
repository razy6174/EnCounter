package com.encounter.app.ui.screens.radar

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.hilt.navigation.compose.hiltViewModel
import com.encounter.app.ui.theme.EnCounterTheme

/**
 * レーダー画面（ホーム）
 * 周囲のユーザーをレーダー表示
 * 
 * 担当: 昆野（Frontend）- UI実装
 * ViewModel連携: 久米（Backend）- 実装済み
 * 
 * ⚠️ 注意: 以下のセクションは久米が実装済みのため変更禁止
 * - ViewModelの取得（hiltViewModel）
 * - uiState/uiEventの監視
 * - 権限リクエストの実装
 * - ViewModel関数の呼び出し（onClick内）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    onNavigateToMatchList: (detectedUids: String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    // ========================================
    // 🔒 久米実装: 変更禁止
    // ========================================
    viewModel: RadarViewModel = hiltViewModel()
) {
    // ========================================
    // 🔒 久米実装: 変更禁止（状態監視）
    // ========================================
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // ========================================
    // 🔒 久米実装: 変更禁止（権限リクエスト）
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
    // 🔒 久米実装: 変更禁止（UIイベント監視）
    // ========================================
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is RadarUiEvent.RequestPermissions -> {
                    permissionLauncher.launch(viewModel.getRequiredPermissions())
                }
                is RadarUiEvent.NavigateToSettings -> {
                    snackbarHostState.showSnackbar("設定画面から権限を許可してください")
                }
                is RadarUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // UIコンポーネントへ描画を委譲
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
        // 修正: Backendのメソッド名(toggleScanning)を使用しつつ、UI上の意味は「すれ違い通信」とする
        onToggleEncounter = { viewModel.toggleScanning() },
        onClearDetectedDevices = { viewModel.clearDetectedDevices() }
    )
}

// ==============================================================================
// 🎨 UI実装: UI担当 (昆野さんの実装エリア)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreenContent(
    uiState: RadarUiState,
    snackbarHostState: SnackbarHostState,
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
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "リスト")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // -----------------------------------------------------------
            // 上部：レーダー表示エリア (画面の60%)
            // -----------------------------------------------------------
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                // 修正: 変数名は isScanning を使用
                if (uiState.isScanning) {
                    RadarAnimationBackground()
                }

                // 中央の情報表示
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // アイコン
                    Surface(
                        shape = CircleShape,
                        // 修正: 変数名は isScanning を使用
                        color = if (uiState.isScanning) MaterialTheme.colorScheme.primaryContainer else Color.LightGray,
                        modifier = Modifier.size(80.dp),
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                // 修正: 変数名は isScanning を使用
                                imageVector = if (uiState.isScanning) Icons.Default.Search else Icons.Default.Settings,
                                contentDescription = "Status",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // 検知数
                    Text(
                        text = "${uiState.detectedDeviceCount}人を検知",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // ステータスチップ
                    StatusChip(
                        isBluetoothEnabled = uiState.isBluetoothEnabled,
                        // 修正: 変数名は isScanning を使用
                        isEncounterActive = uiState.isScanning
                    )
                }
            }

            // -----------------------------------------------------------
            // 下部：コントロール & リストエリア (画面の40%)
            // -----------------------------------------------------------
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 操作ボタン (中央揃え・単一ボタン)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ActionButton(
                        // 修正: 変数名は isScanning だが、表示名は「すれ違い通信」にする
                        text = if (uiState.isScanning) "すれ違い通信停止" else "すれ違い通信開始",
                        icon = if (uiState.isScanning) Icons.Default.Clear else Icons.Default.Search,
                        isActive = uiState.isScanning,
                        onClick = onToggleEncounter
                    )
                }

                // リストヘッダー & クリアボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "検出デバイス一覧",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onClearDetectedDevices) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("クリア")
                    }
                }

                // リスト表示
                DeviceList(devices = uiState.detectedDevices.toList())
            }
        }
    }
}

// ==============================================================================
// 🛠️ UIコンポーネント部品
// ==============================================================================

@Composable
fun StatusChip(isBluetoothEnabled: Boolean, isEncounterActive: Boolean) {
    val (text, color) = when {
        !isBluetoothEnabled -> "Bluetooth OFF" to Color.Red
        isEncounterActive -> "すれ違い通信中" to Color(0xFF4CAF50) // Green
        else -> "待機中" to Color.Gray
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun DeviceList(devices: List<Any>) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(devices) { device ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Unknown Device",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = device.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RadarAnimationBackground() {
    Box(contentAlignment = Alignment.Center) {
        RadarRipple(delayMillis = 0)
        RadarRipple(delayMillis = 1000)
        RadarRipple(delayMillis = 2000)
    }
}

@Composable
fun RadarRipple(delayMillis: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "alpha"
    )
    Canvas(modifier = Modifier.size(100.dp)) {
        drawCircle(color = Color.Gray, radius = size.minDimension / 2 * scale, alpha = alpha)
    }
}

// ==============================================================================
// 🖥️ プレビュー (ダミーデータ)
// ==============================================================================
@Preview(showBackground = true, name = "通信中")
@Composable
fun RadarScreenActivePreview() {
    EnCounterTheme {
        RadarScreenContent(
            // 修正: エラーの原因だった isEncounterActive を isScanning に戻しました
            uiState = RadarUiState(
                isScanning = true,
                isAdvertising = true,
                isBluetoothEnabled = true,
                detectedDeviceCount = 3,
                detectedDevices = setOf("UserA", "UserB", "UserC")
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateToMatchList = {},
            onNavigateToProfile = {},
            onNavigateToHelp = {},
            onToggleEncounter = {},
            onClearDetectedDevices = {}
        )
    }
}