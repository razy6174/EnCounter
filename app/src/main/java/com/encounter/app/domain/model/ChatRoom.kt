package com.encounter.app.domain.model

/**
 * チャットルームデータモデル
 * FirestoreのchatRoomsコレクションに対応
 * 
 * 担当: 久米（Backend）- データ定義
 */
data class ChatRoom(
    val roomId: String = "",
    val participants: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val lastMessage: String = ""
)

/**
 * メッセージデータモデル
 * chatRooms/{roomId}/messagesサブコレクションに対応
 */
data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)
