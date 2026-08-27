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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viralclip.app.domain.model.*
import com.viralclip.app.ui.theme.*
import com.viralclip.app.util.Extensions.maxDurationFormatted
import com.viralclip.app.util.HapticFeedback
import androidx.compose.ui.text.TextStyle

@Composable
fun CaptionStylePresetGrid(
    selectedPreset: CaptionPreset,
    onPresetSelected: (CaptionPreset) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Caption style presets"
) {
    val context = LocalContext.current
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
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(presets) { (preset, colors) ->
            CaptionPresetCard(
                preset = preset,
                colors = colors,
                isSelected = selectedPreset == preset,
                onClick = {
                    HapticFeedback.performSelection(context)
                    onPresetSelected(preset)
                }
            )
        }
    }
}

@Composable
fun CaptionPresetCard(
    preset: CaptionPreset,
    colors: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit,
    contentDescription: String = preset.displayName
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(
                onClickLabel = "Select $contentDescription style"
            ) { onClick() }
            .semantics {
                this.contentDescription = if (isSelected) {
                    "$contentDescription, selected"
                } else {
                    contentDescription
                }
                role = Role.RadioButton
            },
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
                if (colors.size > 1) {
                    Text(
                        text = "Aa",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(brush = Brush.linearGradient(colors))
                    )
                } else {
                    Text(
                        text = "Aa",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Unspecified
                    )
                }
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

@Composable
fun ColorPickerRow(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Long> = listOf(
        0xFFFFFFFF, 0xFFFBBF24, 0xFF34D399, 0xFF60A5FA,
        0xFFF472B6, 0xFFA78BFA, 0xFFEF4444, 0xFFF97316,
        0xFF06B6D4, 0xFF10B981, 0xFF000000, 0xFF7C3AED
    ),
    contentDescription: String = "Color picker"
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        colors.forEachIndexed { index, color ->
            val colorName = getColorName(color)
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
                    .clickable {
                        HapticFeedback.performSelection(context)
                        onColorSelected(color)
                    }
                    .semantics {
                        this.contentDescription = if (color == selectedColor) {
                            "$colorName, selected"
                        } else {
                            colorName
                        }
                        role = Role.RadioButton
                    }
            )
        }
    }
}

private fun getColorName(color: Long): String {
    return when (color) {
        0xFFFFFFFF -> "White"
        0xFF000000 -> "Black"
        0xFFFBBF24 -> "Yellow"
        0xFF34D399 -> "Green"
        0xFF60A5FA -> "Blue"
        0xFFF472B6 -> "Pink"
        0xFFA78BFA -> "Purple"
        0xFFEF4444 -> "Red"
        0xFFF97316 -> "Orange"
        0xFF06B6D4 -> "Cyan"
        0xFF10B981 -> "Emerald"
        0xFF7C3AED -> "Violet"
        else -> "Custom"
    }
}

