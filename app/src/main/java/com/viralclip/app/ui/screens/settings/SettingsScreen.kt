package com.viralclip.app.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.viralclip.app.R
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*
import com.viralclip.app.ui.viewmodels.SettingsViewModel
import com.viralclip.app.util.Extensions.formatFileSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { GradientTopBar(stringResource(R.string.settings_title), onBack = onNavigateBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(stringResource(R.string.settings_general)) {
                ThemeSelector(
                    currentTheme = uiState.themeMode,
                    onClick = { showThemeDialog = true }
                )
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                LanguageSelector(
                    currentLanguage = uiState.language,
                    onClick = { showLanguageDialog = true }
                )
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSettingRow(
                    stringResource(R.string.settings_haptic_feedback),
                    stringResource(R.string.settings_haptic_desc),
                    uiState.hapticFeedback,
                    { viewModel.updateHapticFeedback(it) }
                )
            }

            SettingsSection(stringResource(R.string.settings_processing)) {
                ToggleSettingRow(
                    stringResource(R.string.settings_gpu_acceleration),
                    stringResource(R.string.settings_gpu_desc),
                    uiState.gpuAcceleration,
                    { viewModel.updateGpuAcceleration(it) }
                )
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSettingRow(
                    stringResource(R.string.settings_auto_save),
                    stringResource(R.string.settings_auto_save_desc),
                    uiState.autoSave,
                    { viewModel.updateAutoSave(it) }
                )
            }

            SettingsSection(stringResource(R.string.settings_export)) {
                SettingsItem(
                    title = stringResource(R.string.settings_default_platform),
                    subtitle = uiState.defaultPlatform,
                    icon = Icons.Filled.Devices
                )
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    title = stringResource(R.string.settings_default_quality),
                    subtitle = uiState.defaultQuality,
                    icon = Icons.Filled.HighQuality
                )
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                FpsSelector(
                    currentFps = uiState.defaultFps,
                    onFpsSelected = { viewModel.updateDefaultFps(it) }
                )
            }

            SettingsSection(stringResource(R.string.settings_storage)) {
                SettingsItem(
                    title = stringResource(R.string.settings_cache_size),
                    subtitle = formatFileSize(uiState.cacheSizeBytes),
                    icon = Icons.Filled.Storage
                )
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    title = stringResource(R.string.settings_clear_cache),
                    subtitle = if (uiState.cacheSizeBytes > 0) "Free up space" else "Cache is empty",
                    icon = Icons.Filled.DeleteSweep,
                    onClick = { showClearCacheDialog = true }
                )
            }

            SettingsSection("Statistics") {
                SettingsItem(
                    title = stringResource(R.string.settings_videos_processed),
                    subtitle = "${uiState.totalProcessedVideos} videos",
                    icon = Icons.Filled.VideoLibrary
                )
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    title = stringResource(R.string.settings_clips_exported),
                    subtitle = "${uiState.totalExportedClips} clips",
                    icon = Icons.Filled.FileDownload
                )
            }

            SettingsSection(stringResource(R.string.settings_about)) {
                SettingsItem(
                    title = stringResource(R.string.settings_version),
                    subtitle = stringResource(R.string.settings_version_number),
                    icon = Icons.Filled.Info
                )
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    title = stringResource(R.string.settings_rate_us),
                    subtitle = stringResource(R.string.settings_rate_desc),
                    icon = Icons.Filled.Star,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    title = stringResource(R.string.settings_feedback),
                    subtitle = stringResource(R.string.settings_feedback_desc),
                    icon = Icons.Filled.Feedback,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    title = "Privacy Policy",
                    subtitle = "Read our privacy policy",
                    icon = Icons.Filled.PrivacyTip,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    title = "Open Source Licenses",
                    subtitle = "Third-party licenses",
                    icon = Icons.Filled.Code,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text = "Made with ❤️ by ViralClip Team",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache?") },
            text = {
                Text("This will free up ${formatFileSize(uiState.cacheSizeBytes)} of storage. Temporary files will be deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache()
                        showClearCacheDialog = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                ) {
                    Text("Clear", color = ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = uiState.themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                viewModel.updateThemeMode(it)
                showThemeDialog = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = uiState.language,
            onDismiss = { showLanguageDialog = false },
            onSelect = {
                viewModel.updateLanguage(it)
                showLanguageDialog = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = ViralPurple,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = TextSecondary)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
        }
        if (onClick != null) {
            Icon(Icons.Filled.ChevronRight, null, tint = TextTertiary)
        } else {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun ThemeSelector(currentTheme: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Palette, null, modifier = Modifier.size(22.dp), tint = TextSecondary)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
            Text(currentTheme, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextTertiary)
    }
}

@Composable
private fun LanguageSelector(currentLanguage: String, onClick: () -> Unit) {
    val displayName = when (currentLanguage) {
        "en" -> "English"
        "es" -> "Español"
        "fr" -> "Français"
        "de" -> "Deutsch"
        "pt" -> "Português"
        "ja" -> "日本語"
        "ko" -> "한국어"
        "zh" -> "中文"
        "ar" -> "العربية"
        "hi" -> "हिन्दी"
        else -> "English"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Language, null, modifier = Modifier.size(22.dp), tint = TextSecondary)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyLarge)
            Text(displayName, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextTertiary)
    }
}

@Composable
private fun FpsSelector(currentFps: Int, onFpsSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Speed, null, modifier = Modifier.size(22.dp), tint = TextSecondary)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_default_fps), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_fps_format, currentFps),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(24, 30, 60).forEach { fps ->
                FilterChip(
                    selected = currentFps == fps,
                    onClick = { onFpsSelected(fps) },
                    label = { Text("${fps}fps") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                        selectedLabelColor = ViralPurple
                    )
                )
            }
        }
    }
}

@Composable
private fun ThemeDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column {
                listOf(
                    "System" to "Follow system setting",
                    "Light" to "Always use light theme",
                    "Dark" to "Always use dark theme"
                ).forEach { (theme, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { onSelect(theme) },
                            colors = RadioButtonDefaults.colors(selectedColor = ViralPurple)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(theme, style = MaterialTheme.typography.bodyLarge)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ViralPurple)
            }
        },
        containerColor = DarkSurfaceElevated
    )
}

@Composable
private fun LanguageDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val languages = listOf(
        "en" to "English",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "pt" to "Português",
        "ja" to "日本語",
        "ko" to "한국어",
        "zh" to "中文"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Language") },
        text = {
            LazyColumn {
                items(languages.size) { index ->
                    val (code, name) = languages[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == code,
                            onClick = { onSelect(code) },
                            colors = RadioButtonDefaults.colors(selectedColor = ViralPurple)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ViralPurple)
            }
        },
        containerColor = DarkSurfaceElevated
    )
}
