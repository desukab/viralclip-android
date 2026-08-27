package com.viralclip.app.ui.screens.brand

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.viralclip.app.R
import com.viralclip.app.domain.model.BrandPreset
import com.viralclip.app.domain.model.CaptionStyle
import com.viralclip.app.domain.model.CaptionPreset
import com.viralclip.app.domain.model.FontWeight as DomainFontWeight
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*
import com.viralclip.app.ui.viewmodels.BrandViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit,
    viewModel: BrandViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.seedDefaultPresets()
    }

    Scaffold(
        topBar = { GradientTopBar(stringResource(R.string.brand_title), onBack = onNavigateBack) },
        containerColor = DarkBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEditor(-1) },
                containerColor = ViralPurple,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, stringResource(R.string.brand_create))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.brand_create))
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ViralPurple)
            }
        } else if (uiState.presets.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.brand_no_presets),
                message = stringResource(R.string.brand_no_presets_desc),
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                }

                items(uiState.presets) { preset ->
                    BrandPresetCard(
                        preset = preset,
                        onClick = { onNavigateToEditor(preset.id) },
                        onDelete = { viewModel.deletePreset(preset.id) }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                item {
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun BrandPresetCard(
    preset: BrandPreset,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(preset.primaryColor),
                                    Color(preset.secondaryColor),
                                    Color(preset.accentColor),
                                    Color(preset.primaryColor)
                                )
                            )
                        )
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(preset.primaryColor, preset.secondaryColor, preset.accentColor).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(1.dp, DarkBorder, CircleShape)
                            )
                        }
                    }
                    if (preset.watermarkEnabled && !preset.watermarkText.isNullOrEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.brand_watermark_label, preset.watermarkText),
                            style = MaterialTheme.typography.labelSmall,
                            color = ViralPurple
                        )
                    }
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        stringResource(R.string.delete),
                        tint = ErrorColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Preset") },
            text = { Text("Are you sure you want to delete '${preset.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete), color = ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandEditorScreen(
    brandId: Long,
    onNavigateBack: () -> Unit,
    onSave: (BrandPreset) -> Unit,
    viewModel: BrandViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var primaryColor by remember { mutableStateOf(0xFF7C3AEDL) }
    var secondaryColor by remember { mutableStateOf(0xFFEC4899L) }
    var accentColor by remember { mutableStateOf(0xFF3B82F6L) }
    var watermarkEnabled by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf("") }
    var showColorPicker by remember { mutableStateOf<ColorPickerTarget?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(brandId) {
        if (brandId > 0) {
            uiState.presets.find { it.id == brandId }?.let { preset ->
                name = preset.name
                primaryColor = preset.primaryColor
                secondaryColor = preset.secondaryColor
                accentColor = preset.accentColor
                watermarkEnabled = preset.watermarkEnabled
                watermarkText = preset.watermarkText ?: ""
                isEditing = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Edit Brand" else stringResource(R.string.brand_create),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                val preset = BrandPreset(
                                    id = if (isEditing) brandId else 0,
                                    name = name,
                                    primaryColor = primaryColor,
                                    secondaryColor = secondaryColor,
                                    accentColor = accentColor,
                                    watermarkEnabled = watermarkEnabled,
                                    watermarkText = if (watermarkEnabled) watermarkText else null
                                )
                                onSave(preset)
                                onNavigateBack()
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text(stringResource(R.string.save), color = ViralPurple)
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Brand Name",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter brand name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ViralPurple,
                                cursorColor = ViralPurple
                            )
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            stringResource(R.string.brand_colors),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(16.dp))

                        ColorOption(
                            label = "Primary Color",
                            color = Color(primaryColor),
                            onClick = { showColorPicker = ColorPickerTarget.PRIMARY }
                        )
                        Spacer(Modifier.height(12.dp))
                        ColorOption(
                            label = "Secondary Color",
                            color = Color(secondaryColor),
                            onClick = { showColorPicker = ColorPickerTarget.SECONDARY }
                        )
                        Spacer(Modifier.height(12.dp))
                        ColorOption(
                            label = "Accent Color",
                            color = Color(accentColor),
                            onClick = { showColorPicker = ColorPickerTarget.ACCENT }
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        ToggleSettingRow(
                            title = stringResource(R.string.brand_watermark),
                            subtitle = "Add watermark to exported videos",
                            checked = watermarkEnabled,
                            onCheckedChange = { watermarkEnabled = it }
                        )
                        if (watermarkEnabled) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = watermarkText,
                                onValueChange = { watermarkText = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Watermark text") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ViralPurple,
                                    cursorColor = ViralPurple
                                )
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Preview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(primaryColor), Color(secondaryColor))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Sample Caption",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Your brand colors in action",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                "Primary" to primaryColor,
                                "Secondary" to secondaryColor,
                                "Accent" to accentColor
                            ).forEach { (label, color) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(color))
                                            .border(2.dp, DarkBorder, CircleShape)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    showColorPicker?.let { target ->
        ColorPickerDialog(
            initialColor = when (target) {
                ColorPickerTarget.PRIMARY -> primaryColor
                ColorPickerTarget.SECONDARY -> secondaryColor
                ColorPickerTarget.ACCENT -> accentColor
            },
            onDismiss = { showColorPicker = null },
            onColorSelected = { color ->
                when (target) {
                    ColorPickerTarget.PRIMARY -> primaryColor = color
                    ColorPickerTarget.SECONDARY -> secondaryColor = color
                    ColorPickerTarget.ACCENT -> accentColor = color
                }
                showColorPicker = null
            }
        )
    }
}

