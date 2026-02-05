package com.encounter.app.navigation

/**
 * 画面遷移のルート定義
 * 
 * 担当: 共通（コンフリクト回避のため、変更時は相談）
 */
sealed class Screen(val route: String) {
    // 初期設定フロー
    data object Splash : Screen("splash")
    
    // プロフィール設定フロー（ネスト）
    data object ProfileFlow : Screen("profile_flow")
    data object ProfileSetup : Screen("profile_setup")
    data object TagSelection : Screen("tag_selection")
    data object PermissionRequest : Screen("permission_request")
    
    // メイン画面
    data object Radar : Screen("radar")
    data object MatchList : Screen("match_list?detectedUids={detectedUids}") {
        fun createRoute(detectedUids: String = "") = "match_list?detectedUids=$detectedUids"
    }
    
    // 詳細画面
    data object UserDetail : Screen("user_detail/{userId}") {
        fun createRoute(userId: String) = "user_detail/$userId"
    }
    data object Chat : Screen("chat/{roomId}") {
        fun createRoute(roomId: String) = "chat/$roomId"
    }
    
    // その他
    data object ProfileEdit : Screen("profile_edit")
    data object Settings : Screen("settings")
    data object Help : Screen("help")
}
