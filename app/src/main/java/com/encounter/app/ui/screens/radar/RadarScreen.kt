package com.encounter.app.ui.screens.radar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.encounter.app.ui.theme.EnCounterTheme

/**
 * レーダー画面（ホーム）
 * 周囲のユーザーをレーダー表示
 * 
 * 担当: 昆野（Frontend）- UI実装
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    onNavigateToMatchList: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHelp: () -> Unit
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
                // TODO: リストアイコンに変更
                Text("📋")
            }
        }
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
                // TODO: レーダーアニメーションを実装
                Text(
                    text = "🎯",
                    style = MaterialTheme.typography.displayLarge
                )
                Text(
                    text = "周囲をスキャン中...",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "0人を検知",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RadarScreenPreview() {
    EnCounterTheme {
        RadarScreen(
            onNavigateToMatchList = {},
            onNavigateToProfile = {},
            onNavigateToHelp = {}
        )
    }
}
