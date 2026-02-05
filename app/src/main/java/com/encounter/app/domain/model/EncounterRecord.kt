package com.encounter.app.domain.model

/**
 * すれちがい履歴レコード
 * 永続化されるすれちがいの記録
 * 
 * @param uidPrefix BLEで検知したUID（16文字）
 * @param timestamp すれちがった日時（Unix timestamp）
 * @param displayName ユーザー名（キャッシュ用、オプショナル）
 * 
 * 担当: 久米（Backend）
 */
data class EncounterRecord(
    val uidPrefix: String,
    val timestamp: Long,
    val displayName: String? = null
)
