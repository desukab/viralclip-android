package com.viralclip.app.ui.screens.brand

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.viralclip.app.R
import com.viralclip.app.domain.model.BrandPreset
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(8.dp))

                uiState.presets.forEach { preset ->
                    BrandPresetCard(
                        preset = preset,
                        onClick = { onNavigateToEditor(preset.id) }
                    )
                }

                Spacer(Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun BrandPresetCard(
    preset: BrandPreset,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color palette preview
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
                Text(preset.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(preset.primaryColor, preset.secondaryColor, preset.accentColor).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                        )
                    }
                    Text(stringResource(R.string.brand_colors_label), style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.padding(start = 4.dp))
                }
                if (preset.watermarkEnabled) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.brand_watermark_label, preset.watermarkText ?: ""),
                        style = MaterialTheme.typography.labelSmall, color = ViralPurple)
                }
            }

            Icon(Icons.Filled.ChevronRight, stringResource(R.string.nav_back), tint = TextTertiary)
        }
    }
}
