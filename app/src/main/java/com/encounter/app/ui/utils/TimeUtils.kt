package com.encounter.app.ui.utils

/**
 * 時間関連のユーティリティ関数
 * 
 * 担当: 久米（Backend）
 */
object TimeUtils {
    
    /**
     * タイムスタンプを相対時間文字列に変換
     * 
     * @param timestamp Unix timestamp (milliseconds)
     * @return 相対時間文字列（例: 「たった今」「5分前」「2時間前」）
     */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "たった今"
            diff < 3_600_000 -> "${diff / 60_000}分前"
            diff < 86_400_000 -> "${diff / 3_600_000}時間前"
            diff < 604_800_000 -> "${diff / 86_400_000}日前"
            else -> "${diff / 604_800_000}週間前"
        }
    }
}