@Composable
fun SliderSettingRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    valueText: String = "${(value * 100).toInt()}%",
    contentDescription: String = label
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                valueText,
                style = MaterialTheme.typography.bodySmall,
                color = ViralPurple,
                modifier = Modifier.semantics {
                    this.contentDescription = "$label is $valueText"
                }
            )
        }
        Slider(
            value = value,
            onValueChange = { newValue ->
                HapticFeedback.performSelection(context)
                onValueChange(newValue)
            },
            valueRange = valueRange,
            modifier = Modifier.semantics {
                this.contentDescription = contentDescription
            },
            colors = SliderDefaults.colors(
                thumbColor = ViralPurple,
                activeTrackColor = ViralPurple,
                inactiveTrackColor = ViralPurple.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
fun ToggleSettingRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    contentDescription: String = title
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                HapticFeedback.performToggle(context, !checked)
                onCheckedChange(!checked)
            }
            .padding(vertical = 8.dp)
            .semantics {
                this.contentDescription = if (checked) {
                    "$contentDescription, enabled"
                } else {
                    "$contentDescription, disabled"
                }
                role = Role.Switch
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = { newValue ->
                HapticFeedback.performToggle(context, newValue)
                onCheckedChange(newValue)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ViralPurple,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkSurfaceHighest
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlatformSelectionGrid(
    selectedPlatform: PlatformPreset,
    onPlatformSelected: (PlatformPreset) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Platform selection"
) {
    val context = LocalContext.current
    val platforms = listOf(
        PlatformPreset.TIKTOK,
        PlatformPreset.INSTAGRAM_REELS,
        PlatformPreset.YOUTUBE_SHORTS,
        PlatformPreset.FACEBOOK,
        PlatformPreset.TWITTER,
        PlatformPreset.LINKEDIN,
        PlatformPreset.PINTEREST
    )

    Column(modifier = modifier.semantics { this.contentDescription = contentDescription }) {
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
                    onClick = {
                        HapticFeedback.performSelection(context)
                        onPlatformSelected(platform)
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceHighest)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .semantics {
                        this.contentDescription = buildString {
                            append("Platform details: ")
                            append("Resolution ${selectedPlatform.width} by ${selectedPlatform.height}, ")
                            append("Aspect ratio ${selectedPlatform.aspectRatio}, ")
                            append("Maximum duration ${selectedPlatform.maxDurationFormatted()}")
                        }
                    },
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

@Composable
fun CaptionPositionPicker(
    selectedPosition: CaptionPosition,
    onPositionSelected: (CaptionPosition) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Caption position"
) {
    val context = LocalContext.current
    val positions = listOf(
        CaptionPosition.TOP to Icons.Filled.ArrowUpward,
        CaptionPosition.CENTER to Icons.Filled.CenterFocusWeak,
        CaptionPosition.BOTTOM to Icons.Filled.ArrowDownward
    )

    Column(modifier = modifier) {
        Text(
            "Position",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            positions.forEach { (position, icon) ->
                val isSelected = selectedPosition == position
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        HapticFeedback.performSelection(context)
                        onPositionSelected(position)
                    },
                    label = { Text(position.displayName) },
                    leadingIcon = {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.semantics {
                        this.contentDescription = if (isSelected) {
                            "${position.displayName}, selected"
                        } else {
                            position.displayName
                        }
                        role = Role.RadioButton
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                        selectedLabelColor = ViralPurple,
                        selectedLeadingIconColor = ViralPurple
                    )
                )
            }
        }
    }
}

@Composable
fun AnimationPicker(
    selectedAnimation: CaptionAnimation,
    onAnimationSelected: (CaptionAnimation) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Caption animation"
) {
    val context = LocalContext.current
    val animations = CaptionAnimation.entries.filter { it != CaptionAnimation.NONE }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(animations.size) { index ->
            val animation = animations[index]
            val isSelected = selectedAnimation == animation
            FilterChip(
                selected = isSelected,
                onClick = {
                    HapticFeedback.performSelection(context)
                    onAnimationSelected(animation)
                },
                label = { Text(animation.displayName, fontSize = 12.sp) },
                modifier = Modifier.semantics {
                    this.contentDescription = if (isSelected) {
                        "${animation.displayName}, selected"
                    } else {
                        animation.displayName
                    }
                    role = Role.RadioButton
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                    selectedLabelColor = ViralPurple
                )
            )
        }
    }
}

@Composable
fun AlignmentPicker(
    selectedAlignment: Alignment,
    onAlignmentSelected: (Alignment) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Text alignment"
) {
    val context = LocalContext.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Alignment.entries.forEach { alignment ->
            val isSelected = selectedAlignment == alignment
            val icon = when (alignment) {
                Alignment.LEFT -> Icons.Filled.FormatAlignLeft
                Alignment.CENTER -> Icons.Filled.FormatAlignCenter
                Alignment.RIGHT -> Icons.Filled.FormatAlignRight
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) ViralPurple.copy(alpha = 0.15f)
                        else DarkSurfaceHighest
                    )
                    .clickable {
                        HapticFeedback.performSelection(context)
                        onAlignmentSelected(alignment)
                    }
                    .semantics {
                        this.contentDescription = if (isSelected) {
                            "${alignment.name.lowercase()} alignment, selected"
                        } else {
                            "${alignment.name.lowercase()} alignment"
                        }
                        role = Role.RadioButton
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) ViralPurple else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun FontSizeSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minSize: Int = 16,
    maxSize: Int = 64,
    contentDescription: String = "Font size"
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Font Size",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                "${value}px",
                style = MaterialTheme.typography.bodySmall,
                color = ViralPurple,
                modifier = Modifier.semantics {
                    this.contentDescription = "Font size is $value pixels"
                }
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = minSize.toFloat()..maxSize.toFloat(),
            steps = maxSize - minSize - 1,
            modifier = Modifier.semantics {
                this.contentDescription = contentDescription
            },
            colors = SliderDefaults.colors(
                thumbColor = ViralPurple,
                activeTrackColor = ViralPurple,
                inactiveTrackColor = ViralPurple.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
fun PreviewCaption(
    text: String,
    style: CaptionStyle,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CanvasBackground)
            .padding(16.dp),
        contentAlignment = when (style.position) {
            CaptionPosition.TOP -> Alignment.TopCenter
            CaptionPosition.CENTER -> Alignment.Center
            CaptionPosition.BOTTOM, CaptionPosition.CUSTOM -> Alignment.BottomCenter
        }
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = (style.fontSize * 0.5f).sp,
                fontWeight = when (style.fontWeight) {
                    FontWeight.LIGHT -> androidx.compose.ui.text.font.FontWeight.Light
                    FontWeight.NORMAL -> androidx.compose.ui.text.font.FontWeight.Normal
                    FontWeight.MEDIUM -> androidx.compose.ui.text.font.FontWeight.Medium
                    FontWeight.SEMI_BOLD -> androidx.compose.ui.text.font.FontWeight.SemiBold
                    FontWeight.BOLD -> androidx.compose.ui.text.font.FontWeight.Bold
                    FontWeight.EXTRA_BOLD -> androidx.compose.ui.text.font.FontWeight.ExtraBold
                },
                color = Color(style.fontColor)
            ),
            textAlign = when (style.alignment) {
                Alignment.LEFT -> TextAlign.Left
                Alignment.CENTER -> TextAlign.Center
                Alignment.RIGHT -> TextAlign.Right
            }
        )
    }
}
