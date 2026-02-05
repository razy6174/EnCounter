package com.encounter.app.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encounter.app.R
import com.encounter.app.ui.theme.EnCounterTheme
import kotlinx.coroutines.delay

/**
 * プロフィール設定画面（外側）
 * * 担当: 昆野（Frontend）- UI実装
 * ViewModel連携: 久米（Backend）- 実装済み
 * * 注意: 以下のセクションは久米が実装済みのため変更禁止
 * - ViewModelの取得（hiltViewModel）
 * - uiState/uiEventの監視
 * - ViewModel関数の呼び出し（onChange, onClick内）
 */
@Composable
fun ProfileSetupScreen(
    onNavigateToTags: () -> Unit,
    // ========================================
    // 久米実装: 変更禁止
    // ========================================
    viewModel: ProfileViewModel = hiltViewModel()
) {
    // ========================================
    // 久米実装: 変更禁止（状態監視）
    // ========================================
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ========================================
    // 久米実装: 変更禁止（UIイベント監視）
    // ========================================
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ProfileUiEvent.NavigateToTagSelection -> onNavigateToTags()
                is ProfileUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> { /* 他のイベントはこの画面では処理しない */ }
            }
        }
    }

    // 内側のContent関数を呼び出す
    ProfileSetupScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onDisplayNameChanged = { viewModel.onDisplayNameChanged(it) },
        onCommentChanged = { viewModel.onCommentChanged(it) },
        onNavigateToTags = { viewModel.navigateToTagSelection() }
    )
}

/**
 * プロフィール設定画面のコンテンツ（内側）
 * 状態を引数で受け取るため、Previewが可能
 * * 昆野担当: ロゴアニメーションとデザイン
 */
@Composable
fun ProfileSetupScreenContent(
    uiState: ProfileUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onDisplayNameChanged: (String) -> Unit,
    onCommentChanged: (String) -> Unit,
    onNavigateToTags: () -> Unit
) {
    // フォントファミリー定義
    // 注意: res/font/dot_font.ttf が存在すること
    val dotFont = FontFamily(Font(R.font.dot_font))

    // アニメーション制御用の状態
    var isLogoVisible by remember { mutableStateOf(false) }
    var isFormVisible by remember { mutableStateOf(false) }

    // 画面表示時にアニメーションを開始
    LaunchedEffect(Unit) {
        // 1. ロゴを表示
        isLogoVisible = true
        // 2. 少し遅れて入力フォームを表示（順次表示することでリッチに見せる）
        delay(1000)
        isFormVisible = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // 全体をスクロール可能にするか、コンテンツを中心に配置
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            // ロゴのサイズによってはTop配置の方が良い場合もありますが、
            // 導入画面らしさを出すためにCenter配置を維持します
            verticalArrangement = Arrangement.Center
        ) {

            // ==========================================
            // 🖼️ ロゴエリア (アニメーション付き)
            // ==========================================
            AnimatedVisibility(
                visible = isLogoVisible,
                // スケールイン（飛び出してくる感じ） + フェードイン
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy, // ボヨンと弾む
                        stiffness = Spring.StiffnessLow
                    ),
                    transformOrigin = TransformOrigin.Center
                ) + fadeIn(animationSpec = tween(500))
            ) {
                // ロゴが浮遊するアニメーション
                val infiniteTransition = rememberInfiniteTransition(label = "logo_float")
                val floatOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -15f, // 15px上に浮く
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing), // 2秒かけてふわっと
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "offset"
                )

                Box(
                    modifier = Modifier
                        .offset(y = floatOffset.dp) // 上下に動かす
                        .padding(bottom = 16.dp)
                ) {
                    Image(
                        // ★ R.drawable.logo_encounter が必要です
                        painter = painterResource(id = R.drawable.logo_encounter),
                        contentDescription = "EnCounter Logo",
                        modifier = Modifier
                            .fillMaxWidth(0.8f) // 画面幅の8割くらい
                            .height(150.dp),    // 高さを制限
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // ==========================================
            // 📝 入力フォームエリア (スライドイン)
            // ==========================================
            AnimatedVisibility(
                visible = isFormVisible,
                // 下からスライドして現れる
                enter = slideInVertically(
                    initialOffsetY = { 100 }, // 100px下から
                    animationSpec = tween(500)
                ) + fadeIn()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // タイトル
                    Text(
                        text = "プロフィール設定",
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = dotFont
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // ニックネーム入力欄
                    OutlinedTextField(
                        value = uiState.displayName,
                        onValueChange = onDisplayNameChanged,
                        label = {
                            Text("ニックネーム", fontFamily = dotFont)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontFamily = dotFont,
                            fontSize = 18.sp
                        ),
                        supportingText = {
                            Text(
                                "${uiState.displayName.length}/${ProfileUiState.MAX_DISPLAY_NAME_LENGTH}",
                                fontFamily = dotFont
                            )
                        },
                        isError = uiState.displayName.length > ProfileUiState.MAX_DISPLAY_NAME_LENGTH
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ひとこと入力欄
                    OutlinedTextField(
                        value = uiState.comment,
                        onValueChange = onCommentChanged,
                        label = {
                            Text("ひとこと（任意）", fontFamily = dotFont)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontFamily = dotFont,
                            fontSize = 18.sp
                        ),
                        supportingText = {
                            Text(
                                "${uiState.comment.length}/${ProfileUiState.MAX_COMMENT_LENGTH}",
                                fontFamily = dotFont
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 次へボタン
                    Button(
                        onClick = onNavigateToTags,
                        enabled = uiState.displayName.isNotBlank() && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            // テーマのSecondary(緑)を使用
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            disabledContainerColor = androidx.compose.ui.graphics.Color.Gray,
                            disabledContentColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(24.dp),
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        } else {
                            Text(
                                "次へ",
                                fontFamily = dotFont,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileSetupScreenPreview() {
    EnCounterTheme {
        ProfileSetupScreenContent(
            uiState = ProfileUiState(),
            onDisplayNameChanged = {},
            onCommentChanged = {},
            onNavigateToTags = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileSetupScreenFilledPreview() {
    EnCounterTheme {
        ProfileSetupScreenContent(
            uiState = ProfileUiState(
                displayName = "久米",
                comment = "よろしくお願いします"
            ),
            onDisplayNameChanged = {},
            onCommentChanged = {},
            onNavigateToTags = {}
        )
    }
}