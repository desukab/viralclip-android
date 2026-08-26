package com.viralclip.app.ui.screens.preview

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    projectId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToExport: (Long) -> Unit
) {
    // Placeholder preview data
    val clips = remember {
        listOf(
            Clip(1, projectId, "Highlight 1", "", 0, 30000, 0, 0.85f),
            Clip(2, projectId, "Highlight 2", "", 32000, 58000, 1, 0.72f),
            Clip(3, projectId, "Highlight 3", "", 60000, 90000, 2, 0.65f)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "AI found ${clips.size} viral clips",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ranked by virality potential. Tap to preview and export.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            itemsIndexed(clips) { index, clip ->
                ViralityScoreCard(
                    score = ViralityScore(
                        overall = clip.viralityScore,
                        engagementPotential = clip.viralityScore * 0.9f,
                        emotionalImpact = clip.viralityScore * 0.85f,
                        shareability = clip.viralityScore * 0.95f,
                        watchTime = clip.viralityScore * 0.8f,
                        hookStrength = clip.viralityScore * 1.1f,
                        reasons = listOf("AI-detected highlight #${index + 1}"),
                        suggestedStartTime = clip.startTimeMs,
                        suggestedEndTime = clip.endTimeMs
                    )
                )

                GradientButton(
                    text = "Export Clip",
                    onClick = { onNavigateToExport(clip.id) },
                    icon = Icons.Filled.FileDownload,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
