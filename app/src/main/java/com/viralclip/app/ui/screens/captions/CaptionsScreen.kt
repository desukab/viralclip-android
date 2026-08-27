package com.viralclip.app.ui.screens.captions

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.viralclip.app.domain.model.Alignment as CaptionAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight as ComposeFontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val haptic = LocalHapticFeedback.current

    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(clipId) { viewModel.loadClip(clipId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Captions", fontWeight = ComposeFontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = ViralPurple,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                    } else {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.generateCaptions()
                            }
                        ) {
                            Icon(Icons.Filled.AutoAwesome, stringResource(R.string.captions_generate), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Generate", color = ViralPurple)
                        }
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
            item {
                CaptionPreviewBox(
                    style = uiState.currentCaptionStyle,
                    text = uiState.previewText,
                    modifier = Modifier.padding(16.dp)
                )
            }

            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = ViralPurple,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Tab(selected = selectedTab == 0, onClick = {
                        selectedTab = 0
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Text("Style", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = {
                        selectedTab = 1
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Text("Edit", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 2, onClick = {
                        selectedTab = 2
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Text("Language", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 3, onClick = {
                        selectedTab = 3
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Text("Advanced", modifier = Modifier.padding(12.dp))
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    item { Spacer(Modifier.height(16.dp)) }
                    item { SectionHeader(title = "Caption Style") }
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        CaptionStylePresetGrid(
                            selectedPreset = uiState.selectedPreset,
                            onPresetSelected = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.updateCaptionPreset(it)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(400.dp)
                        )
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                    item { SectionHeader(title = "Colors") }
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        ColorSection(
                            label = "Font Color",
                            color = uiState.currentCaptionStyle.fontColor,
                            onColorSelected = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.updateFontColor(it)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                    item {
                        ColorSection(
                            label = "Highlight Color",
                            color = uiState.currentCaptionStyle.highlightColor,
                            onColorSelected = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.updateHighlightColor(it)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                    item {
                        ColorSection(
                            label = "Outline Color",
                            color = uiState.currentCaptionStyle.outlineColor,
                            onColorSelected = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.updateOutlineColor(it)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                    item { SectionHeader(title = "Size & Weight") }
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        FontControls(
                            fontSize = uiState.currentCaptionStyle.fontSize,
                            fontWeight = uiState.currentCaptionStyle.fontWeight,
                            onFontSizeChange = { viewModel.updateFontSize(it) },
                            onFontWeightChange = { viewModel.updateFontWeight(it) }
                        )
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                    item { SectionHeader(title = "Position") }
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        PositionControls(
                            position = uiState.currentCaptionStyle.position,
                            onPositionChange = { viewModel.updatePosition(it) }
                        )
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                    item { SectionHeader(title = "Animation") }
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        AnimationControls(
                            animation = uiState.currentCaptionStyle.animation,
                            onAnimationChange = { viewModel.updateAnimation(it) }
                        )
                    }
                }
                1 -> {
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
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${uiState.captions.size} caption segments",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                TextButton(onClick = { viewModel.clearAllCaptions() }) {
                                    Text("Clear All", color = ErrorColor)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
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
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                2 -> {
                    item { Spacer(Modifier.height(16.dp)) }
                    item { SectionHeader(title = "Transcription Language") }
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                uiState.availableLanguages.forEach { (code, name) ->
                                    LanguageItem(
                                        name = name,
                                        code = code,
                                        isSelected = uiState.selectedLanguage == code,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.updateLanguage(code)
                                        }
                                    )
                                    if (code != uiState.availableLanguages.last().first) {
                                        Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    item { Spacer(Modifier.height(16.dp)) }
                    item { SectionHeader(title = "Background") }
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        BackgroundControls(
                            backgroundColor = uiState.currentCaptionStyle.backgroundColor,
                            backgroundCornerRadius = uiState.currentCaptionStyle.backgroundCornerRadius,
                            backgroundPadding = uiState.currentCaptionStyle.backgroundPadding,
                            onBackgroundChange = { viewModel.updateBackgroundColor(it) },
                            onCornerRadiusChange = { viewModel.updateBackgroundCornerRadius(it) },
                            onPaddingChange = { viewModel.updateBackgroundPadding(it) }
                        )
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                    item { SectionHeader(title = "Outline") }
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        OutlineControls(
                            outlineWidth = uiState.currentCaptionStyle.outlineWidth,
                            onOutlineWidthChange = { viewModel.updateOutlineWidth(it) }
                        )
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                    item { SectionHeader(title = "Shadow") }
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        ShadowControls(
                            shadow = uiState.currentCaptionStyle.shadow,
                            onShadowChange = { viewModel.updateShadow(it) }
                        )
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                    item { SectionHeader(title = "Text Case") }
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        CaseStyleControls(
                            caseStyle = uiState.currentCaptionStyle.caseStyle,
                            onCaseStyleChange = { viewModel.updateCaseStyle(it) }
                        )
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                    item { SectionHeader(title = "Alignment") }
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        AlignmentControls(
                            alignment = uiState.currentCaptionStyle.alignment,
                            onAlignmentChange = { viewModel.updateAlignment(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSection(
    label: String,
    color: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(2.dp, DarkBorder, CircleShape)
                )
            }
            Spacer(Modifier.height(12.dp))
            ColorPickerRow(
                selectedColor = color,
                onColorSelected = onColorSelected,
                colors = listOf(
                    0xFFFFFFFF, 0xFFFBBF24, 0xFF34D399, 0xFF60A5FA,
                    0xFFF472B6, 0xFFA78BFA, 0xFFEF4444, 0xFFF97316,
                    0xFF06B6D4, 0xFF10B981, 0xFF000000, 0xFF7C3AED
                )
            )
        }
    }
}

@Composable
private fun FontControls(
    fontSize: Int,
    fontWeight: FontWeight,
    onFontSizeChange: (Int) -> Unit,
    onFontWeightChange: (FontWeight) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Font Size", style = MaterialTheme.typography.titleSmall)
                Text("${fontSize}sp", color = ViralPurple, fontWeight = ComposeFontWeight.Bold)
            }
            Slider(
                value = fontSize.toFloat(),
                onValueChange = { onFontSizeChange(it.toInt()) },
                valueRange = 16f..72f,
                colors = SliderDefaults.colors(
                    thumbColor = ViralPurple,
                    activeTrackColor = ViralPurple,
                    inactiveTrackColor = ViralPurple.copy(alpha = 0.15f)
                )
            )

            Spacer(Modifier.height(12.dp))

            Text("Font Weight", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FontWeight.entries.take(4).forEach { weight ->
                    FilterChip(
                        selected = fontWeight == weight,
                        onClick = { onFontWeightChange(weight) },
                        label = { Text(weight.name.take(4), fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                            selectedLabelColor = ViralPurple
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PositionControls(
    position: CaptionPosition,
    onPositionChange: (CaptionPosition) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    CaptionPosition.TOP to Icons.Filled.VerticalAlignTop,
                    CaptionPosition.CENTER to Icons.Filled.VerticalAlignCenter,
                    CaptionPosition.BOTTOM to Icons.Filled.VerticalAlignBottom
                ).forEach { (pos, icon) ->
                    FilterChip(
                        selected = position == pos,
                        onClick = { onPositionChange(pos) },
                        label = { Text(pos.displayName) },
                        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                            selectedLabelColor = ViralPurple
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimationControls(
    animation: CaptionAnimation,
    onAnimationChange: (CaptionAnimation) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CaptionAnimation.entries.forEach { anim ->
                    FilterChip(
                        selected = animation == anim,
                        onClick = { onAnimationChange(anim) },
                        label = { Text(anim.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                            selectedLabelColor = ViralPurple
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundControls(
    backgroundColor: Long,
    backgroundCornerRadius: Float,
    backgroundPadding: Float,
    onBackgroundChange: (Long) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onPaddingChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Background Color", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(backgroundColor))
                            .border(2.dp, DarkBorder, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = backgroundColor != 0L,
                        onCheckedChange = { onBackgroundChange(if (it) 0x80000000 else 0L) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ViralPurple,
                            checkedTrackColor = ViralPurple.copy(alpha = 0.3f)
                        )
                    )
                }
            }
            if (backgroundColor != 0L) {
                ColorPickerRow(
                    selectedColor = backgroundColor,
                    onColorSelected = onBackgroundChange,
                    colors = listOf(
                        0x80000000, 0x80708090, 0x80333333, 0x80FFFFFF,
                        0x807C3AED, 0x80EC4899, 0x8006B6D4, 0x80EF4444
                    )
                )
                Spacer(Modifier.height(16.dp))
                SliderSettingRow(
                    label = "Corner Radius",
                    value = backgroundCornerRadius,
                    onValueChange = onCornerRadiusChange,
                    valueRange = 0f..24f,
                    valueText = "${backgroundCornerRadius.toInt()}dp"
                )
                Spacer(Modifier.height(8.dp))
                SliderSettingRow(
                    label = "Padding",
                    value = backgroundPadding,
                    onValueChange = onPaddingChange,
                    valueRange = 0f..24f,
                    valueText = "${backgroundPadding.toInt()}dp"
                )
            }
        }
    }
}

@Composable
private fun OutlineControls(
    outlineWidth: Float,
    onOutlineWidthChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Outline Width", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = outlineWidth > 0f,
                    onCheckedChange = { onOutlineWidthChange(if (it) 2f else 0f) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ViralPurple,
                        checkedTrackColor = ViralPurple.copy(alpha = 0.3f)
                    )
                )
            }
            if (outlineWidth > 0f) {
                Spacer(Modifier.height(8.dp))
                SliderSettingRow(
                    label = "Width",
                    value = outlineWidth,
                    onValueChange = onOutlineWidthChange,
                    valueRange = 1f..8f,
                    valueText = "${outlineWidth}px"
                )
            }
        }
    }
}

@Composable
private fun ShadowControls(
    shadow: CaptionShadow,
    onShadowChange: (CaptionShadow) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Text Shadow", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = shadow.enabled,
                    onCheckedChange = { onShadowChange(shadow.copy(enabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ViralPurple,
                        checkedTrackColor = ViralPurple.copy(alpha = 0.3f)
                    )
                )
            }
            if (shadow.enabled) {
                Spacer(Modifier.height(12.dp))
                SliderSettingRow(
                    label = "Blur Radius",
                    value = shadow.blurRadius,
                    onValueChange = { onShadowChange(shadow.copy(blurRadius = it)) },
                    valueRange = 0f..16f,
                    valueText = "${shadow.blurRadius.toInt()}px"
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Offset X", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Slider(
                            value = shadow.offsetX,
                            onValueChange = { onShadowChange(shadow.copy(offsetX = it)) },
                            valueRange = -8f..8f,
                            colors = SliderDefaults.colors(
                                thumbColor = ViralPurple,
                                activeTrackColor = ViralPurple
                            )
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Offset Y", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Slider(
                            value = shadow.offsetY,
                            onValueChange = { onShadowChange(shadow.copy(offsetY = it)) },
                            valueRange = -8f..8f,
                            colors = SliderDefaults.colors(
                                thumbColor = ViralPurple,
                                activeTrackColor = ViralPurple
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaseStyleControls(
    caseStyle: CaseStyle,
    onCaseStyleChange: (CaseStyle) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
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
                        selected = caseStyle == cs,
                        onClick = { onCaseStyleChange(cs) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                            selectedLabelColor = ViralPurple
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AlignmentControls(
    alignment: CaptionAlignment,
    onAlignmentChange: (CaptionAlignment) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    CaptionAlignment.LEFT to Icons.Filled.FormatAlignLeft,
                    CaptionAlignment.CENTER to Icons.Filled.FormatAlignCenter,
                    CaptionAlignment.RIGHT to Icons.Filled.FormatAlignRight
                ).forEach { (align, icon) ->
                    FilterChip(
                        selected = alignment == align,
                        onClick = { onAlignmentChange(align) },
                        label = { Text(align.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ViralPurple.copy(alpha = 0.15f),
                            selectedLabelColor = ViralPurple
                        )
                    )
                }
            }
        }
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
            .padding(horizontal = 16.dp),
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
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge, color = if (isSelected) ViralPurple else TextPrimary)
        if (isSelected) {
            Icon(Icons.Filled.CheckCircle, null, tint = ViralPurple, modifier = Modifier.size(22.dp))
        }
    }
}
