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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viralclip.app.domain.model.*
import com.viralclip.app.ui.components.*
import com.viralclip.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit
) {
    val brandPresets = remember {
        listOf(
            BrandPreset(1, "My Brand", 0xFF7C3AED, 0xFFEC4899, 0xFF3B82F6, "default", null, false, null),
            BrandPreset(2, "Tech Channel", 0xFF3B82F6, 0xFF06B6D4, 0xFF10B981, "default", null, false, null),
            BrandPreset(3, "Gaming", 0xFFEF4444, 0xFFF97316, 0xFFFBBF24, "default", null, false, null)
        )
    }

    Scaffold(
        topBar = { GradientTopBar("Brand Presets", onBack = onNavigateBack) },
        containerColor = DarkBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEditor(-1) },
                containerColor = ViralPurple,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Create Brand")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            brandPresets.forEach { preset ->
                BrandPresetCard(
                    preset = preset,
                    onClick = { onNavigateToEditor(preset.id) }
                )
            }

            Spacer(Modifier.height(120.dp))
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
                    Text("Colors", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.padding(start = 4.dp))
                }
            }

            Icon(Icons.Filled.ChevronRight, null, tint = TextTertiary)
        }
    }
}
