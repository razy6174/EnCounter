package com.encounter.app.domain.model

/**
 * ユーザーデータモデル
 * Firestoreのusersコレクションに対応
 * 
 * 担当: 久米（Backend）- データ定義
 */
data class User(
    val uid: String = "",
    val displayName: String = "",
    val comment: String = "",
    val status: UserStatus = UserStatus.OFFLINE,
    val tags: List<String> = emptyList(),
    val fcmToken: String = ""
)

/**
 * ユーザーステータス
 */
enum class UserStatus {
    WANTED,   // 話したい
    BUSY,     // 忙しい
    OFFLINE   // オフライン
}
