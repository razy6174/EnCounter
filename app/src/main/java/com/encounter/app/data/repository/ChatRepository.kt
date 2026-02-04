package com.encounter.app.data.repository

import com.encounter.app.domain.model.ChatRoom
import com.encounter.app.domain.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * チャット機能のリポジトリ
 * チャットルームとメッセージの管理を担当
 * 
 * 担当: 久米（Backend）
 */
@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    /**
     * チャットルームを作成または取得
     */
    suspend fun getOrCreateChatRoom(userId1: String, userId2: String): Result<ChatRoom> {
        return try {
            // 既存のルームを検索
            val participants = listOf(userId1, userId2).sorted()
            val existingRoom = firestore.collection("chatRooms")
                .whereEqualTo("participants", participants)
                .get()
                .await()
            
            if (!existingRoom.isEmpty) {
                val room = existingRoom.documents.first().toObject(ChatRoom::class.java)
                    ?: throw Exception("Failed to parse ChatRoom")
                return Result.success(room)
            }
            
            // 新しいルームを作成
            val roomRef = firestore.collection("chatRooms").document()
            val newRoom = ChatRoom(
                roomId = roomRef.id,
                participants = participants,
                createdAt = System.currentTimeMillis(),
                lastMessage = ""
            )
            roomRef.set(newRoom).await()
            Result.success(newRoom)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * メッセージを送信
     */
    suspend fun sendMessage(roomId: String, senderId: String, text: String): Result<Unit> {
        return try {
            val messageRef = firestore.collection("chatRooms")
                .document(roomId)
                .collection("messages")
                .document()
            
            val message = Message(
                messageId = messageRef.id,
                senderId = senderId,
                text = text,
                createdAt = System.currentTimeMillis()
            )
            
            messageRef.set(message).await()
            
            // lastMessageを更新
            firestore.collection("chatRooms")
                .document(roomId)
                .update("lastMessage", text)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * メッセージをリアルタイムで監視
     */
    fun observeMessages(roomId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("chatRooms")
            .document(roomId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject(Message::class.java)
                } ?: emptyList()
                
                trySend(messages)
            }
        
        awaitClose { listener.remove() }
    }
}
