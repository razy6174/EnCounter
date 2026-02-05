package com.encounter.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encounter.app.ble.ScanSensitivity
import com.encounter.app.ble.TxPowerLevel
import com.encounter.app.ui.theme.EnCounterTheme

/**
 * 設定画面（外側）
 * 
 * 担当: 昆野（Frontend）- UI実装
 * ViewModel連携: 久米（Backend）- 実装済み
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    // ========================================
    // 久米実装: 変更禁止
    // ========================================
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // ========================================
    // 久米実装: 変更禁止（状態監視）
    // ========================================
    val uiState by viewModel.uiState.collectAsState()
    
    SettingsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onScanSensitivityChanged = { viewModel.setScanSensitivity(it) },
        onTxPowerLevelChanged = { viewModel.setTxPowerLevel(it) },
        onVibrationEnabledChanged = { viewModel.setVibrationEnabled(it) },
        onSoundEnabledChanged = { viewModel.setSoundEnabled(it) },
        onSoundVolumeChanged = { viewModel.setSoundVolume(it) },
        onStealthModeChanged = { viewModel.setStealthMode(it) }
    )
}

/**
 * 設定画面のコンテンツ（内側）
 * 状態を引数で受け取るため、Previewが可能
 * 
 * 昆野担当: 以下は自由に編集可能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onScanSensitivityChanged: (ScanSensitivity) -> Unit,
    onTxPowerLevelChanged: (TxPowerLevel) -> Unit,
    onVibrationEnabledChanged: (Boolean) -> Unit,
    onSoundEnabledChanged: (Boolean) -> Unit,
    onSoundVolumeChanged: (Float) -> Unit,
    onStealthModeChanged: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 距離説明カード
            if (uiState.distanceDescription.isNotEmpty()) {
                DistanceDescriptionCard(description = uiState.distanceDescription)
            }
            
            // BLE設定セクション
            SettingsSectionCard(title = "すれ違い検知設定") {
                // 受信感度
                SettingsSubsection(title = "受信感度（検知距離）") {
                    ScanSensitivitySelector(
                        selected = uiState.scanSensitivity,
                        onSelected = onScanSensitivityChanged
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 送信電力
                SettingsSubsection(title = "発信強度（届く距離）") {
                    TxPowerLevelSelector(
                        selected = uiState.txPowerLevel,
                        onSelected = onTxPowerLevelChanged
                    )
                }
            }
            
            // 通知設定セクション
            SettingsSectionCard(title = "通知設定") {
                // バイブレーション
                SwitchSettingItem(
                    title = "バイブレーション",
                    description = "検知時に振動で通知",
                    checked = uiState.vibrationEnabled,
                    onCheckedChange = onVibrationEnabledChanged
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 音声通知
                SwitchSettingItem(
                    title = "サウンド",
                    description = "検知時に音声で通知",
                    checked = uiState.soundEnabled,
                    onCheckedChange = onSoundEnabledChanged
                )
                
                // 音量スライダー（サウンドON時のみ表示）
                if (uiState.soundEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 8.dp)
                    ) {
                        Text(
                            text = "音量: ${(uiState.soundVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = uiState.soundVolume,
                            onValueChange = onSoundVolumeChanged,
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // ステルスモードセクション
            SettingsSectionCard(title = "プライバシー") {
                SwitchSettingItem(
                    title = if (uiState.stealthMode) "👁️ ステルスモード" else "ステルスモード",
                    description = "自分を他のユーザーに見せないが、他の人は検知できる",
                    checked = uiState.stealthMode,
                    onCheckedChange = onStealthModeChanged
                )
            }
        }
    }
}

/**
 * 距離説明カード
 */
@Composable
private fun DistanceDescriptionCard(description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * 設定セクションカード
 */
@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * 設定サブセクション
 */
@Composable
private fun SettingsSubsection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

/**
 * 受信感度セレクター
 */
@Composable
private fun ScanSensitivitySelector(
    selected: ScanSensitivity,
    onSelected: (ScanSensitivity) -> Unit
) {
    Column(modifier = Modifier.selectableGroup()) {
        ScanSensitivity.entries.forEach { sensitivity ->
            RadioSettingItem(
                title = sensitivity.displayName,
                description = sensitivity.description,
                selected = selected == sensitivity,
                onClick = { onSelected(sensitivity) }
            )
        }
    }
}

/**
 * 送信電力セレクター
 */
@Composable
private fun TxPowerLevelSelector(
    selected: TxPowerLevel,
    onSelected: (TxPowerLevel) -> Unit
) {
    Column(modifier = Modifier.selectableGroup()) {
        TxPowerLevel.entries.forEach { level ->
            RadioSettingItem(
                title = level.displayName,
                description = level.description,
                selected = selected == level,
                onClick = { onSelected(level) }
            )
        }
    }
}

/**
 * ラジオボタン付き設定項目
 */
@Composable
private fun RadioSettingItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // Row の selectable で処理
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * スイッチ付き設定項目
 */
@Composable
private fun SwitchSettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    EnCounterTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                distanceDescription = "あなたは約5-8mまで届き、約5mまで検知します"
            ),
            onNavigateBack = {},
            onScanSensitivityChanged = {},
            onTxPowerLevelChanged = {},
            onVibrationEnabledChanged = {},
            onSoundEnabledChanged = {},
            onSoundVolumeChanged = {},
            onStealthModeChanged = {}
        )
    }
}
