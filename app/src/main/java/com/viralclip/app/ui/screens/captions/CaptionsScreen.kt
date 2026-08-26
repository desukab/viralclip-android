package com.viralclip.app.ui.screens.captions

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.viralclip.app.R
import com.viralclip.app.domain.model.*
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*
import com.viralclip.app.ui.viewmodels.CaptionsViewModel
import com.viralclip.app.util.Extensions.formatDurationShort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptionsScreen(
    clipId: Long,
    onNavigateBack: () -> Unit,
    viewModel: CaptionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(clipId) { viewModel.loadClip(clipId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Captions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.generateCaptions() }) {
                        Icon(Icons.Filled.AutoAwesome, stringResource(R.string.captions_generate), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Generate", color = ViralPurple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Caption Preview
            item {
                CaptionPreviewBox(
                    style = uiState.currentCaptionStyle,
                    text = uiState.previewText,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = ViralPurple,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Tab(selectedTab == 0, { selectedTab = 0 }) { Text("Style", modifier = Modifier.padding(12.dp)) }
                    Tab(selectedTab == 1, { selectedTab = 1 }) { Text("Edit", modifier = Modifier.padding(12.dp)) }
                    Tab(selectedTab == 2, { selectedTab = 2 }) { Text("Language", modifier = Modifier.padding(12.dp)) }
                }
            }

            when (selectedTab) {
                0 -> {
                    // Style Tab
                    item {
                        Spacer(Modifier.height(16.dp))
                        SectionHeader(title = "Caption Style")
                        Spacer(Modifier.height(12.dp))
                    }
                    item {
                        CaptionStylePresetGrid(
                            selectedPreset = uiState.selectedPreset,
                            onPresetSelected = { viewModel.updateCaptionPreset(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(400.dp)
                        )
                    }

                    // Color Picker
                    item {
                        Spacer(Modifier.height(20.dp))
                        SectionHeader(title = "Font Color")
                        Spacer(Modifier.height(12.dp))
                        ColorPickerRow(
                            selectedColor = uiState.currentCaptionStyle.fontColor,
                            onColorSelected = { viewModel.updateFontColor(it) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Highlight Color
                    item {
                        Spacer(Modifier.height(20.dp))
                        SectionHeader(title = "Highlight Color")
                        Spacer(Modifier.height(12.dp))
                        ColorPickerRow(
                            selectedColor = uiState.currentCaptionStyle.highlightColor,
                            onColorSelected = { viewModel.updateHighlightColor(it) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Font Size
                    item {
                        Spacer(Modifier.height(20.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Font Size", style = MaterialTheme.typography.titleSmall)
                                    Text("${uiState.currentCaptionStyle.fontSize}sp", color = ViralPurple, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = uiState.currentCaptionStyle.fontSize.toFloat(),
                                    onValueChange = { viewModel.updateFontSize(it.toInt()) },
                                    valueRange = 16f..72f,
                                    colors = SliderDefaults.colors(thumbColor = ViralPurple, activeTrackColor = ViralPurple)
                                )
                            }
                        }
                    }

                    // Position
                    item {
                        Spacer(Modifier.height(20.dp))
                        SectionHeader(title = "Position")
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CaptionPosition.entries.take(3).forEach { pos ->
                                val label = pos.displayName
                                FilterChip(
                                    selected = uiState.currentCaptionStyle.position == pos,
                                    onClick = { viewModel.updatePosition(pos) },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                                        selectedLabelColor = ViralPurple
                                    )
                                )
                            }
                        }
                    }

                    // Animation
                    item {
                        Spacer(Modifier.height(20.dp))
                        SectionHeader(title = "Animation")
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CaptionAnimation.entries.forEach { anim ->
                                FilterChip(
                                    selected = uiState.currentCaptionStyle.animation == anim,
                                    onClick = { viewModel.updateAnimation(anim) },
                                    label = { Text(anim.displayName, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Case Style
                    item {
                        Spacer(Modifier.height(20.dp))
                        SectionHeader(title = "Text Case")
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CaseStyle.entries.forEach { cs ->
                                val label = when(cs) {
                                    CaseStyle.NORMAL -> "Normal"
                                    CaseStyle.UPPERCASE -> "UPPER"
                                    CaseStyle.LOWERCASE -> "lower"
                                    CaseStyle.TITLE_CASE -> "Title Case"
                                    CaseStyle.FIRST_WORD_CAPS -> "First caps"
                                }
                                FilterChip(
                                    selected = uiState.currentCaptionStyle.caseStyle == cs,
                                    onClick = { viewModel.updateCaseStyle(cs) },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Edit Tab - Transcript
                    item { Spacer(Modifier.height(16.dp)) }
                    if (uiState.captions.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Filled.Subtitles,
                                title = "No captions yet",
                                message = "Tap 'Generate' to auto-create captions from speech"
                            )
                        }
                    } else {
                        items(uiState.captions.size) { index ->
                            val caption = uiState.captions[index]
                            val isEditing = uiState.editingCaptionId == caption.id
                            TranscriptItem(
                                caption = caption,
                                isEditing = isEditing,
                                editText = uiState.editText,
                                onEdit = { viewModel.startEditingCaption(caption.id, caption.text) },
                                onTextChange = { viewModel.updateEditText(it) },
                                onSave = { viewModel.saveCaptionEdit() },
                                onCancel = { viewModel.cancelCaptionEdit() },
                                onDelete = { viewModel.deleteCaption(caption.id) }
                            )
                        }
                    }
                }
                2 -> {
                    // Language Tab
                    item { Spacer(Modifier.height(16.dp)) }
                    items(uiState.availableLanguages.size) { index ->
                        val (code, name) = uiState.availableLanguages[index]
                        LanguageItem(
                            name = name,
                            code = code,
                            isSelected = uiState.selectedLanguage == code,
                            onClick = { viewModel.updateLanguage(code) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptionPreviewBox(
    style: CaptionStyle,
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(DarkSurfaceHighest, DarkBackground)
                )
            ),
        contentAlignment = when (style.position) {
            CaptionPosition.TOP -> Alignment.TopCenter
            CaptionPosition.CENTER -> Alignment.Center
            else -> Alignment.BottomCenter
        }
    ) {
        Text(
            text = text,
            fontSize = style.fontSize.sp,
            fontWeight = FontWeight.Bold,
            color = Color(style.fontColor),
            modifier = Modifier.padding(24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun TranscriptItem(
    caption: CaptionSegment,
    isEditing: Boolean,
    editText: String,
    onEdit: () -> Unit,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEditing) ViralPurple.copy(alpha = 0.08f) else DarkSurfaceElevated
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    caption.startTimeMs.formatDurationShort() + " → " +
                    caption.endTimeMs.formatDurationShort(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ViralPurple
                )
                if (!isEditing) {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Edit, null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(14.dp), tint = ErrorColor)
                        }
                    }
                }
            }
            if (isEditing) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ViralPurple,
                        cursorColor = ViralPurple
                    ),
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onSave) { Text("Save", color = ViralPurple) }
                    TextButton(onClick = onCancel) { Text("Cancel", color = TextSecondary) }
                }
            } else {
                Text(caption.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LanguageItem(name: String, code: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge, color = if (isSelected) ViralPurple else TextPrimary)
        if (isSelected) {
            Icon(Icons.Filled.CheckCircle, null, tint = ViralPurple, modifier = Modifier.size(22.dp))
        }
    }
}
