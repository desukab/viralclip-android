package com.viralclip.app.ui.screens.export

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.viralclip.app.domain.model.*
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*
import com.viralclip.app.ui.viewmodels.ExportViewModel
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

    LaunchedEffect(clipId) { viewModel.loadClip(clipId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
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
            // Export Preview
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
                    Text(uiState.clip?.name ?: "Clip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${uiState.exportWidth}×${uiState.exportHeight} • ${uiState.selectedPlatform.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Platform Selection
            PlatformSelectionGrid(
                selectedPlatform = uiState.selectedPlatform,
                onPlatformSelected = { viewModel.selectPlatform(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(20.dp))

            // Quality & Format
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Export Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))

                    // Quality
                    Text("Quality", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportQuality.entries.forEach { quality ->
                            FilterChip(
                                selected = uiState.selectedQuality == quality,
                                onClick = { viewModel.selectQuality(quality) },
                                label = { Text(quality.displayName, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                                    selectedLabelColor = ViralPurple
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // FPS
                    Text("Frame Rate", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(24, 30, 60).forEach { fps ->
                            FilterChip(
                                selected = uiState.selectedFps == fps,
                                onClick = { viewModel.selectFps(fps) },
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

                    // Format
                    Text("Format", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VideoFormat.entries.forEach { format ->
                            FilterChip(
                                selected = uiState.selectedFormat == format,
                                onClick = { viewModel.selectFormat(format) },
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

                    // Captions toggle
                    ToggleSettingRow(
                        title = "Include Captions",
                        subtitle = "Burn captions into video",
                        checked = uiState.includeCaptions,
                        onCheckedChange = { viewModel.toggleCaptions() }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Export Button
            AnimatedContent(
                targetState = uiState.exportComplete,
                label = "export_state"
            ) { complete ->
                if (complete) {
                    // Success state
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(ViralGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = ViralGreen, modifier = Modifier.size(48.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Export Complete!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Video saved successfully", color = TextSecondary)
                        Spacer(Modifier.height(20.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GradientButton(
                                text = "Share",
                                onClick = {
                                    uiState.exportPath?.let { path ->
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
                                    }
                                },
                                icon = Icons.Filled.Share,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { onNavigateBack() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Done")
                            }
                        }
                    }
                } else {
                    GradientButton(
                        text = if (uiState.isExporting) "Exporting… ${(uiState.exportProgress * 100).toInt()}%" else "Export Video",
                        onClick = { viewModel.exportVideo(context) },
                        icon = Icons.Filled.FileDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        enabled = !uiState.isExporting
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Processing indicator
            if (uiState.isExporting) {
                LinearProgressIndicator(
                    progress = uiState.exportProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ViralPurple,
                    trackColor = ViralPurple.copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(16.dp))
            }

            // Error
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
