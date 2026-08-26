package com.viralclip.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viralclip.app.domain.model.*
import com.viralclip.app.ui.theme.*

// ─── Caption Style Preset Grid ───────────────────────────────────────

@Composable
fun CaptionStylePresetGrid(
    selectedPreset: CaptionPreset,
    onPresetSelected: (CaptionPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        CaptionPreset.DEFAULT to listOf(ViralPurple, ViralBlue),
        CaptionPreset.BOLD_HIGHLIGHT to listOf(ViralYellow, ViralOrange),
        CaptionPreset.KARAOKE to listOf(ViralPink, ViralPurple),
        CaptionPreset.TYPEWRITER to listOf(Color.White, Color.Gray),
        CaptionPreset.POP_IN to listOf(ViralGreen, ViralCyan),
        CaptionPreset.BOUNCE to listOf(ViralOrange, ViralRed),
        CaptionPreset.NEON to listOf(ViralCyan, ViralPink),
        CaptionPreset.MINIMAL to listOf(Color.White, Color.White),
        CaptionPreset.PROFESSIONAL to listOf(Color(0xFF1E40AF), Color(0xFF3B82F6)),
        CaptionPreset.DRAMATIC to listOf(ViralRed, ViralOrange),
        CaptionPreset.RETRO to listOf(Color(0xFFFF6B6B), Color(0xFF4ECDC4)),
        CaptionPreset.GRADIENT to listOf(ViralPurple, ViralPink, ViralCyan)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(presets) { (preset, colors) ->
            CaptionPresetCard(
                preset = preset,
                colors = colors,
                isSelected = selectedPreset == preset,
                onClick = { onPresetSelected(preset) }
            )
        }
    }
}

@Composable
fun CaptionPresetCard(
    preset: CaptionPreset,
    colors: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ViralPurple.copy(alpha = 0.15f)
            else DarkSurfaceHighest
        ),
        border = if (isSelected) BorderStroke(2.dp, ViralPurple) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp)
            ) {
                // Preview text with style
                Text(
                    text = "Aa",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Unspecified,
                    brush = if (colors.size > 1) {
                        Brush.linearGradient(colors.map { Color(it) })
                    } else null
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    preset.displayName,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) ViralPurple else TextSecondary,
                    maxLines = 2
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(ViralPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// ─── Color Picker Row ────────────────────────────────────────────────

@Composable
fun ColorPickerRow(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Long> = listOf(
        0xFFFFFFFF, 0xFFFBBF24, 0xFF34D399, 0xFF60A5FA,
        0xFFF472B6, 0xFFA78BFA, 0xFFEF4444, 0xFFF97316,
        0xFF06B6D4, 0xFF10B981, 0xFF000000, 0xFF7C3AED
    )
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .then(
                        if (color == selectedColor) {
                            Modifier.border(3.dp, Color.White, CircleShape)
                        } else Modifier
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

// ─── Slider Setting Row ──────────────────────────────────────────────

@Composable
fun SliderSettingRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    valueText: String = "${(value * 100).toInt()}%"
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(valueText, style = MaterialTheme.typography.bodySmall, color = ViralPurple)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ViralPurple,
                activeTrackColor = ViralPurple,
                inactiveTrackColor = ViralPurple.copy(alpha = 0.15f)
            )
        )
    }
}

// ─── Toggle Setting Row ──────────────────────────────────────────────

@Composable
fun ToggleSettingRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ViralPurple,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkSurfaceHighest
            )
        )
    }
}

// ─── Platform Selection Grid ─────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlatformSelectionGrid(
    selectedPlatform: PlatformPreset,
    onPlatformSelected: (PlatformPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val platforms = listOf(
        PlatformPreset.TIKTOK,
        PlatformPreset.INSTAGRAM_REELS,
        PlatformPreset.YOUTUBE_SHORTS,
        PlatformPreset.FACEBOOK,
        PlatformPreset.TWITTER,
        PlatformPreset.LINKEDIN,
        PlatformPreset.PINTEREST
    )

    Column(modifier = modifier) {
        Text(
            "Select Platform",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            platforms.forEach { platform ->
                PlatformChip(
                    platform = platform,
                    isSelected = selectedPlatform == platform,
                    onClick = { onPlatformSelected(platform) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Platform info
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceHighest)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Resolution", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Text("${selectedPlatform.width}×${selectedPlatform.height}", style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Aspect Ratio", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Text(selectedPlatform.aspectRatio, style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Max Duration", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Text(selectedPlatform.maxDurationFormatted(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
