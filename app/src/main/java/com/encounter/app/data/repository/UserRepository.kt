package com.encounter.app.data.repository

import android.util.Log
import com.encounter.app.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRepository"

/**
 * ユーザー情報のリポジトリ
 * Firebase AuthとFirestoreとの通信を担当
 * 
 * 担当: 久米（Backend）
 */
@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        /** Firestoreのin演算子の最大数 */
        private const val FIRESTORE_IN_QUERY_LIMIT = 30
    }
    
    /**
     * 匿名認証でログイン
     */
    suspend fun signInAnonymously(): Result<String> {
        return try {
            val result = auth.signInAnonymously().await()
            Result.success(result.user?.uid ?: throw Exception("UID is null"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 現在のユーザーIDを取得
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * ユーザープロフィールを保存
     * uidPrefixはuidの先頭16文字を自動設定（BLE通信用）
     */
    suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            // uidPrefixを自動設定（BLEでは16文字に短縮されるため）
            val userWithPrefix = user.copy(uidPrefix = user.uid.take(16))
            Log.d(TAG, "Saving user profile: uid=${user.uid}, uidPrefix=${userWithPrefix.uidPrefix}")
            
            firestore.collection("users")
                .document(user.uid)
                .set(userWithPrefix)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user profile", e)
            Result.failure(e)
        }
    }
    
    /**
     * ユーザー情報を取得
     */
    suspend fun getUser(uid: String): Result<User> {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            val user = snapshot.toObject(User::class.java)
                ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 複数のユーザー情報を取得（BLEで検知したUID用）
     * whereInクエリで一括取得（パフォーマンス最適化）
     * Firestoreのin演算子は最大30件のため、チャンク分割で対応
     * 
     * BLEで検知されるUIDは16文字に短縮されているため、
     * uidPrefixフィールドで検索する
     */
    fun getUsersByIds(uidPrefixes: List<String>): Flow<List<User>> = flow {
        Log.d(TAG, "getUsersByIds called with ${uidPrefixes.size} uidPrefixes")
        uidPrefixes.forEachIndexed { index, prefix ->
            Log.d(TAG, "  uidPrefix[$index]: '$prefix' (length: ${prefix.length})")
        }
        
        if (uidPrefixes.isEmpty()) {
            Log.w(TAG, "uidPrefixes list is empty")
            emit(emptyList())
            return@flow
        }
        
        val allUsers = mutableListOf<User>()
        
        // 30件ずつチャンク分割してクエリ
        uidPrefixes.chunked(FIRESTORE_IN_QUERY_LIMIT).forEachIndexed { chunkIndex, chunk ->
            Log.d(TAG, "Fetching chunk[$chunkIndex] by uidPrefix: $chunk")
            try {
                // uidPrefixフィールドで検索（BLEで送信される16文字と一致）
                val snapshot = firestore.collection("users")
                    .whereIn("uidPrefix", chunk)
                    .get()
                    .await()
                
                Log.d(TAG, "Chunk[$chunkIndex] result: ${snapshot.documents.size} documents")
                snapshot.documents.forEach { doc ->
                    Log.d(TAG, "  Found doc: ${doc.id}, uidPrefix: ${doc.getString("uidPrefix")}")
                }
                
                val users = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)
                }
                allUsers.addAll(users)
            } catch (e: Exception) {
                Log.e(TAG, "Chunk[$chunkIndex] failed: ${e.message}", e)
                // エラーがあっても他のチャンクは取得継続
            }
        }
        
        Log.d(TAG, "Total users fetched: ${allUsers.size}")
        emit(allUsers)
    }
    
    /**
     * プロフィールを部分更新
     * 指定したフィールドのみを更新する
     */
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(uid)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 現在のユーザープロフィールをリアルタイムで監視
     */
    fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(User::class.java))
            }
        
        awaitClose { listener.remove() }
    }
}
