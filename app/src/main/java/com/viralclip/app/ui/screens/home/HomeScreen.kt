package com.viralclip.app.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.viralclip.app.R
import com.viralclip.app.domain.model.ProcessingState
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*
import com.viralclip.app.ui.viewmodels.HomeViewModel

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

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importVideo(context, it) }
    }

    // Handle initial video URI from intent
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
                    IconButton(onClick = onNavigateToSettings) {
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
                // Hero Section
                item {
                    HeroSection(
                        onImportVideo = { videoPicker.launch("video/*") },
                        onRecord = { /* TODO: Camera */ }
                    )
                }

                // Quick Actions
                item {
                    Spacer(Modifier.height(28.dp))
                    SectionHeader(title = "Quick Actions")
                    Spacer(Modifier.height(14.dp))
                    QuickActionsRow(
                        onAutoClip = { videoPicker.launch("video/*") },
                        onAddCaptions = { videoPicker.launch("video/*") },
                        onResize = { videoPicker.launch("video/*") },
                        onSmartCrop = { videoPicker.launch("video/*") }
                    )
                }

                // Recent Projects
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
                    items(uiState.recentProjects) { project ->
                        ClipCard(
                            clip = com.viralclip.app.domain.model.Clip(
                                projectId = project.id,
                                name = project.name,
                                sourceVideoUri = project.sourceVideoUri,
                                startTimeMs = 0,
                                endTimeMs = project.duration
                            ),
                            onClick = { onNavigateToEditor(project.id) },
                            showScore = false
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // Stats
                item {
                    Spacer(Modifier.height(28.dp))
                    StatsSection()
                }
            }

            // Processing Overlay — auto-dismiss on Complete and navigate
            val currentState = uiState.processingState
            LaunchedEffect(currentState) {
                if (currentState is ProcessingState.Complete) {
                    kotlinx.coroutines.delay(1500) // show "Complete!" briefly
                    viewModel.dismissProcessing()
                    uiState.lastProcessedProject?.let { onNavigateToEditor(it.id) }
                }
            }
            ProcessingOverlay(state = currentState)

            // Error Snackbar
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

        // Import Button - Hero CTA
        GradientButton(
            text = "Import Video",
            onClick = onImportVideo,
            icon = Icons.Filled.FileOpen,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Record Button
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
    }
}

@Composable
private fun QuickActionsRow(
    onAutoClip: () -> Unit,
    onAddCaptions: () -> Unit,
    onResize: () -> Unit,
    onSmartCrop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionItem(
            icon = Icons.Filled.AutoAwesome,
            title = "Auto-Clip",
            subtitle = "AI finds\nbest moments",
            gradient = listOf(ViralPurple, ViralPink),
            modifier = Modifier.weight(1f),
            onClick = onAutoClip
        )
        QuickActionItem(
            icon = Icons.Filled.Subtitles,
            title = "Captions",
            subtitle = "Auto-generate\nsubtitles",
            gradient = listOf(ViralBlue, ViralCyan),
            modifier = Modifier.weight(1f),
            onClick = onAddCaptions
        )
        QuickActionItem(
            icon = Icons.Filled.Crop,
            title = "Resize",
            subtitle = "Adapt for\nany platform",
            gradient = listOf(ViralGreen, ViralCyan),
            modifier = Modifier.weight(1f),
            onClick = onResize
        )
        QuickActionItem(
            icon = Icons.Filled.CenterFocusStrong,
            title = "Smart Crop",
            subtitle = "AI-powered\nreframing",
            gradient = listOf(ViralOrange, ViralPink),
            modifier = Modifier.weight(1f),
            onClick = onSmartCrop
        )
    }
}

@Composable
private fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
private fun StatsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("AI Models", "Built-in", Icons.Filled.SmartToy, ViralPurple, Modifier.weight(1f))
        StatCard("Languages", "12+", Icons.Filled.Language, ViralBlue, Modifier.weight(1f))
        StatCard("Platforms", "7+", Icons.Filled.Devices, ViralGreen, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
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
