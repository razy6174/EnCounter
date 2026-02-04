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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.encounter.app.ui.theme.EnCounterTheme

/**
 * タグ選択画面
 * 
 * 担当: 昆野（Frontend）- UI実装
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSelectionScreen(
    onNavigateToRadar: () -> Unit
) {
    val availableTags = listOf(
        "Android", "iOS", "Kotlin", "Java", "Swift",
        "Beatbox", "音楽", "ゲーム", "サウナ", "カフェ",
        "エンジニア", "デザイナー", "学生", "社会人"
    )
    val selectedTags = remember { mutableStateListOf<String>() }
    
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "興味タグを選択",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Text(
                text = "あなたの興味や属性を選んでください",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableTags.forEach { tag ->
                    FilterChip(
                        selected = tag in selectedTags,
                        onClick = {
                            if (tag in selectedTags) {
                                selectedTags.remove(tag)
                            } else {
                                selectedTags.add(tag)
                            }
                        },
                        label = { Text("#$tag") }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onNavigateToRadar,
                enabled = selectedTags.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("はじめる")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TagSelectionScreenPreview() {
    EnCounterTheme {
        TagSelectionScreen(
            onNavigateToRadar = {}
        )
    }
}
