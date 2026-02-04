package com.encounter.app.ui.screens.matchlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.encounter.app.ui.theme.EnCounterTheme

/**
 * すれちがいリスト画面
 * 検知したユーザーの一覧を表示
 * 
 * 担当: 昆野（Frontend）- UI実装
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchListScreen(
    onNavigateToUserDetail: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    // TODO: ViewModelからデータを取得
    val dummyUsers = listOf(
        "User 1" to "タグ: #Android, #Kotlin",
        "User 2" to "タグ: #Java, #Beatbox",
        "User 3" to "タグ: #サウナ"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("すれちがいリスト") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            items(dummyUsers) { (name, tags) ->
                MatchUserCard(
                    name = name,
                    tags = tags,
                    onClick = { onNavigateToUserDetail("dummy_id") }
                )
            }
        }
    }
}

@Composable
fun MatchUserCard(
    name: String,
    tags: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = tags,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MatchListScreenPreview() {
    EnCounterTheme {
        MatchListScreen(
            onNavigateToUserDetail = {},
            onNavigateBack = {}
        )
    }
}
