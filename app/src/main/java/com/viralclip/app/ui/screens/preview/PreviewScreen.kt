package com.viralclip.app.ui.screens.preview

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.viralclip.app.R
import com.viralclip.app.domain.model.*
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*
import com.viralclip.app.ui.viewmodels.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    projectId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToExport: (Long) -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    var selectedClipId by remember { mutableStateOf<Long?>(null) }
    var showComparison by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val clips = uiState.clips

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preview_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showComparison = !showComparison
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Icon(
                            if (showComparison) Icons.Filled.ViewCarousel else Icons.Filled.ViewList,
                            "Compare clips"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (clips.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.VideoLibrary,
                title = stringResource(R.string.preview_no_clips),
                message = stringResource(R.string.preview_no_clips_desc),
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    PreviewHeader(
                        clipsCount = clips.size,
                        projectDuration = uiState.project?.duration ?: 0L
                    )
                }

                if (showComparison) {
                    item {
                        ComparisonView(
                            clips = clips,
                            selectedClipId = selectedClipId,
                            onSelect = { selectedClipId = it }
                        )
                    }
                }

                item {
                    Text(
                        stringResource(R.string.preview_ai_found, clips.size),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.preview_ranked),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                itemsIndexed(clips.sortedByDescending { it.viralityScore }) { index, clip ->
                    PreviewClipCard(
                        clip = clip,
                        rank = index + 1,
                        isSelected = selectedClipId == clip.id,
                        onSelect = { selectedClipId = clip.id },
                        onExport = { onNavigateToExport(clip.id) }
                    )
                }

                item {
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewHeader(
    clipsCount: Int,
    projectDuration: Long
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PreviewStat(
                value = "$clipsCount",
                label = "Viral Clips",
                icon = Icons.Filled.ContentCut,
                color = ViralPurple
            )
            PreviewStat(
                value = formatDuration(projectDuration),
                label = "Total Duration",
                icon = Icons.Filled.Schedule,
                color = ViralBlue
            )
            PreviewStat(
                value = "~${clipsCount * 15}s",
                label = "Est. Content",
                icon = Icons.Filled.AutoAwesome,
                color = ViralGreen
            )
        }
    }
}

@Composable
private fun PreviewStat(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}

@Composable
private fun ComparisonView(
    clips: List<Clip>,
    selectedClipId: Long?,
    onSelect: (Long) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Compare Clips",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                clips.take(4).forEach { clip ->
                    val isSelected = selectedClipId == clip.id
                    val scoreColor = when {
                        clip.viralityScore >= 0.7f -> ViralityHigh
                        clip.viralityScore >= 0.4f -> ViralityMedium
                        else -> ViralityLow
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.7f)
                            .clickable { onSelect(clip.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) scoreColor.copy(alpha = 0.2f) else DarkSurfaceHighest
                        ),
                        border = if (isSelected) BorderStroke(2.dp, scoreColor) else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "${(clip.viralityScore * 100).toInt()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor
                            )
                            Text(
                                "Score",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${clip.durationSeconds.toInt()}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewClipCard(
    clip: Clip,
    rank: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onExport: () -> Unit
) {
    val scoreColor = when {
        clip.viralityScore >= 0.7f -> ViralityHigh
        clip.viralityScore >= 0.4f -> ViralityMedium
        else -> ViralityLow
    }

    val animatedScore by animateFloatAsState(
        targetValue = clip.viralityScore,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "score"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) scoreColor.copy(alpha = 0.1f) else DarkSurfaceElevated
        ),
        border = if (isSelected) BorderStroke(2.dp, scoreColor) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (rank) {
                                    1 -> Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
                                    2 -> Brush.linearGradient(listOf(Color(0xFFC0C0C0), Color(0xFF808080)))
                                    3 -> Brush.linearGradient(listOf(Color(0xFFCD7F32), Color(0xFF8B4513)))
                                    else -> Brush.linearGradient(listOf(DarkSurfaceHighest, DarkSurfaceHighest))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "#$rank",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (rank <= 3) Color.White else TextSecondary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            clip.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${clip.durationSeconds.toInt()} seconds",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${(animatedScore * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        scoreColor.let { when (it) {
                            ViralityHigh -> "High"
                            ViralityMedium -> "Medium"
                            else -> "Low"
                        } },
                        style = MaterialTheme.typography.labelSmall,
                        color = scoreColor
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            ViralityScoreCard(
                score = ViralityScore(
                    overall = clip.viralityScore,
                    engagementPotential = clip.viralityScore * 0.9f,
                    emotionalImpact = clip.viralityScore * 0.85f,
                    shareability = clip.viralityScore * 0.95f,
                    watchTime = clip.viralityScore * 0.8f,
                    hookStrength = (clip.viralityScore * 1.1f).coerceAtMost(1f),
                    reasons = buildList {
                        if (clip.viralityScore > 0.7f) add(stringResource(R.string.preview_high_viral))
                        if (clip.captions.isNotEmpty()) add(stringResource(R.string.preview_captions_available))
                        add(stringResource(R.string.preview_highlight, rank))
                    },
                    suggestedStartTime = clip.startTimeMs,
                    suggestedEndTime = clip.endTimeMs
                )
            )

            Spacer(Modifier.height(16.dp))

            GradientButton(
                text = stringResource(R.string.preview_export_clip),
                onClick = onExport,
                icon = Icons.Filled.FileDownload,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000 / 60) % 60
    val hours = ms / 1000 / 3600
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}
