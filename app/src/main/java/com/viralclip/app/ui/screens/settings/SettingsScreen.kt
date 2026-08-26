package com.viralclip.app.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.viralclip.app.R
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*
import com.viralclip.app.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            // General
            SettingsSection(stringResource(R.string.settings_general)) {
                ToggleSettingRow(
                    stringResource(R.string.settings_dark_mode),
                    stringResource(R.string.settings_dark_mode_desc),
                    uiState.darkMode, { viewModel.updateDarkMode(it) }
                )
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSettingRow(
                    stringResource(R.string.settings_haptic_feedback),
                    stringResource(R.string.settings_haptic_desc),
                    uiState.hapticFeedback, { viewModel.updateHapticFeedback(it) }
                )
            }

            // Processing
            SettingsSection(stringResource(R.string.settings_processing)) {
                ToggleSettingRow(
                    stringResource(R.string.settings_gpu_acceleration),
                    stringResource(R.string.settings_gpu_desc),
                    uiState.gpuAcceleration, { viewModel.updateGpuAcceleration(it) }
                )
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSettingRow(
                    stringResource(R.string.settings_auto_save),
                    stringResource(R.string.settings_auto_save_desc),
                    uiState.autoSave, { viewModel.updateAutoSave(it) }
                )
            }

            // Export Defaults
            SettingsSection(stringResource(R.string.settings_export)) {
                SettingsItem(stringResource(R.string.settings_default_platform), uiState.defaultPlatform, Icons.Filled.Devices)
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(stringResource(R.string.settings_default_quality), uiState.defaultQuality, Icons.Filled.HighQuality)
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(stringResource(R.string.settings_default_fps), stringResource(R.string.settings_fps_format, uiState.defaultFps), Icons.Filled.Speed)
            }

            // Storage
            SettingsSection(stringResource(R.string.settings_storage)) {
                SettingsItem(stringResource(R.string.settings_cache_size), stringResource(R.string.settings_cache_format, uiState.cacheSizeMb.toInt()), Icons.Filled.Storage)
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(stringResource(R.string.settings_clear_cache), "", Icons.Filled.DeleteSweep, onClick = { viewModel.clearCache() })
            }

            // Stats
            SettingsSection(stringResource(R.string.settings_about)) {
                SettingsItem(stringResource(R.string.settings_videos_processed), "${uiState.totalProcessedVideos}", Icons.Filled.VideoLibrary)
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(stringResource(R.string.settings_clips_exported), "${uiState.totalExportedClips}", Icons.Filled.FileDownload)
            }

            // About
            SettingsSection(stringResource(R.string.settings_about)) {
                SettingsItem(stringResource(R.string.settings_version), stringResource(R.string.settings_version_number), Icons.Filled.Info)
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(stringResource(R.string.settings_rate_us), stringResource(R.string.settings_rate_desc), Icons.Filled.Star, onClick = {})
                Divider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(stringResource(R.string.settings_feedback), stringResource(R.string.settings_feedback_desc), Icons.Filled.Feedback, onClick = {})
            }

            Spacer(Modifier.height(40.dp))
        }
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
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ViralPurple,
                checkedTrackColor = ViralPurple.copy(alpha = 0.3f),
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = DarkBorder
            )
        )
    }
}
