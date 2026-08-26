package com.viralclip.app.ui.screens.preview

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                itemsIndexed(clips) { index, clip ->
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
                                add(stringResource(R.string.preview_highlight, index + 1))
                            },
                            suggestedStartTime = clip.startTimeMs,
                            suggestedEndTime = clip.endTimeMs
                        )
                    )

                    GradientButton(
                        text = stringResource(R.string.preview_export_clip),
                        onClick = { onNavigateToExport(clip.id) },
                        icon = Icons.Filled.FileDownload,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
