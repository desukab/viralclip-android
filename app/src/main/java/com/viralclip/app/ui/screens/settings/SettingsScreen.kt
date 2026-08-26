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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    var darkMode by remember { mutableStateOf(true) }
    var gpuAcceleration by remember { mutableStateOf(true) }
    var autoSave by remember { mutableStateOf(true) }
    var hapticFeedback by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { GradientTopBar("Settings", onBack = onNavigateBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // General
            SettingsSection("General") {
                ToggleSettingRow("Dark Mode", "Use dark theme", darkMode, { darkMode = it })
                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSettingRow("Haptic Feedback", "Vibration on interactions", hapticFeedback, { hapticFeedback = it })
            }

            // Processing
            SettingsSection("Processing") {
                ToggleSettingRow("GPU Acceleration", "Use GPU for faster processing", gpuAcceleration, { gpuAcceleration = it })
                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ToggleSettingRow("Auto-Save", "Save changes automatically", autoSave, { autoSave = it })
            }

            // Export Defaults
            SettingsSection("Export Defaults") {
                SettingsItem("Default Platform", "TikTok", Icons.Filled.Devices)
                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem("Default Quality", "High (1080p)", Icons.Filled.HighQuality)
                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem("Default FPS", "30 fps", Icons.Filled.Speed)
            }

            // Storage
            SettingsSection("Storage") {
                SettingsItem("Cache Size", "128 MB", Icons.Filled.Storage)
                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem("Clear Cache", "", Icons.Filled.DeleteSweep, onClick = {})
            }

            // About
            SettingsSection("About") {
                SettingsItem("Version", "1.0.0", Icons.Filled.Info)
                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem("Rate Us", "Love ViralClip? Rate us!", Icons.Filled.Star, onClick = {})
                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem("Send Feedback", "Help us improve", Icons.Filled.Feedback, onClick = {})
                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem("Privacy Policy", "", Icons.Filled.PrivacyTip, onClick = {})
                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem("Terms of Service", "", Icons.Filled.Gavel, onClick = {})
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
