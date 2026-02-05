package com.encounter.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.encounter.app.R

// ==========================================
// 🔠 フォント定義
// ==========================================
// res/font/dot_font.ttf を読み込みます
val DotFontFamily = FontFamily(
    Font(R.font.dot_font)
)

// ==========================================
// 📝 テキストスタイル定義 (Typography)
// アプリ全体の文字をドット絵フォントに統一
// ==========================================
val Typography = Typography(
    // --- 大きい見出し（検知数など） ---
    displayLarge = TextStyle(
        fontFamily = DotFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    displayMedium = TextStyle(
        fontFamily = DotFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),

    // --- タイトル（画面名など） ---
    titleLarge = TextStyle(
        fontFamily = DotFontFamily,
        fontWeight = FontWeight.Bold, // ドット絵でも太字指定は有効
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DotFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),

    // --- 本文（メッセージ、リストの中身） ---
    bodyLarge = TextStyle(
        fontFamily = DotFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DotFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),

    // --- ボタンや小さいラベル ---
    labelLarge = TextStyle(
        fontFamily = DotFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DotFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)