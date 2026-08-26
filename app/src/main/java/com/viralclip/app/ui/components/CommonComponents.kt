package com.viralclip.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viralclip.app.domain.model.*
import com.viralclip.app.ui.theme.*

// ─── Gradient Button ─────────────────────────────────────────────────

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush = Brush.horizontalGradient(listOf(ViralPurple, ViralPink)),
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp)),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) gradient
                    else Brush.horizontalGradient(listOf(Color.Gray, Color.DarkGray)),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ─── Virality Score Card ─────────────────────────────────────────────

@Composable
fun ViralityScoreCard(
    score: ViralityScore,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.overall,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "score"
    )

    val scoreColor = when {
        score.overall >= 0.7f -> ViralityHigh
        score.overall >= 0.4f -> ViralityMedium
        else -> ViralityLow
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceElevated
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Score Circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = animatedScore,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = scoreColor,
                    trackColor = scoreColor.copy(alpha = 0.15f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(animatedScore * 100).toInt()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = score.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = scoreColor
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Score breakdown
            ScoreBar("Hook Strength", score.hookStrength, ViralPurple)
            Spacer(Modifier.height(8.dp))
            ScoreBar("Engagement", score.engagementPotential, ViralBlue)
            Spacer(Modifier.height(8.dp))
            ScoreBar("Emotional Impact", score.emotionalImpact, ViralPink)
            Spacer(Modifier.height(8.dp))
            ScoreBar("Shareability", score.shareability, ViralCyan)
            Spacer(Modifier.height(8.dp))
            ScoreBar("Watch Time", score.watchTime, ViralGreen)

            if (score.reasons.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Why this clip",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                score.reasons.forEach { reason ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = ViralityHigh
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(label: String, value: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.width(110.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}

// ─── Clip Card ───────────────────────────────────────────────────────

@Composable
fun ClipCard(
    clip: Clip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    showScore: Boolean = true
) {
    val borderColor = when {
        isSelected -> ViralPurple
        clip.viralityScore >= 0.7f -> ViralityHigh.copy(alpha = 0.5f)
        clip.viralityScore >= 0.4f -> ViralityMedium.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ViralPurple.copy(alpha = 0.1f)
            else DarkSurfaceElevated
        ),
        border = if (isSelected || showScore) BorderStroke(2.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ViralPurple.copy(alpha = 0.3f), ViralPink.copy(alpha = 0.3f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    clip.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(clip.durationMs / 1000)}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (clip.captions.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Subtitles,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = ViralCyan
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${clip.captions.size} captions",
                            style = MaterialTheme.typography.labelSmall,
                            color = ViralCyan
                        )
                    }
                }
            }

            if (showScore) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val scoreColor = when {
                        clip.viralityScore >= 0.7f -> ViralityHigh
                        clip.viralityScore >= 0.4f -> ViralityMedium
                        else -> ViralityLow
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(scoreColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${(clip.viralityScore * 100).toInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Score",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

// ─── Section Header ──────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        action?.invoke()
    }
}

// ─── Empty State ─────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(ViralPurple.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = ViralPurple.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

// ─── Gradient Top Bar ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            navigationIconContentColor = TextPrimary
        )
    )
}

// ─── Platform Chip ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformChip(
    platform: PlatformPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val platformColor = when (platform) {
        PlatformPreset.TIKTOK -> TikTokColor
        PlatformPreset.INSTAGRAM_REELS, PlatformPreset.INSTAGRAM_STORY, PlatformPreset.INSTAGRAM_FEED -> InstagramColor
        PlatformPreset.YOUTUBE_SHORTS -> YouTubeColor
        PlatformPreset.TWITTER -> TwitterColor
        PlatformPreset.FACEBOOK -> FacebookColor
        PlatformPreset.LINKEDIN -> LinkedInColor
        else -> ViralPurple
    }

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(platform.displayName, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = platformColor.copy(alpha = 0.15f),
            selectedLabelColor = platformColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = DarkBorder,
            selectedBorderColor = platformColor,
            borderWidth = 1.dp,
            selectedBorderWidth = 2.dp
        )
    )
}

// ─── Processing Overlay ──────────────────────────────────────────────

@Composable
fun ProcessingOverlay(
    state: ProcessingState,
    modifier: Modifier = Modifier
) {
    if (state is ProcessingState.Idle) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = ViralPurple,
                    strokeWidth = 6.dp
                )
                Spacer(Modifier.height(20.dp))
                val message = when (state) {
                    is ProcessingState.Analyzing -> state.message
                    is ProcessingState.Transcribing -> "Transcribing audio…"
                    is ProcessingState.DetectingFaces -> "Detecting faces…"
                    is ProcessingState.ScoringVirality -> "Scoring virality…"
                    is ProcessingState.GeneratingClips -> "Generating clips…"
                    is ProcessingState.ApplyingCaptions -> "Applying captions…"
                    is ProcessingState.Exporting -> "Exporting…"
                    is ProcessingState.Error -> "Error: ${state.message}"
                    ProcessingState.Complete -> "Complete!"
                    ProcessingState.Idle -> ""
                }
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(12.dp))
                val progress = when (state) {
                    is ProcessingState.Analyzing -> state.progress
                    is ProcessingState.Transcribing -> state.progress
                    is ProcessingState.DetectingFaces -> state.progress
                    is ProcessingState.ScoringVirality -> state.progress
                    is ProcessingState.GeneratingClips -> state.progress
                    is ProcessingState.ApplyingCaptions -> state.progress
                    is ProcessingState.Exporting -> state.progress
                    else -> 0f
                }
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ViralPurple,
                        trackColor = ViralPurple.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
