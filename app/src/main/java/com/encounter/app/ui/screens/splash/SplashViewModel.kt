package com.encounter.app.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encounter.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * スプラッシュ画面のナビゲーション先
 */
sealed class NavigationTarget {
    data object ProfileSetup : NavigationTarget()
    data object Radar : NavigationTarget()
}

/**
 * スプラッシュ画面のUI状態
 */
data class SplashUiState(
    val isLoading: Boolean = true,
    val navigationTarget: NavigationTarget? = null,
    val error: String? = null
)

/**
 * スプラッシュ画面のViewModel
 * アプリ起動時の認証状態確認と初期ルーティングを担当
 * 
 * 担当: 久米（Backend）
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    /**
     * 認証状態を確認し、適切な画面へのナビゲーションを決定
     * 
     * 処理フロー:
     * 1. getCurrentUserId() で認証状態確認
     * 2. 未認証の場合 → signInAnonymously() で匿名認証
     * 3. getUser(uid) でプロフィール存在確認
     * 4. プロフィール未作成 → ProfileSetup へ遷移
     * 5. プロフィール作成済み → Radar へ遷移
     */
    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                // Step 1: 現在のユーザーIDを取得
                var userId = userRepository.getCurrentUserId()
                
                // Step 2: 未認証の場合は匿名認証を実行
                if (userId == null) {
                    val authResult = userRepository.signInAnonymously()
                    authResult.fold(
                        onSuccess = { uid ->
                            userId = uid
                        },
                        onFailure = { e ->
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = "認証に失敗しました: ${e.message}"
                                )
                            }
                            return@launch
                        }
                    )
                }
                
                // Step 3: プロフィールの存在確認
                val userResult = userRepository.getUser(userId!!)
                userResult.fold(
                    onSuccess = { user ->
                        // Step 5: プロフィール作成済み → Radar へ
                        if (user.displayName.isNotBlank()) {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    navigationTarget = NavigationTarget.Radar
                                )
                            }
                        } else {
                            // displayNameが空の場合はセットアップへ
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    navigationTarget = NavigationTarget.ProfileSetup
                                )
                            }
                        }
                    },
                    onFailure = {
                        // Step 4: プロフィール未作成 → ProfileSetup へ
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                navigationTarget = NavigationTarget.ProfileSetup
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "エラーが発生しました: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 認証を再試行
     */
    fun retry() {
        checkAuthState()
    }
}
