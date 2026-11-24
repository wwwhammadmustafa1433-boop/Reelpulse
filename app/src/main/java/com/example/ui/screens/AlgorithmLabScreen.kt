package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.recommendation.AlgorithmRoadmapStage
import com.example.ui.theme.PulseAmber
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseOutline
import com.example.ui.theme.PulseSurface
import com.example.ui.theme.PulseSurfaceElevated
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.theme.PulseTextSecondary
import com.example.ui.viewmodel.ReelPulseUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlgorithmLabScreen(
    uiState: ReelPulseUiState,
    onExplorationRateChange: (Float) -> Unit,
    onResetSignals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = uiState.interestProfile
    val videoById = uiState.allScoredVideos.associateBy { it.video.id }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseObsidian)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = PulseCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.algo_lab_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = PulseTextPrimary
                            )
                        }
                        Text(
                            text = stringResource(R.string.algo_lab_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = PulseTextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = onResetSignals,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("reset_signals_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = null,
                            tint = PulseCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Reset", color = PulseCrimson)
                    }
                }
            }

            // Live Signal Telemetry Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryStatCard(
                        label = "Watches",
                        value = "${profile.totalWatches}",
                        accent = PulseCyan,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryStatCard(
                        label = "Avg Finish",
                        value = "${(profile.avgCompletionRate * 100).roundToInt()}%",
                        accent = PulseEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryStatCard(
                        label = "Likes",
                        value = "${profile.totalLikes}",
                        accent = PulseCrimson,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryStatCard(
                        label = "Follows",
                        value = "${profile.totalFollows}",
                        accent = PulseAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Exploration vs Exploitation Control
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = PulseSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PulseOutline, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Exploration vs. Precision",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PulseCyan.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "${(uiState.explorationRate * 100).roundToInt()}% Explore",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PulseCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "Higher exploration surfaces new categories outside your watch history to prevent filter bubbles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PulseTextSecondary
                        )
                        Slider(
                            value = uiState.explorationRate,
                            onValueChange = onExplorationRateChange,
                            valueRange = 0.05f..0.50f,
                            colors = SliderDefaults.colors(
                                thumbColor = PulseCyan,
                                activeTrackColor = PulseCyan,
                                inactiveTrackColor = PulseSurfaceElevated
                            ),
                            modifier = Modifier.testTag("exploration_slider")
                        )
                    }
                }
            }

            // Live User Interest Vector
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = PulseSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PulseOutline, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Your Live Interest Vector",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (profile.topCategories.isEmpty()) {
                            Text(
                                text = "Watch videos, double-tap to like, or follow creators in the Feed to watch your personal taste vector build in real time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTextSecondary
                            )
                        } else {
                            profile.topCategories.take(6).forEach { cat ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = cat.category,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${(cat.normalizedWeight * 100).roundToInt()}% weight",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PulseCrimson
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { cat.normalizedWeight },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp),
                                        color = PulseCrimson,
                                        trackColor = PulseSurfaceElevated,
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }
                            if (profile.topTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    profile.topTags.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PulseSurfaceElevated
                                        ) {
                                            Text(
                                                text = tag.tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PulseCyan,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent Watch Signals
            if (uiState.watchHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Watch Signals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(uiState.watchHistory.take(5), key = { it.id }) { watch ->
                    val title = videoById[watch.videoId]?.video?.title ?: watch.videoId
                    val pct = (watch.completionRate * 100).roundToInt()
                    val isPositive = watch.completionRate >= 0.50f
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PulseSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${watch.category} • ${watch.watchTimeMs / 1000}s watched",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PulseTextSecondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPositive) PulseEmerald.copy(alpha = 0.16f) else PulseAmber.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = if (isPositive) "$pct% +Boost" else "$pct% Skip",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPositive) PulseEmerald else PulseAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Algorithm Improvement Roadmap Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TrendingUp,
                        contentDescription = null,
                        tint = PulseEmerald
                    )
                    Text(
                        text = "How to Improve This Algorithm Over Time",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(uiState.roadmapStages, key = { it.stageNumber }) { stage ->
                RoadmapStageCard(stage = stage)
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun TelemetryStatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PulseSurface,
        modifier = modifier.border(1.dp, PulseOutline, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = PulseTextSecondary
            )
        }
    }
}

@Composable
private fun RoadmapStageCard(stage: AlgorithmRoadmapStage) {
    val isActive = stage.stageNumber == 1
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PulseSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isActive) PulseCyan.copy(alpha = 0.6f) else PulseOutline,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stage.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) PulseCyan.copy(alpha = 0.18f) else PulseSurfaceElevated
                ) {
                    Text(
                        text = stage.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) PulseCyan else PulseTextSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = stage.summary,
                style = MaterialTheme.typography.bodySmall,
                color = PulseTextSecondary
            )
            stage.keyTechniques.forEach { technique ->
                Text(
                    text = "• $technique",
                    style = MaterialTheme.typography.bodySmall,
                    color = PulseTextPrimary
                )
            }
        }
    }
}
