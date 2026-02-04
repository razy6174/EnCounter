package com.encounter.app.ui.screens.userdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
 * ユーザー詳細画面
 * 
 * 担当: 昆野（Frontend）- UI実装
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    userId: String,
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    // TODO: ViewModelからユーザー情報を取得
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ユーザー詳細") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // アバター（仮）
            Text(
                text = "👤",
                style = MaterialTheme.typography.displayLarge
            )
            
            Text(
                text = "ユーザー名",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Text(
                text = "#Android #Kotlin #サウナ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "よろしくお願いします！",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Button(
                onClick = { onNavigateToChat("dummy_room_id") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("話しかける")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserDetailScreenPreview() {
    EnCounterTheme {
        UserDetailScreen(
            userId = "dummy",
            onNavigateToChat = {},
            onNavigateBack = {}
        )
    }
}
