package com.viralclip.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.viralclip.app.ui.screens.home.HomeScreen
import com.viralclip.app.ui.screens.editor.EditorScreen
import com.viralclip.app.ui.screens.captions.CaptionsScreen
import com.viralclip.app.ui.screens.preview.PreviewScreen
import com.viralclip.app.ui.screens.export.ExportScreen
import com.viralclip.app.ui.screens.templates.TemplatesScreen
import com.viralclip.app.ui.screens.brand.BrandScreen
import com.viralclip.app.ui.screens.settings.SettingsScreen
import com.viralclip.app.ui.screens.projects.ProjectsScreen

@Composable
fun ViralClipNavHost(
    initialVideoUri: String? = null,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(200))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(150))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(200))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(150))
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToEditor = { projectId ->
                    navController.navigate(Screen.Editor.createRoute(projectId))
                },
                onNavigateToProjects = {
                    navController.navigate(Screen.Projects.route)
                },
                onNavigateToTemplates = {
                    navController.navigate(Screen.Templates.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                initialVideoUri = initialVideoUri
            )
        }

        composable(Screen.Projects.route) {
            ProjectsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProject = { projectId ->
                    navController.navigate(Screen.Editor.createRoute(projectId))
                }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("clipId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val clipId = backStackEntry.arguments?.getLong("clipId") ?: -1L
            EditorScreen(
                projectId = projectId,
                clipId = clipId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCaptions = { clipId ->
                    navController.navigate(Screen.Captions.createRoute(clipId))
                },
                onNavigateToExport = { clipId ->
                    navController.navigate(Screen.Export.createRoute(clipId))
                },
                onNavigateToPreview = {
                    navController.navigate(Screen.Preview.createRoute(projectId))
                }
            )
        }

        composable(
            route = Screen.Captions.route,
            arguments = listOf(
                navArgument("clipId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val clipId = backStackEntry.arguments?.getLong("clipId") ?: 0L
            CaptionsScreen(
                clipId = clipId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Preview.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            PreviewScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExport = { clipId ->
                    navController.navigate(Screen.Export.createRoute(clipId))
                }
            )
        }

        composable(
            route = Screen.Export.route,
            arguments = listOf(
                navArgument("clipId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val clipId = backStackEntry.arguments?.getLong("clipId") ?: 0L
            ExportScreen(
                clipId = clipId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Templates.route) {
            TemplatesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Brand.route) {
            BrandScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { brandId ->
                    navController.navigate(Screen.BrandEditor.createRoute(brandId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
