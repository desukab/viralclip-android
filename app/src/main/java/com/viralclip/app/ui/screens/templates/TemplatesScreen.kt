package com.viralclip.app.ui.screens.templates

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viralclip.app.domain.model.*
import com.viralclip.app.domain.model.FontWeight as DomainFontWeight
import com.viralclip.app.ui.components.GradientTopBar
import com.viralclip.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    onNavigateBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedCategory by remember { mutableStateOf<TemplateCategory?>(null) }
    var selectedTemplate by remember { mutableStateOf<Template?>(null) }

    val templates = remember { getBuiltInTemplates() }
    val filteredTemplates = if (selectedCategory != null) {
        templates.filter { it.category == selectedCategory }
    } else templates

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Templates",
                onBack = onNavigateBack
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedCategory = null
                    },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                        selectedLabelColor = ViralPurple
                    )
                )
                TemplateCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedCategory = cat
                        },
                        label = { Text(cat.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                            selectedLabelColor = ViralPurple
                        )
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTemplates) { template ->
                    TemplateCard(
                        template = template,
                        isSelected = selectedTemplate?.id == template.id,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedTemplate = template
                        }
                    )
                }
            }
        }

        selectedTemplate?.let { template ->
            TemplateDetailSheet(
                template = template,
                onDismiss = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedTemplate = null
                },
                onApply = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedTemplate = null
                }
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: Template,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = getTemplateColors(template.category)
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = if (isSelected) BorderStroke(2.dp, ViralPurple) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors.map { it.copy(alpha = if (isSelected) 0.2f else 0.12f) }
                        )
                    )
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors.map { it.copy(alpha = glowAlpha * 0.3f) }
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(colors.map { it.copy(alpha = 0.3f) })
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Aa",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.first()
                    )
                }

                Column {
                    Text(
                        template.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        template.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }

                if (template.isPremium) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ViralOrange.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("PRO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ViralOrange)
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ViralPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateDetailSheet(
    template: Template,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val colors = getTemplateColors(template.category)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(colors.map { it.copy(alpha = 0.3f) })
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Your caption\nappears here",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = colors.first()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Preview of ${template.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(template.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(template.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(template.category.displayName, fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = colors.first().copy(alpha = 0.1f)
                    )
                )
                if (template.isPremium) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Premium", fontSize = 12.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = ViralOrange.copy(alpha = 0.1f))
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Style Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StyleDetail("Preset", template.captionStyle.preset.displayName)
                        StyleDetail("Size", "${template.captionStyle.fontSize}sp")
                        StyleDetail("Position", template.captionStyle.position.displayName)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StyleDetail("Animation", template.captionStyle.animation.displayName)
                        StyleDetail("Weight", template.captionStyle.fontWeight.name)
                        StyleDetail("Case", template.captionStyle.caseStyle.name)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            com.viralclip.app.ui.components.GradientButton(
                text = if (template.isPremium) "Upgrade to Use" else "Apply Template",
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                enabled = !template.isPremium
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StyleDetail(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun getTemplateColors(category: TemplateCategory): List<Color> = when (category) {
    TemplateCategory.VIRAL -> listOf(ViralPurple, ViralPink)
    TemplateCategory.PROFESSIONAL -> listOf(ViralBlue, Color(0xFF1E40AF))
    TemplateCategory.CREATIVE -> listOf(ViralOrange, ViralPink)
    TemplateCategory.MINIMAL -> listOf(Color.White, Color.Gray)
    TemplateCategory.BOLD -> listOf(ViralRed, ViralOrange)
    TemplateCategory.NEON -> listOf(ViralCyan, ViralGreen)
    TemplateCategory.CINEMATIC -> listOf(Color(0xFF8B5CF6), Color(0xFF1E293B))
    TemplateCategory.RETRO -> listOf(Color(0xFFFF6B6B), Color(0xFF4ECDC4))
    TemplateCategory.PLAYFUL -> listOf(ViralYellow, ViralPink)
    TemplateCategory.ELEGANT -> listOf(Color(0xFFD4AF37), Color(0xFF2C2C2C))
}

private fun getBuiltInTemplates(): List<Template> = listOf(
    Template(1, "Bold Highlight", TemplateCategory.VIRAL, CaptionStyle(preset = CaptionPreset.BOLD_HIGHLIGHT, fontSize = 36, fontWeight = DomainFontWeight.EXTRA_BOLD, fontColor = 0xFFFFFFFF, highlightColor = 0xFFFBBF24), "Bold yellow-highlighted captions for maximum impact"),
    Template(2, "Karaoke Flow", TemplateCategory.CREATIVE, CaptionStyle(preset = CaptionPreset.KARAOKE, fontSize = 30, animation = CaptionAnimation.KARAOKE), "Karaoke-style word-by-word highlighting"),
    Template(3, "Clean Minimal", TemplateCategory.MINIMAL, CaptionStyle(preset = CaptionPreset.MINIMAL, fontSize = 24, fontWeight = DomainFontWeight.NORMAL, fontColor = 0xFFFFFFFF), "Clean, minimal white text for professional content"),
    Template(4, "Neon Glow", TemplateCategory.NEON, CaptionStyle(preset = CaptionPreset.NEON, fontSize = 32, fontColor = 0xFF06B6D4, highlightColor = 0xFFEC4899, outlineWidth = 3f), "Eye-catching neon glow effect"),
    Template(5, "Dramatic", TemplateCategory.BOLD, CaptionStyle(preset = CaptionPreset.DRAMATIC, fontSize = 38, fontWeight = DomainFontWeight.BOLD, fontColor = 0xFFFFFFFF, highlightColor = 0xFFEF4444), "High-contrast dramatic captions"),
    Template(6, "Professional Blue", TemplateCategory.PROFESSIONAL, CaptionStyle(preset = CaptionPreset.DEFAULT, fontSize = 26, fontWeight = DomainFontWeight.SEMI_BOLD, fontColor = 0xFFFFFFFF, backgroundColor = 0xFF1E40AF), "Professional blue-accented subtitles"),
    Template(7, "Typewriter", TemplateCategory.CREATIVE, CaptionStyle(preset = CaptionPreset.TYPEWRITER, fontSize = 22, fontWeight = DomainFontWeight.NORMAL, animation = CaptionAnimation.TYPEWRITER), "Classic typewriter animation effect"),
    Template(8, "Pop In", TemplateCategory.PLAYFUL, CaptionStyle(preset = CaptionPreset.POP_IN, fontSize = 34, animation = CaptionAnimation.POP_IN, highlightColor = 0xFF34D399), "Fun pop-in animation for engaging content"),
    Template(9, "Cinematic", TemplateCategory.CINEMATIC, CaptionStyle(preset = CaptionPreset.DEFAULT, fontSize = 28, fontWeight = DomainFontWeight.LIGHT, fontColor = 0xFFE2E8F0, position = CaptionPosition.BOTTOM), "Cinematic feel with subtle captions"),
    Template(10, "Retro Wave", TemplateCategory.RETRO, CaptionStyle(preset = CaptionPreset.RETRO, fontSize = 30, fontColor = 0xFFFF6B6B, highlightColor = 0xFF4ECDC4), "Retro-inspired color palette"),
    Template(11, "Elegant Gold", TemplateCategory.ELEGANT, CaptionStyle(preset = CaptionPreset.DEFAULT, fontSize = 26, fontWeight = DomainFontWeight.MEDIUM, fontColor = 0xFFD4AF37), "Sophisticated gold accents"),
    Template(12, "Gen Z Chaos", TemplateCategory.PLAYFUL, CaptionStyle(preset = CaptionPreset.BOUNCE, fontSize = 40, fontWeight = DomainFontWeight.BOLD, fontColor = 0xFFFF0000, highlightColor = 0xFF00FF00, animation = CaptionAnimation.BOUNCE), "Loud, bold, chaotic energy")
)
