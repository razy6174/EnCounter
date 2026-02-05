package com.encounter.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encounter.app.data.repository.UserRepository
import com.encounter.app.domain.model.User
import com.encounter.app.domain.model.UserStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 利用可能なタグ一覧
 */
data class Tag(
    val id: String,
    val label: String,
    val emoji: String
)

/**
 * 利用可能なタグの定義
 */
object AvailableTags {
    val list = listOf(
        Tag("it", "IT", ""),
        Tag("game", "ゲーム", ""),
        Tag("music", "音楽", ""),
        Tag("reading", "読書", ""),
        Tag("drinking", "酒", ""),
        Tag("sports", "スポーツ", ""),
        Tag("movie", "映画", ""),
        Tag("travel", "旅行", ""),
        Tag("cooking", "料理", ""),
        Tag("art", "アート", ""),
        Tag("android", "Android", ""),
        Tag("ios", "iOS", ""),
        Tag("kotlin", "Kotlin", ""),
        Tag("java", "Java", ""),
        Tag("swift", "Swift", ""),
        Tag("cafe", "カフェ", ""),
        Tag("sauna", "サウナ", ""),
        Tag("engineer", "エンジニア", ""),
        Tag("designer", "デザイナー", ""),
        Tag("student", "学生", ""),
        Tag("developer", "開発者", "")
    )
}

/**
 * プロフィール画面のUI状態
 */
data class ProfileUiState(
    val displayName: String = "",
    val comment: String = "",
    val selectedTags: Set<String> = emptySet(),
    val status: UserStatus = UserStatus.OPEN,
    val isLoading: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val isEditMode: Boolean = false,
    val error: String? = null
) {
    companion object {
        const val MAX_DISPLAY_NAME_LENGTH = 20
        const val MAX_COMMENT_LENGTH = 100
        const val MAX_TAGS = 5
        const val MIN_TAGS = 1
    }
}

/**
 * プロフィール画面のUIイベント（一度きりのイベント）
 */
sealed class ProfileUiEvent {
    data object NavigateToTagSelection : ProfileUiEvent()
    data object NavigateToRadar : ProfileUiEvent()
    data object NavigateBack : ProfileUiEvent()
    data class ShowError(val message: String) : ProfileUiEvent()
}

/**
 * プロフィール画面のViewModel
 * 初回プロフィール登録・編集を担当
 * ProfileSetupScreen, TagSelectionScreen, ProfileEditScreen で共有
 * 
 * 担当: 久米（Backend）
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ProfileUiEvent>()
    val uiEvent: SharedFlow<ProfileUiEvent> = _uiEvent.asSharedFlow()

    /**
     * 編集モードでプロフィールを読み込む
     */
    fun loadProfile() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId() ?: return@launch
            
            _uiState.update { it.copy(isLoading = true) }
            
            userRepository.getUser(userId).fold(
                onSuccess = { user ->
                    _uiState.update { 
                        it.copy(
                            displayName = user.displayName,
                            comment = user.comment,
                            selectedTags = user.tags.toSet(),
                            status = user.status,
                            isLoading = false,
                            isEditMode = true,
                            isSaveEnabled = true
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "プロフィールの読み込みに失敗しました: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * 表示名を更新
     */
    fun onDisplayNameChanged(name: String) {
        if (name.length <= ProfileUiState.MAX_DISPLAY_NAME_LENGTH) {
            _uiState.update { 
                it.copy(
                    displayName = name,
                    isSaveEnabled = validateInput(name, it.selectedTags)
                )
            }
        }
    }

    /**
     * ひとことを更新
     */
    fun onCommentChanged(comment: String) {
        if (comment.length <= ProfileUiState.MAX_COMMENT_LENGTH) {
            _uiState.update { it.copy(comment = comment) }
        }
    }

    /**
     * タグの選択/解除
     */
    fun onTagToggled(tagId: String) {
        val currentTags = _uiState.value.selectedTags
        val newTags = if (tagId in currentTags) {
            currentTags - tagId
        } else {
            if (currentTags.size < ProfileUiState.MAX_TAGS) {
                currentTags + tagId
            } else {
                // 上限に達している場合は追加しない
                currentTags
            }
        }
        
        _uiState.update { 
            it.copy(
                selectedTags = newTags,
                isSaveEnabled = validateInput(it.displayName, newTags)
            )
        }
    }

    /**
     * ステータスを変更
     */
    fun onStatusChanged(status: UserStatus) {
        _uiState.update { it.copy(status = status) }
    }

    /**
     * 入力値のバリデーション
     */
    private fun validateInput(displayName: String, tags: Set<String>): Boolean {
        return displayName.isNotBlank() && 
               displayName.length <= ProfileUiState.MAX_DISPLAY_NAME_LENGTH &&
               tags.size >= ProfileUiState.MIN_TAGS &&
               tags.size <= ProfileUiState.MAX_TAGS
    }

    /**
     * ProfileSetupからTagSelectionへ遷移
     * 一時的に表示名とコメントを保持
     */
    fun navigateToTagSelection() {
        val state = _uiState.value
        if (state.displayName.isBlank()) {
            viewModelScope.launch {
                _uiEvent.emit(ProfileUiEvent.ShowError("ニックネームを入力してください"))
            }
            return
        }
        
        viewModelScope.launch {
            _uiEvent.emit(ProfileUiEvent.NavigateToTagSelection)
        }
    }

    /**
     * プロフィールを保存
     */
    fun saveProfile() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId()
            if (userId == null) {
                _uiEvent.emit(ProfileUiEvent.ShowError("ログインが必要です"))
                return@launch
            }

            val state = _uiState.value
            
            // バリデーション
            if (state.displayName.isBlank()) {
                _uiEvent.emit(ProfileUiEvent.ShowError("ニックネームを入力してください"))
                return@launch
            }
            
            if (state.selectedTags.isEmpty()) {
                _uiEvent.emit(ProfileUiEvent.ShowError("タグを1つ以上選択してください"))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            val user = User(
                uid = userId,
                displayName = state.displayName.trim(),
                comment = state.comment.trim(),
                status = state.status,
                tags = state.selectedTags.toList(),
                fcmToken = ""
            )

            userRepository.saveUserProfile(user).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    if (state.isEditMode) {
                        _uiEvent.emit(ProfileUiEvent.NavigateBack)
                    } else {
                        _uiEvent.emit(ProfileUiEvent.NavigateToRadar)
                    }
                },
                onFailure = { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "保存に失敗しました: ${e.message}"
                        )
                    }
                    _uiEvent.emit(ProfileUiEvent.ShowError("保存に失敗しました"))
                }
            )
        }
    }

    /**
     * エラーをクリア
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
