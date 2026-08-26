package com.viralclip.app.ui.screens.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.viralclip.app.domain.model.Clip
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*
import com.viralclip.app.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProject: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Projects", fontWeight = FontWeight.Bold) },
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
        if (uiState.recentProjects.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.VideoLibrary,
                title = "No projects yet",
                message = "Import a video from the home screen to create your first project",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.recentProjects) { project ->
                    ClipCard(
                        clip = Clip(
                            projectId = project.id,
                            name = project.name,
                            sourceVideoUri = project.sourceVideoUri,
                            startTimeMs = 0,
                            endTimeMs = project.duration
                        ),
                        onClick = { onNavigateToProject(project.id) },
                        showScore = false
                    )
                }
            }
        }
    }
}
