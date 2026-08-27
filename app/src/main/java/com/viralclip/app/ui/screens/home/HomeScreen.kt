package com.viralclip.app.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.viralclip.app.R
import com.viralclip.app.domain.model.ProcessingState
import com.viralclip.app.domain.model.Project
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*
import com.viralclip.app.ui.viewmodels.HomeViewModel
import com.viralclip.app.util.Extensions.formatRelativeDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToSettings: () -> Unit,
    initialVideoUri: String? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importVideo(context, it) }
    }

    LaunchedEffect(initialVideoUri) {
        initialVideoUri?.let {
            viewModel.importVideo(context, Uri.parse(it))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(listOf(ViralPurple, ViralPink))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("ViralClip", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToSettings()
                    }) {
                        Icon(Icons.Outlined.Settings, stringResource(R.string.nav_settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    HeroSection(
                        onImportVideo = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            videoPicker.launch("video/*")
                        },
                        onRecord = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(28.dp))
                    SectionHeader(title = "Quick Actions")
                    Spacer(Modifier.height(14.dp))
                    QuickActionsRow(
                        onAutoClip = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            videoPicker.launch("video/*")
                        },
                        onAddCaptions = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            videoPicker.launch("video/*")
                        },
                        onResize = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            videoPicker.launch("video/*")
                        },
                        onSmartCrop = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            videoPicker.launch("video/*")
                        },
                        onTemplates = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToTemplates()
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(28.dp))
                    SectionHeader(
                        title = "Recent Projects",
                        action = {
                            TextButton(onClick = onNavigateToProjects) {
                                Text("See All", color = ViralPurple)
                            }
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                }

                if (uiState.recentProjects.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.VideoLibrary,
                            title = "No projects yet",
                            message = "Import a video to get started creating viral clips"
                        )
                    }
                } else {
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.recentProjects.take(8), key = { it.id }) { project ->
                                RecentProjectCard(
                                    project = project,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onNavigateToEditor(project.id)
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(28.dp))
                    StatsSection(
                        videosProcessed = uiState.totalVideosProcessed,
                        clipsCreated = uiState.totalClipsCreated
                    )
                }

                item {
                    Spacer(Modifier.height(28.dp))
                    TipsSection()
                }
            }

            val currentState = uiState.processingState
            LaunchedEffect(currentState) {
                if (currentState is ProcessingState.Complete) {
                    kotlinx.coroutines.delay(1500)
                    viewModel.dismissProcessing()
                    uiState.lastProcessedProject?.let { onNavigateToEditor(it.id) }
                }
            }
            ProcessingOverlay(state = currentState)

            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("Dismiss", color = ViralPurple)
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
private fun HeroSection(
    onImportVideo: () -> Unit,
    onRecord: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        val infiniteTransition = rememberInfiniteTransition(label = "hero")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Text(
            "Create viral clips",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Transform long videos into short,\nengaging content with AI",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            lineHeight = 24.sp
        )
        Spacer(Modifier.height(24.dp))

        GradientButton(
            text = "Import Video",
            onClick = onImportVideo,
            icon = Icons.Filled.FileOpen,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRecord,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, DarkBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = DarkSurfaceElevated,
                contentColor = TextPrimary
            )
        ) {
            Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Record Video", fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(20.dp))

        // Animated showcase banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            ViralPurple.copy(alpha = 0.3f),
                            ViralPink.copy(alpha = 0.3f),
                            ViralBlue.copy(alpha = 0.3f)
                        )
                    )
                )
                .border(1.dp, ViralPurple.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = ViralPurple,
                    modifier = Modifier
                        .size(40.dp)
                        .then(
                            androidx.compose.ui.graphics.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                )
                Column {
                    Text(
                        "AI-Powered",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Viral moments detected automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onAutoClip: () -> Unit,
    onAddCaptions: () -> Unit,
    onResize: () -> Unit,
    onSmartCrop: () -> Unit,
    onTemplates: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            QuickActionItem(
                icon = Icons.Filled.AutoAwesome,
                title = "Auto-Clip",
                subtitle = "AI finds\nbest moments",
                gradient = listOf(ViralPurple, ViralPink),
                onClick = onAutoClip
            )
        }
        item {
            QuickActionItem(
                icon = Icons.Filled.Subtitles,
                title = "Captions",
                subtitle = "Auto-generate\nsubtitles",
                gradient = listOf(ViralBlue, ViralCyan),
                onClick = onAddCaptions
            )
        }
        item {
            QuickActionItem(
                icon = Icons.Filled.Crop,
                title = "Resize",
                subtitle = "Adapt for\nany platform",
                gradient = listOf(ViralGreen, ViralCyan),
                onClick = onResize
            )
        }
        item {
            QuickActionItem(
                icon = Icons.Filled.CenterFocusStrong,
                title = "Smart Crop",
                subtitle = "AI-powered\nreframing",
                gradient = listOf(ViralOrange, ViralPink),
                onClick = onSmartCrop
            )
        }
        item {
            QuickActionItem(
                icon = Icons.Filled.Style,
                title = "Templates",
                subtitle = "Browse\n12+ styles",
                gradient = listOf(ViralRed, ViralOrange),
                onClick = onTemplates
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(gradient[0].copy(alpha = 0.15f), gradient[1].copy(alpha = 0.08f))
                    )
                )
                .border(
                    1.dp,
                    gradient[0].copy(alpha = 0.2f),
                    RoundedCornerShape(16.dp)
                )
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(gradient.map { it.copy(alpha = 0.3f) })
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = gradient[0], modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RecentProjectCard(
    project: Project,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                ViralPurple.copy(alpha = 0.4f),
                                ViralPink.copy(alpha = 0.4f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                if (project.clips.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ViralPurple.copy(alpha = 0.9f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${project.clips.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    project.updatedAt.formatRelativeDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun StatsSection(
    videosProcessed: Int,
    clipsCreated: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "Your Activity",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = videosProcessed.toString(),
                label = "Videos",
                icon = Icons.Filled.VideoLibrary,
                color = ViralPurple,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = clipsCreated.toString(),
                label = "Clips",
                icon = Icons.Filled.ContentCut,
                color = ViralBlue,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "12+",
                label = "Languages",
                icon = Icons.Filled.Language,
                color = ViralGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
    }
}

@Composable
private fun TipsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "Pro Tips",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))

        val tips = listOf(
            Tip(
                "Hook viewers in 3 seconds",
                "Start your clip with an attention-grabbing moment",
                Icons.Filled.Bolt,
                ViralYellow
            ),
            Tip(
                "Keep it 15-60 seconds",
                "Shorter clips perform better on social platforms",
                Icons.Filled.Schedule,
                ViralBlue
            ),
            Tip(
                "Use bold captions",
                "Captions boost engagement by 80%",
                Icons.Filled.Subtitles,
                ViralPink
            )
        )

        tips.forEach { tip ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(tip.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tip.icon, null, tint = tip.color, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tip.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            tip.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}

private data class Tip(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)