@Composable
private fun ColorOption(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, DarkBorder, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.ChevronRight,
                null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private enum class ColorPickerTarget {
    PRIMARY, SECONDARY, ACCENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerDialog(
    initialColor: Long,
    onDismiss: () -> Unit,
    onColorSelected: (Long) -> Unit
) {
    val presetColors = listOf(
        0xFF7C3AED, 0xFFEC4899, 0xFF3B82F6, 0xFF06B6D4,
        0xFF10B981, 0xFFF97316, 0xFFEF4444, 0xFFFBBF24,
        0xFF8B5CF6, 0xFFF472B6, 0xFF60A5FA, 0xFF34D399,
        0xFFA78BFA, 0xFFFB7185, 0xFF818CF8, 0xFF2DD4BF,
        0xFFFFFFFF, 0xFF000000, 0xFF1F2937, 0xFFF3F4F6
    )

    var customRed by remember { mutableFloatStateOf(((initialColor shr 16) and 0xFF).toFloat()) }
    var customGreen by remember { mutableFloatStateOf(((initialColor shr 8) and 0xFF).toFloat()) }
    var customBlue by remember { mutableFloatStateOf((initialColor and 0xFF).toFloat()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Color") },
        text = {
            Column {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = ViralPurple
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Presets", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Custom", modifier = Modifier.padding(12.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(presetColors) { color ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .border(
                                            width = if (color == initialColor) 3.dp else 1.dp,
                                            color = if (color == initialColor) ViralPurple else DarkBorder,
                                            shape = CircleShape
                                        )
                                        .clickable { onColorSelected(color) }
                                )
                            }
                        }
                    }
                    1 -> {
                        Column {
                            val customColor = ((customRed.toInt() shl 16) or (customGreen.toInt() shl 8) or customBlue.toInt())
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(customColor))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.height(16.dp))
                            ColorSlider("Red", customRed, { customRed = it }, Color.Red)
                            Spacer(Modifier.height(8.dp))
                            ColorSlider("Green", customGreen, { customGreen = it }, Color.Green)
                            Spacer(Modifier.height(8.dp))
                            ColorSlider("Blue", customBlue, { customBlue = it }, Color.Blue)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val color = ((customRed.toInt() shl 16) or (customGreen.toInt() shl 8) or customBlue.toInt())
                onColorSelected(color.toLong() or 0xFF000000)
            }) {
                Text(stringResource(R.string.save), color = ViralPurple)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = DarkSurfaceElevated
    )
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("${value.toInt()}", style = MaterialTheme.typography.labelMedium, color = color)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = DarkBorder
            )
        )
    }
}
