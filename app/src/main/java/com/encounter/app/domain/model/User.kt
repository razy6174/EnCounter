package com.encounter.app.domain.model

/**
 * ユーザーデータモデル
 * Firestoreのusersコレクションに対応
 * 
 * 担当: 久米（Backend）- データ定義
 */
data class User(
    val uid: String = "",
    /** BLE通信用のUIDプレフィックス（16文字） */
    val uidPrefix: String = "",
    val displayName: String = "",
    val comment: String = "",
    val status: UserStatus = UserStatus.OFFLINE,
    val tags: List<String> = emptyList(),
    val fcmToken: String = ""
)

/**
 * ユーザーステータス（「気分」ステータス）
 * プロダクトの核心機能 - 多彩な気分表現を実現
 * 
 * 担当: 久米（Backend）
 */
enum class UserStatus(val displayName: String, val emoji: String) {
    // 話したい系（積極的にマッチング）
    OPEN("話しかけてOK", "👋"),
    BORED("暇してます", "😴"),
    LOOKING_FOR_HELP("誰か助けて", "🆘"),
    GAME_PARTNER("ゲーム仲間募集", "🎮"),
    COFFEE("カフェ行きたい", "☕"),
    
    // 状況系（条件付きマッチング）
    COMMUTING("移動中", "🚃"),
    WORKING("作業中", "💻"),
    
    // 非アクティブ系（マッチングOFF）
    BUSY("忙しい", "🔴"),
    STEALTH("ステルス", "👁️"),
    OFFLINE("オフライン", "⚫");
    
    companion object {
        /**
         * マッチング対象となるステータス一覧
         * これらのステータスの場合、すれ違い検知の対象となる
         */
        val matchableStatuses = setOf(
            OPEN, BORED, LOOKING_FOR_HELP, GAME_PARTNER, COFFEE, COMMUTING, WORKING
        )
        
        /**
         * ステータスがマッチング対象かどうかを判定
         */
        fun UserStatus.isMatchable(): Boolean = this in matchableStatuses
        
        /**
         * 文字列からUserStatusを取得（Firestore互換用）
         * 旧バージョン（WANTED）もサポート
         */
        fun fromString(value: String): UserStatus {
            return when (value.uppercase()) {
                "WANTED" -> OPEN  // 旧バージョン互換
                else -> entries.find { it.name == value.uppercase() } ?: OFFLINE
            }
        }
    }
}
