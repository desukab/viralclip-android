package com.viralclip.app.ui.screens.projects

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.viralclip.app.domain.model.Clip
import com.viralclip.app.domain.model.Project
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*
import com.viralclip.app.ui.viewmodels.HomeViewModel
import com.viralclip.app.util.Extensions.formatRelativeDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProject: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    var isGridView by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(SortOrder.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteFor by remember { mutableStateOf<Long?>(null) }
    var renameTarget by remember { mutableStateOf<Project?>(null) }
    var newName by remember { mutableStateOf("") }

    val filteredProjects = remember(uiState.recentProjects, searchQuery, sortOrder) {
        val filtered = if (searchQuery.isBlank()) {
            uiState.recentProjects
        } else {
            uiState.recentProjects.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        when (sortOrder) {
            SortOrder.RECENT -> filtered.sortedByDescending { it.updatedAt }
            SortOrder.OLDEST -> filtered.sortedBy { it.createdAt }
            SortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            SortOrder.DURATION -> filtered.sortedByDescending { it.duration }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search projects…") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ViralPurple,
                                cursorColor = ViralPurple
                            )
                        )
                    } else {
                        Text("My Projects", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            if (isSearchActive) Icons.Filled.Close else Icons.Filled.ArrowBack,
                            "Back"
                        )
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Filled.Search, "Search")
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Filled.Sort, "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label) },
                                        onClick = {
                                            sortOrder = order
                                            showSortMenu = false
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        leadingIcon = {
                                            if (sortOrder == order) {
                                                Icon(Icons.Filled.Check, null, tint = ViralPurple)
                                            } else {
                                                Spacer(Modifier.width(24.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = {
                            isGridView = !isGridView
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }) {
                            Icon(
                                if (isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                                "Toggle view"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        when {
            uiState.isLoadingProjects -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ViralPurple)
                }
            }
            filteredProjects.isEmpty() -> {
                EmptyState(
                    icon = if (searchQuery.isNotEmpty()) Icons.Filled.SearchOff else Icons.Filled.VideoLibrary,
                    title = if (searchQuery.isNotEmpty()) "No results" else "No projects yet",
                    message = if (searchQuery.isNotEmpty())
                        "Try a different search term"
                    else
                        "Import a video from the home screen to create your first project",
                    modifier = Modifier.padding(padding),
                    action = if (searchQuery.isNotEmpty()) {
                        {
                            TextButton(onClick = {
                                searchQuery = ""
                                isSearchActive = false
                            }) {
                                Text("Clear search", color = ViralPurple)
                            }
                        }
                    } else null
                )
            }
            isGridView -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProjects, key = { it.id }) { project ->
                        ProjectGridCard(
                            project = project,
                            onClick = { onNavigateToProject(project.id) },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showDeleteFor = project.id
                            }
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProjects, key = { it.id }) { project ->
                        ProjectListCard(
                            project = project,
                            onClick = { onNavigateToProject(project.id) },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showDeleteFor = project.id
                            }
                        )
                    }
                }
            }
        }
    }

    showDeleteFor?.let { projectId ->
        val project = uiState.recentProjects.find { it.id == projectId }
        if (project != null) {
            AlertDialog(
                onDismissRequest = { showDeleteFor = null },
                title = { Text("Project Options") },
                text = { Text("What would you like to do with '${project.name}'?") },
                confirmButton = {
                    TextButton(onClick = {
                        newName = project.name
                        renameTarget = project
                        showDeleteFor = null
                    }) {
                        Text("Rename", color = ViralPurple)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            viewModel.deleteProject(project.id)
                            showDeleteFor = null
                        }) {
                            Text("Delete", color = ErrorColor)
                        }
                        TextButton(onClick = { showDeleteFor = null }) {
                            Text("Cancel")
                        }
                    }
                },
                containerColor = DarkSurfaceElevated
            )
        }
    }

    renameTarget?.let { project ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ViralPurple,
                        cursorColor = ViralPurple
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renameProject(project.id, newName)
                        }
                        renameTarget = null
                    }
                ) {
                    Text("Save", color = ViralPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}

private enum class SortOrder(val label: String) {
    RECENT("Most recent"),
    OLDEST("Oldest first"),
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
    DURATION("Longest")
}

@Composable
private fun ProjectGridCard(
    project: Project,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                ViralPurple.copy(alpha = 0.3f),
                                ViralPink.copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayCircleFilled,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp)
                )

                if (project.clips.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${project.clips.size} clips",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${(project.duration / 1000)}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    Text(
                        project.updatedAt.formatRelativeDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectListCard(
    project: Project,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                ViralPurple.copy(alpha = 0.3f),
                                ViralPink.copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = TextTertiary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${(project.duration / 1000)}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    if (project.clips.isNotEmpty()) {
                        Spacer(Modifier.width(12.dp))
                        Icon(
                            Icons.Outlined.ContentCut,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = TextTertiary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${project.clips.size} clips",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    project.updatedAt.formatRelativeDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextTertiary
            )
        }
    }
}
