package com.viralclip.app.ui.screens.editor

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView as StyledPlayerView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import com.viralclip.app.R
import com.viralclip.app.domain.model.*
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*
import com.viralclip.app.ui.viewmodels.EditorViewModel
import com.viralclip.app.ui.viewmodels.EditorTool
import com.viralclip.app.util.Extensions.formatDurationShort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: Long,
    clipId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToCaptions: (Long) -> Unit,
    onNavigateToExport: (Long) -> Unit,
    onNavigateToPreview: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Clip", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.undo()
                        },
                        enabled = uiState.canUndo
                    ) {
                        Icon(
                            Icons.Filled.Undo,
                            stringResource(R.string.editor_undo),
                            tint = if (uiState.canUndo) TextPrimary else TextTertiary
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.redo()
                        },
                        enabled = uiState.canRedo
                    ) {
                        Icon(
                            Icons.Filled.Redo,
                            stringResource(R.string.editor_redo),
                            tint = if (uiState.canRedo) TextPrimary else TextTertiary
                        )
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToPreview()
                    }) {
                        Icon(Icons.Filled.PlayCircle, stringResource(R.string.editor_preview))
                    }
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            uiState.selectedClip?.let { onNavigateToExport(it.id) }
                        }
                    ) {
                        Text("Export", color = ViralPurple, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            EditorBottomBar(
                selectedTool = uiState.selectedTool,
                onToolSelected = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.selectTool(it)
                },
                onExport = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    uiState.selectedClip?.let { onNavigateToExport(it.id) }
                }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            VideoPreviewArea(
                clip = uiState.selectedClip,
                isPlaying = uiState.isPlaying,
                currentPositionMs = uiState.currentPositionMs,
                onPlayPause = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.togglePlayPause()
                },
                onSeek = { viewModel.updatePosition(it) }
            )

            if (uiState.clips.size > 1) {
                ClipsRow(
                    clips = uiState.clips,
                    selectedIndex = uiState.selectedClipIndex,
                    onSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.selectClip(it)
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            uiState.selectedClip?.let { clip ->
                ViralityScoreCard(
                    score = ViralityScore(
                        overall = clip.viralityScore,
                        engagementPotential = clip.viralityScore * 0.9f,
                        emotionalImpact = clip.viralityScore * 0.85f,
                        shareability = clip.viralityScore * 0.95f,
                        watchTime = clip.viralityScore * 0.8f,
                        hookStrength = clip.viralityScore * 1.1f,
                        reasons = listOf("AI-detected highlight", "Strong engagement potential"),
                        suggestedStartTime = clip.startTimeMs,
                        suggestedEndTime = clip.endTimeMs
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                targetState = uiState.selectedTool,
                transitionSpec = {
                    slideInVertically { it / 2 } + fadeIn() togetherWith
                    slideOutVertically { -it / 2 } + fadeOut()
                },
                label = "tool"
            ) { tool ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    when (tool) {
                        EditorTool.TRIM -> TrimControls(
                            clip = uiState.selectedClip,
                            onTrim = { start, end ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                uiState.selectedClip?.let {
                                    viewModel.trimClip(it.id, start, end)
                                }
                            },
                            onSplit = { pos ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                uiState.selectedClip?.let {
                                    viewModel.splitClip(it.id, pos)
                                }
                            },
                            onDelete = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                uiState.selectedClip?.let {
                                    viewModel.deleteClip(it.id)
                                }
                            }
                        )
                        EditorTool.CAPTIONS -> {
                            uiState.selectedClip?.let { clip ->
                                CaptionStylePreviewCard(
                                    captionStyle = clip.captionStyle,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onNavigateToCaptions(clip.id)
                                    }
                                )
                            }
                        }
                        EditorTool.TEXT -> TextControls(
                            onAddText = { text ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                uiState.selectedClip?.let {
                                    viewModel.addTextOverlay(it.id, text)
                                }
                            }
                        )
                        EditorTool.EFFECTS -> FiltersPanel(
                            filters = uiState.selectedClip?.filters ?: ClipFilters(),
                            onFiltersChange = { filters ->
                                uiState.selectedClip?.let {
                                    viewModel.updateFilters(it.id, filters)
                                }
                            }
                        )
                        EditorTool.AUDIO -> AudioControls(
                            volume = uiState.selectedClip?.volume ?: 1f,
                            isMuted = uiState.selectedClip?.isMuted ?: false,
                            onVolumeChange = { volume ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                uiState.selectedClip?.let {
                                    viewModel.updateVolume(it.id, volume)
                                }
                            },
                            onMuteToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                uiState.selectedClip?.let {
                                    viewModel.toggleMute(it.id)
                                }
                            }
                        )
                        EditorTool.SPEED -> SpeedControls(
                            speed = uiState.selectedClip?.speed ?: 1f,
                            onSpeedChange = { speed ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                uiState.selectedClip?.let {
                                    viewModel.changeSpeed(it.id, speed)
                                }
                            }
                        )
                        EditorTool.ADJUST -> AdjustControls(
                            filters = uiState.selectedClip?.filters ?: ClipFilters(),
                            onFiltersChange = { filters ->
                                uiState.selectedClip?.let {
                                    viewModel.updateFilters(it.id, filters)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun EditorBottomBar(
    selectedTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    onExport: () -> Unit
) {
    val tools = listOf(
        EditorTool.TRIM to ("Trim" to Icons.Filled.ContentCut),
        EditorTool.CAPTIONS to ("Captions" to Icons.Filled.Subtitles),
        EditorTool.TEXT to ("Text" to Icons.Filled.TextFields),
        EditorTool.EFFECTS to ("Effects" to Icons.Filled.AutoAwesome),
        EditorTool.AUDIO to ("Audio" to Icons.Filled.VolumeUp),
        EditorTool.SPEED to ("Speed" to Icons.Filled.Speed),
        EditorTool.ADJUST to ("Adjust" to Icons.Filled.Tune)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tools.forEach { (tool, label) ->
                val isSelected = tool == selectedTool
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToolSelected(tool) }
                        .padding(4.dp)
                ) {
                    Icon(
                        label.second,
                        contentDescription = label.first,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) ViralPurple else TextTertiary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label.first,
                        fontSize = 10.sp,
                        color = if (isSelected) ViralPurple else TextTertiary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoPreviewArea(
    clip: Clip?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember(clip?.sourceVideoUri) {
        if (clip?.sourceVideoUri != null) {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(clip.sourceVideoUri)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = false
            }
        } else null
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }

    LaunchedEffect(isPlaying, exoPlayer) {
        exoPlayer?.playWhenReady = isPlaying
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    StyledPlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        setShowBuffering(StyledPlayerView.SHOW_BUFFERING_NEVER)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.PlayCircleFilled,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = ViralPurple.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    clip?.name ?: "No clip selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        clip?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            currentPositionMs.formatDurationShort(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            it.durationMs.formatDurationShort(),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    LinearProgressIndicator(
                        progress = { if (it.durationMs > 0) currentPositionMs.toFloat() / it.durationMs else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = ViralPurple,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipsRow(
    clips: List<Clip>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(clips.withIndex().toList()) { (index, clip) ->
            val isSelected = index == selectedIndex
            Card(
                modifier = Modifier
                    .width(80.dp)
                    .clickable { onSelect(index) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) ViralPurple.copy(alpha = 0.2f)
                    else DarkSurfaceElevated
                ),
                border = if (isSelected) BorderStroke(2.dp, ViralPurple) else null
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ViralPurple.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) ViralPurple else TextSecondary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${(clip.durationMs / 1000)}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    val scoreColor = when {
                        clip.viralityScore >= 0.7f -> ViralityHigh
                        clip.viralityScore >= 0.4f -> ViralityMedium
                        else -> ViralityLow
                    }
                    Text(
                        "${(clip.viralityScore * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = scoreColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TrimControls(
    clip: Clip?,
    onTrim: (Long, Long) -> Unit,
    onSplit: (Long) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Trim & Split", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            clip?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Start", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        Text(it.startTimeMs.formatDurationShort(), style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Duration", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        Text(
                            "${it.durationSeconds.toInt()}s",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ViralPurple
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("End", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        Text(it.endTimeMs.formatDurationShort(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { clip?.let { onSplit((it.startTimeMs + it.endTimeMs) / 2) } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.ContentCut, stringResource(R.string.editor_split), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Split", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor)
                ) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.editor_delete), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CaptionStylePreviewCard(
    captionStyle: CaptionStyle,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ViralPurple.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Aa", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ViralPurple)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Edit Captions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(captionStyle.preset.displayName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextTertiary)
        }
    }
}

@Composable
private fun TextControls(onAddText: (String) -> Unit) {
    var textInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Add Text Overlay", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter text…") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ViralPurple,
                    cursorColor = ViralPurple
                )
            )
            Spacer(Modifier.height(12.dp))
            GradientButton(
                text = "Add Text",
                onClick = {
                    if (textInput.isNotBlank()) {
                        onAddText(textInput)
                        textInput = ""
                    }
                },
                icon = Icons.Filled.TextFields,
                modifier = Modifier.fillMaxWidth(),
                enabled = textInput.isNotBlank()
            )
        }
    }
}

@Composable
private fun AudioControls(
    volume: Float,
    isMuted: Boolean,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Audio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onMuteToggle) {
                    Icon(
                        if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = "Mute",
                        tint = if (isMuted) ErrorColor else TextPrimary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${(volume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = ViralPurple)
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = ViralPurple,
                        activeTrackColor = ViralPurple
                    )
                )
            }
        }
    }
}

@Composable
private fun SpeedControls(
    speed: Float,
    onSpeedChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Playback Speed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${speed}x",
                    style = MaterialTheme.typography.titleMedium,
                    color = ViralPurple,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            Slider(
                value = speed,
                onValueChange = onSpeedChange,
                valueRange = 0.25f..3f,
                steps = 10,
                colors = SliderDefaults.colors(
                    thumbColor = ViralPurple,
                    activeTrackColor = ViralPurple,
                    inactiveTrackColor = ViralPurple.copy(alpha = 0.15f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("0.25x", "0.5x", "1x", "1.5x", "2x", "3x").forEach { label ->
                    val value = label.replace("x", "").toFloat()
                    TextButton(
                        onClick = { onSpeedChange(value) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(label, fontSize = 11.sp, color = if (speed == value) ViralPurple else TextTertiary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersPanel(
    filters: ClipFilters,
    onFiltersChange: (ClipFilters) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Filters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            SliderSettingRow("Brightness", filters.brightness + 1f, { onFiltersChange(filters.copy(brightness = it - 1f)) }, 0f..2f)
            SliderSettingRow("Contrast", filters.contrast, { onFiltersChange(filters.copy(contrast = it)) }, 0f..2f)
            SliderSettingRow("Saturation", filters.saturation, { onFiltersChange(filters.copy(saturation = it)) }, 0f..2f)

            Spacer(Modifier.height(8.dp))
            Text("Presets", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterPreset.values().take(6).forEach { preset ->
                    FilterChip(
                        selected = filters.preset == preset,
                        onClick = {
                            onFiltersChange(
                                when (preset) {
                                    FilterPreset.VIVID -> filters.copy(preset = preset, saturation = 1.5f, contrast = 1.2f)
                                    FilterPreset.WARM -> filters.copy(preset = preset, saturation = 1.1f)
                                    FilterPreset.COOL -> filters.copy(preset = preset, saturation = 0.9f)
                                    FilterPreset.NOIR -> filters.copy(preset = preset, saturation = 0f, contrast = 1.4f)
                                    FilterPreset.VINTAGE -> filters.copy(preset = preset, saturation = 0.8f, contrast = 1.1f)
                                    else -> filters.copy(preset = preset)
                                }
                            )
                        },
                        label = { Text(preset.displayName, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustControls(
    filters: ClipFilters,
    onFiltersChange: (ClipFilters) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Adjustments", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            SliderSettingRow("Temperature", filters.temperature + 1f, { onFiltersChange(filters.copy(temperature = it - 1f)) }, 0f..2f)
            SliderSettingRow("Vignette", filters.vignette, { onFiltersChange(filters.copy(vignette = it)) }, 0f..1f)
            SliderSettingRow("Blur", filters.blur, { onFiltersChange(filters.copy(blur = it)) }, 0f..1f)
            SliderSettingRow("Sharpen", filters.sharpen, { onFiltersChange(filters.copy(sharpen = it)) }, 0f..1f)
        }
    }
}
