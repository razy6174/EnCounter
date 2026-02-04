package com.encounter.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme // ← 修正：これが必要です
import androidx.compose.material3.dynamicLightColorScheme // ← 修正：これが必要です
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 🌑 夜のダンジョンモード（ダークテーマ）
private val DarkColorScheme = darkColorScheme(
    primary = RpgGoldPrimary,
    onPrimary = RpgInkText,
    primaryContainer = RpgGoldDark,
    onPrimaryContainer = RpgGoldLight,

    secondary = RpgGreenSecondary,
    onSecondary = Color.White,
    secondaryContainer = RpgGreenSecondary.copy(alpha = 0.3f),

    tertiary = RpgManaBlue,

    background = RpgDungeonBg,
    onBackground = RpgMoonText,

    surface = RpgStoneSurface,
    onSurface = RpgMoonText,

    error = RpgRedLight
)

// 📜 昼の冒険モード（ライトテーマ）
private val LightColorScheme = lightColorScheme(
    primary = RpgGoldPrimary,
    onPrimary = Color.White,
    primaryContainer = RpgGoldLight,
    onPrimaryContainer = RpgInkText,

    secondary = RpgGreenSecondary,
    onSecondary = Color.White,
    secondaryContainer = RpgGreenLight,

    tertiary = RpgManaBlue,

    background = RpgParchmentBg,
    onBackground = RpgInkText,

    surface = RpgParchmentSurface,
    onSurface = RpgInkText,

    error = RpgRedError,
    onError = Color.White
)

@Composable
fun EnCounterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // ⚠️ 重要: RPGの世界観を守るため、ダイナミックカラー（壁紙連動）はデフォルトでOFF
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // 修正箇所：dynamicDarkColorScheme と dynamicLightColorScheme に書き換えました
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}