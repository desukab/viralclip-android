package com.viralclip.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Projects : Screen("projects")
    data object Editor : Screen("editor/{projectId}/{clipId}") {
        fun createRoute(projectId: Long, clipId: Long = -1L) = "editor/$projectId/$clipId"
    }
    data object Captions : Screen("captions/{clipId}") {
        fun createRoute(clipId: Long) = "captions/$clipId"
    }
    data object Preview : Screen("preview/{projectId}") {
        fun createRoute(projectId: Long) = "preview/$projectId"
    }
    data object Export : Screen("export/{clipId}") {
        fun createRoute(clipId: Long) = "export/$clipId"
    }
    data object Templates : Screen("templates")
    data object TemplateDetail : Screen("template/{templateId}") {
        fun createRoute(templateId: Long) = "template/$templateId"
    }
    data object Brand : Screen("brand")
    data object BrandEditor : Screen("brand/{brandId}") {
        fun createRoute(brandId: Long = -1L) = "brand/$brandId"
    }
    data object Settings : Screen("settings")
}
