package com.viralclip.app.ui.screens.export

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.viralclip.app.R
import com.viralclip.app.domain.model.*
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*
import com.viralclip.app.ui.viewmodels.ExportViewModel
import com.viralclip.app.util.Extensions.formatFileSize
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    clipId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(clipId) { viewModel.loadClip(clipId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, stringResource(R.string.nav_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExportPreviewCard(
                clipName = uiState.clip?.name ?: "Clip",
                width = uiState.exportWidth,
                height = uiState.exportHeight,
                platform = uiState.selectedPlatform.displayName
            )

            Spacer(Modifier.height(20.dp))

            PlatformSection(
                selectedPlatform = uiState.selectedPlatform,
                onPlatformSelected = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.selectPlatform(it)
                }
            )

            Spacer(Modifier.height(20.dp))

            ExportSettingsCard(
                selectedQuality = uiState.selectedQuality,
                onQualityChange = { viewModel.selectQuality(it) },
                selectedFps = uiState.selectedFps,
                onFpsChange = { viewModel.selectFps(it) },
                selectedFormat = uiState.selectedFormat,
                onFormatChange = { viewModel.selectFormat(it) },
                includeCaptions = uiState.includeCaptions,
                onCaptionsToggle = { viewModel.toggleCaptions() }
            )

            Spacer(Modifier.height(20.dp))

            ExportSummaryCard(
                platform = uiState.selectedPlatform,
                quality = uiState.selectedQuality,
                duration = uiState.clip?.durationMs ?: 0L
            )

            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = uiState.exportComplete,
                transitionSpec = {
                    fadeIn() + scaleIn(initialScale = 0.9f) togetherWith
                    fadeOut() + scaleOut(targetScale = 0.9f)
                },
                label = "export_state"
            ) { complete ->
                if (complete) {
                    ExportCompleteSection(
                        exportPath = uiState.exportPath,
                        onShare = { shareVideo(context, uiState.exportPath) },
                        onDone = onNavigateBack,
                        onExportAnother = { viewModel.resetExport() }
                    )
                } else {
                    GradientButton(
                        text = if (uiState.isExporting) "Exporting… ${(uiState.exportProgress * 100).toInt()}%" else "Export Video",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.exportVideo(context)
                        },
                        icon = if (uiState.isExporting) Icons.Filled.HourglassBottom else Icons.Filled.FileDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        enabled = !uiState.isExporting
                    )
                }
            }

            if (uiState.isExporting) {
                Spacer(Modifier.height(16.dp))
                ExportProgressIndicator(progress = uiState.exportProgress)
            }

            uiState.errorMessage?.let { error ->
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorColor.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Error, null, tint = ErrorColor)
                        Spacer(Modifier.width(12.dp))
                        Text(error, color = ErrorColor, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ExportPreviewCard(
    clipName: String,
    width: Int,
    height: Int,
    platform: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(ViralPurple, ViralPink))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.FileDownload, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(clipName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "${width}×${height} • $platform",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlatformSection(
    selectedPlatform: PlatformPreset,
    onPlatformSelected: (PlatformPreset) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Select Platform",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        PlatformSelectionGrid(
            selectedPlatform = selectedPlatform,
            onPlatformSelected = onPlatformSelected
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSettingsCard(
    selectedQuality: ExportQuality,
    onQualityChange: (ExportQuality) -> Unit,
    selectedFps: Int,
    onFpsChange: (Int) -> Unit,
    selectedFormat: VideoFormat,
    onFormatChange: (VideoFormat) -> Unit,
    includeCaptions: Boolean,
    onCaptionsToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Export Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            Text("Quality", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportQuality.entries.forEach { quality ->
                    FilterChip(
                        selected = selectedQuality == quality,
                        onClick = { onQualityChange(quality) },
                        label = { Text(quality.displayName, fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                            selectedLabelColor = ViralPurple
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Frame Rate", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(24, 30, 60).forEach { fps ->
                    FilterChip(
                        selected = selectedFps == fps,
                        onClick = { onFpsChange(fps) },
                        label = { Text("${fps}fps") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                            selectedLabelColor = ViralPurple
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Format", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VideoFormat.entries.forEach { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = { onFormatChange(format) },
                        label = { Text(format.extension.uppercase()) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                            selectedLabelColor = ViralPurple
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            ToggleSettingRow(
                title = "Include Captions",
                subtitle = "Burn captions into video",
                checked = includeCaptions,
                onCheckedChange = { onCaptionsToggle() }
            )
        }
    }
}

@Composable
private fun ExportSummaryCard(
    platform: PlatformPreset,
    quality: ExportQuality,
    duration: Long
) {
    val estimatedSize = (quality.bitrate.toLong() * (duration / 1000)) / 8

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceElevated
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Export Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            ExportSummaryRow("Platform", platform.displayName, Icons.Filled.Devices)
            Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 8.dp))
            ExportSummaryRow("Resolution", "${platform.width}×${platform.height}", Icons.Filled.AspectRatio)
            Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 8.dp))
            ExportSummaryRow("Quality", quality.displayName, Icons.Filled.HighQuality)
            Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 8.dp))
            ExportSummaryRow("Bitrate", "${quality.bitrate / 1_000_000} Mbps", Icons.Filled.Speed)
            Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 8.dp))
            ExportSummaryRow(
                "Est. File Size",
                estimatedSize.formatFileSize(),
                Icons.Filled.Storage
            )
        }
    }
}

@Composable
private fun ExportSummaryRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = TextTertiary)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExportProgressIndicator(progress: Float) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Exporting", style = MaterialTheme.typography.bodyMedium)
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = ViralPurple)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = ViralPurple,
            trackColor = ViralPurple.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun ExportCompleteSection(
    exportPath: String?,
    onShare: () -> Unit,
    onDone: () -> Unit,
    onExportAnother: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val scale by rememberInfiniteTransition(label = "scale").animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(ViralGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                null,
                tint = ViralGreen,
                modifier = Modifier
                    .size(56.dp)
                    .then(
                        androidx.compose.ui.graphics.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                    )
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Export Complete!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Your video is ready", color = TextSecondary)
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientButton(
                text = "Share",
                onClick = onShare,
                icon = Icons.Filled.Share,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Done")
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onExportAnother,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Export Another", color = ViralPurple)
        }
    }
}

private fun shareVideo(context: android.content.Context, path: String?) {
    if (path == null) return
    try {
        val file = File(path)
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
