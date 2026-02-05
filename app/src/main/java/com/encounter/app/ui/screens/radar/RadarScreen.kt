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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.encounter.app.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.filled.Info

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
    onNavigateToSettings: () -> Unit = {},
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
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToHelp = onNavigateToHelp,
        // ========================================
        // 🔒 久米実装: toggleEncounter()を使用（スキャン+アドバタイズ同時実行）
        // ========================================
        onToggleEncounter = { viewModel.toggleEncounter() },
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
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onToggleEncounter: () -> Unit,
    onClearDetectedDevices: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // タイトルも見やすいように少し太字にする
                    Text(
                        "EnCounter",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    // ---------------------------------------------------
                    // 変更点: アイコンを「立体的な箱」に入れるスタイルに変更
                    // ---------------------------------------------------

                    // プロフィールボタン
                    TopBarActionButton(
                        icon = Icons.Default.Person,
                        contentDescription = "プロフィール",
                        onClick = onNavigateToProfile
                    )

                    Spacer(modifier = Modifier.width(8.dp)) // ボタン同士の間隔

                    // 設定ボタン
                    TopBarActionButton(
                        icon = Icons.Default.Settings,
                        contentDescription = "設定",
                        onClick = onNavigateToSettings
                    )

                    Spacer(modifier = Modifier.width(8.dp)) // ボタン同士の間隔

                    // メニューボタン
                    TopBarActionButton(
                        icon = Icons.Default.Info,
                        // 説明文も変更
                        contentDescription = "ヘルプ",
                        onClick = onNavigateToHelp
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToMatchList,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_list_log), // リソース名は前回設定したもの
                    contentDescription = "冒険の記録を見る",
                    modifier = Modifier
                        .size(50.dp)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // ... (中身は変更なしのため省略。以前のコードのままでOK) ...

        // ※念のため Column の中身も維持してください
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ... (以前のBoxやColumnの実装) ...
            // ここは変更ありません
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                // ... (レーダーやアイコンの表示) ...
                // 変更なし
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (uiState.isScanning) {
                            RadarAnimationBackground()
                        }
                        // ==============================================================================
// 以下のブロックを、元の Surface(...) ブロックと置き換えてください
// ==============================================================================

                        // アイコンと背景（台座）を重ねるBox
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(180.dp) // 全体のサイズ
                        ) {
                            // 1. 背景描画 (Canvasでリッチな枠とグラデーションを描く)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // --- 設定値 ---
                                val strokeWidth = 4.dp.toPx() // リングの太さ

                                // 色の定義 (検知中: ゴールドの輝き / 待機中: 鉄のくすみ)
                                val mainColor = if (uiState.isScanning) Color(0xFFFFD700) else Color(0xFF555555)
                                val glowColor = if (uiState.isScanning) Color(0xFFD4A017) else Color(0xFF333333)

                                // --- A. 内側のグラデーション (RadialGradient) ---
                                // 中心は透明度高く、外側に向かって濃くなる「オーラ」表現
                                val gradientBrush = Brush.radialGradient(
                                    colors = listOf(
                                        glowColor.copy(alpha = 0.0f), // 中心 (完全に透明でアイコンを見やすく)
                                        glowColor.copy(alpha = 0.2f), // 中間
                                        glowColor.copy(alpha = 0.6f)  // 端 (色が濃くなる)
                                    ),
                                    center = center,
                                    radius = size.minDimension / 2
                                )

                                drawCircle(
                                    brush = gradientBrush,
                                    radius = (size.minDimension / 2) - strokeWidth
                                )

                                // --- B. 外側のリング (レアアイテム枠) ---
                                drawCircle(
                                    color = mainColor,
                                    radius = (size.minDimension / 2) - (strokeWidth / 2),
                                    style = Stroke(width = strokeWidth)
                                )
                            }

                            // 2. メインのドット絵アイコン
                            // ========================================================
                            // 🐇 ホップアニメーションの定義
                            // ========================================================
                            val infiniteTransition = rememberInfiniteTransition(label = "hopping")
                            val hopOffsetY by infiniteTransition.animateValue(
                                initialValue = 0.dp,
                                targetValue = (-10).dp, // 10dpぶん上に浮く
                                typeConverter = androidx.compose.ui.unit.Dp.VectorConverter,
                                animationSpec = infiniteRepeatable(
                                    // 1.5秒かけてゆっくり動く (行き帰り合わせて3秒の周期でふわふわさせる)
                                    animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "hopOffsetY"
                            )

                            // スキャン中でなければ 0.dp (静止) にする
                            val animatedOffset = if (uiState.isScanning) hopOffsetY else 0.dp
                            // ========================================================

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .offset(y = animatedOffset) // ← ここで上下のアニメーションを適用！
                            ) {
                                Image(
                                    painter = painterResource(
                                        id = if (uiState.isScanning) R.drawable.ic_radar_active else R.drawable.ic_radar_inactive
                                    ),
                                    contentDescription = if (uiState.isScanning) "スキャン中" else "設定・待機中",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        // 枠線に被らないように内側にパディング
                                        .padding(16.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
// ==============================================================================
                    }
                    Text(
                        text = "${uiState.detectedDeviceCount}人を検知",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatusChip(
                        isBluetoothEnabled = uiState.isBluetoothEnabled,
                        isEncounterActive = uiState.isScanning
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ActionButton(
                        text = if (uiState.isScanning) "すれ違い通信停止" else "すれ違い通信開始",
                        icon = if (uiState.isScanning) Icons.Default.Clear else Icons.Default.Search,
                        isActive = uiState.isScanning,
                        onClick = onToggleEncounter
                    )
                }
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
                DeviceList(devices = uiState.detectedDevices.toList())
            }
        }
    }
}

// ==============================================================================
// 🛠️ 新しい共通部品 (TopBar用ボタン)
// ==============================================================================
@Composable
fun TopBarActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp), // 少し丸める（右下と合わせるなら16.dpでもOK）
        color = MaterialTheme.colorScheme.primary, // 右下と同じメインカラー
        contentColor = MaterialTheme.colorScheme.onPrimary, // 文字・アイコン色
        shadowElevation = 4.dp, // 立体感を出す影
        modifier = Modifier.size(40.dp) // 指で押しやすいサイズ
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}// ==============================================================================
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
                        // 修正箇所: bodySmall → labelSmall に変更
                        // labelSmall には DotGothic16 フォントが適用されているため、
                        // これで灰色の文字もドット絵フォントになります！
                        Text(
                            text = device.toString(),
                            style = MaterialTheme.typography.labelSmall,
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
            onNavigateToSettings = {},
            onNavigateToHelp = {},
            onToggleEncounter = {},
            onClearDetectedDevices = {}
        )
    }
}