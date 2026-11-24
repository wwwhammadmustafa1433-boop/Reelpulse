package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.recommendation.ScoredVideo
import com.example.ui.theme.PulseAmber
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseSurface
import com.example.ui.theme.PulseSurfaceElevated
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.theme.PulseTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationBreakdownSheet(
    scoredVideo: ScoredVideo,
    onOpenAlgorithmLab: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val b = scoredVideo.breakdown

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PulseSurface,
        contentColor = PulseTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = PulseCyan
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.algo_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${scoredVideo.video.title} • ${scoredVideo.primaryReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PulseCyan.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "${scoredVideo.matchScore}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PulseCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PulseSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SignalRow("Creator Follow Boost", b.followPoints, 24, PulseEmerald)
                    SignalRow("Category Affinity (Watch + Likes)", b.categoryPoints, 24, PulseCrimson)
                    SignalRow("Topic / Hashtag Overlap", b.tagPoints, 14, PulseCyan)
                    SignalRow("Creator Watch Loyalty", b.creatorLoyaltyPoints, 10, PulseAmber)
                    SignalRow("Freshness & Exploration", b.explorationPoints, 16, Color(0xFFA78BFA))
                    if (b.repeatPenaltyPoints < 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Recent Watch / Skip Adjustment",
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTextSecondary
                            )
                            Text(
                                text = "${b.repeatPenaltyPoints} pts",
                                style = MaterialTheme.typography.labelMedium,
                                color = PulseAmber
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onDismiss()
                    onOpenAlgorithmLab()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PulseCyan,
                    contentColor = PulseObsidian
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Open For You AI & Roadmap",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun SignalRow(
    label: String,
    points: Int,
    maxPoints: Int,
    color: Color
) {
    val fraction = (points.toFloat() / maxPoints.toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = PulseTextPrimary
            )
            Text(
                text = "+$points / $maxPoints",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = Color.White.copy(alpha = 0.08f),
            strokeCap = StrokeCap.Round
        )
    }
}
