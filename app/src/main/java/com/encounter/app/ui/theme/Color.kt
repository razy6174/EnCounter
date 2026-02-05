package com.encounter.app.ui.theme

import androidx.compose.ui.graphics.Color


// ==========================================
// 🛡️ EnCounter RPG Palette
// ドット絵RPGの世界観を表現するカラーパレット
// ==========================================

// --- メインカラー (Primary) ---
// 冒険の道標となる「アンティークゴールド」
// 重要なボタンや強調表示に使用
val RpgGoldPrimary = Color(0xFFD4A017)
val RpgGoldLight = Color(0xFFFFD700) // ハイライト用
val RpgGoldDark = Color(0xFFA67C00)  // 影用

// --- セカンダリカラー (Secondary) ---
// 自然と調和する「フォレストグリーン」
// 「すれ違い通信中」などの肯定的なステータスに使用
val RpgGreenSecondary = Color(0xFF558B2F)
val RpgGreenLight = Color(0xFF85BB5C)

// --- アクション/警告色 (Error/Stop) ---
// 危険を知らせる「ドラゴンレッド」
// 「停止」ボタンやエラー表示に使用
val RpgRedError = Color(0xFFC62828)
val RpgRedLight = Color(0xFFEF5350)

// --- アクセント (Tertiary) ---
// 神秘的な「マナブルー」
// リンクや補助的な情報に使用
val RpgManaBlue = Color(0xFF1E88E5)

// ==========================================
// 📜 ライトモード（昼の冒険）用の背景色
// ==========================================
// 背景：古びた羊皮紙の色
val RpgParchmentBg = Color(0xFFFdf5E6)
// コンテナ：少し濃い羊皮紙（リストアイテムやカードの背景）
val RpgParchmentSurface = Color(0xFFF0E6D2)
// テキスト：インクの焦げ茶色（真っ黒ではなく、紙に馴染む色）
val RpgInkText = Color(0xFF3E2723)

// ==========================================
// 🌑 ダークモード（夜のダンジョン）用の背景色
// ==========================================
// 背景：深い夜の洞窟色
val RpgDungeonBg = Color(0xFF1A1B26)
// コンテナ：石壁のグレー
val RpgStoneSurface = Color(0xFF2F3242)
// テキスト：月明かりの白（真っ白ではなく、目に優しい色）
val RpgMoonText = Color(0xFFE0E0E0)

// ==========================================
// 🔲 共通要素
// ==========================================
// ドット絵風の輪郭線に使う濃い色
val RpgBorderColor = Color(0xFF2D241E)

// ★追加: 成功・稼働中の色（ネオ・エメラルド）
val RetroSuccess = Color(0xFF2ECC71)