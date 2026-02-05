package com.encounter.app.domain.model

/**
 * すれ違い履歴レコード
 * 永続化されるすれ違いの記録
 * 
 * @param uidPrefix BLEで検知したUID（16文字）
 * @param timestamp すれ違った日時（Unix timestamp）
 * @param displayName ユーザー名（キャッシュ用、オプショナル）
 * 
 * 担当: 久米（Backend）
 */
data class EncounterRecord(
    val uidPrefix: String,
    val timestamp: Long,
    val displayName: String? = null
)
