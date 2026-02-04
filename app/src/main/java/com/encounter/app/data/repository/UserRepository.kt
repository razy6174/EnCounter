package com.encounter.app.data.repository

import com.encounter.app.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

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
     */
    suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
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
     */
    fun getUsersByIds(uids: List<String>): Flow<List<User>> = flow {
        if (uids.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        
        val users = uids.mapNotNull { uid ->
            try {
                val snapshot = firestore.collection("users")
                    .document(uid)
                    .get()
                    .await()
                snapshot.toObject(User::class.java)
            } catch (e: Exception) {
                null
            }
        }
        emit(users)
    }
}
