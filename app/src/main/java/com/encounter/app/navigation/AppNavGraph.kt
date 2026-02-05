package com.encounter.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.encounter.app.ui.screens.chat.ChatScreen
import com.encounter.app.ui.screens.help.HelpScreen
import com.encounter.app.ui.screens.matchlist.MatchListScreen
import com.encounter.app.ui.screens.profile.ProfileEditScreen
import com.encounter.app.ui.screens.profile.ProfileSetupScreen
import com.encounter.app.ui.screens.profile.ProfileViewModel
import com.encounter.app.ui.screens.profile.TagSelectionScreen
import com.encounter.app.ui.screens.radar.RadarScreen
import com.encounter.app.ui.screens.splash.SplashScreen
import com.encounter.app.ui.screens.userdetail.UserDetailScreen

/**
 * アプリのナビゲーショングラフ
 * 
 * 担当: 共通（コンフリクト回避のため、変更時は相談）
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // スプラッシュ画面
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToSetup = {
                    navController.navigate(Screen.ProfileFlow.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Radar.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        // プロフィール設定フロー（ViewModelを共有）
        navigation(
            startDestination = Screen.ProfileSetup.route,
            route = Screen.ProfileFlow.route
        ) {
            // プロフィール設定画面
            composable(Screen.ProfileSetup.route) { backStackEntry ->
                // 親ルートのbackStackEntryからViewModelを取得（共有）
                // rememberでキャッシュしてrecomposition時のクラッシュを防ぐ
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.ProfileFlow.route)
                }
                val viewModel: ProfileViewModel = hiltViewModel(parentEntry)
                
                ProfileSetupScreen(
                    onNavigateToTags = {
                        navController.navigate(Screen.TagSelection.route)
                    },
                    viewModel = viewModel
                )
            }
            
            // タグ選択画面
            composable(Screen.TagSelection.route) { backStackEntry ->
                // 親ルートのbackStackEntryからViewModelを取得（共有）
                // rememberでキャッシュしてrecomposition時のクラッシュを防ぐ
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.ProfileFlow.route)
                }
                val viewModel: ProfileViewModel = hiltViewModel(parentEntry)
                
                TagSelectionScreen(
                    onNavigateToRadar = {
                        navController.navigate(Screen.Radar.route) {
                            popUpTo(Screen.ProfileFlow.route) { inclusive = true }
                        }
                    },
                    viewModel = viewModel
                )
            }
        }
        
        // レーダー画面（ホーム）
        composable(Screen.Radar.route) {
            RadarScreen(
                onNavigateToMatchList = {
                    navController.navigate(Screen.MatchList.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.ProfileEdit.route)
                },
                onNavigateToHelp = {
                    navController.navigate(Screen.Help.route)
                }
            )
        }
        
        // マッチリスト画面
        composable(Screen.MatchList.route) {
            MatchListScreen(
                onNavigateToUserDetail = { userId ->
                    navController.navigate(Screen.UserDetail.createRoute(userId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // ユーザー詳細画面
        composable(
            route = Screen.UserDetail.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            UserDetailScreen(
                userId = userId,
                onNavigateToChat = { roomId ->
                    navController.navigate(Screen.Chat.createRoute(roomId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // チャット画面
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            ChatScreen(
                roomId = roomId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // プロフィール編集画面
        composable(Screen.ProfileEdit.route) {
            ProfileEditScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // ヘルプ画面
        composable(Screen.Help.route) {
            HelpScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
