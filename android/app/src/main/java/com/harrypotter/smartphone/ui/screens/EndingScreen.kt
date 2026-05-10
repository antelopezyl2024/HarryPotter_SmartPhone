package com.harrypotter.smartphone.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.harrypotter.smartphone.ui.components.DementorEffect
import com.harrypotter.smartphone.ui.components.FloatingParticles
import com.harrypotter.smartphone.ui.components.GoldenSnitchCanvas
import com.harrypotter.smartphone.ui.components.LottiePatronus
import com.harrypotter.smartphone.ui.components.StarRain
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harrypotter.smartphone.data.model.GetEndingResponse
import com.harrypotter.smartphone.ui.theme.*

@Composable
fun EndingScreen(ending: GetEndingResponse, onPlayAgain: () -> Unit) {
    val isSuccess = ending.endingId.contains("success", ignoreCase = true)
    val accentColor = if (isSuccess) HPCorrect else HPWrong
    val bgGradient = if (isSuccess) {
        Brush.verticalGradient(listOf(Color(0xFF0A2010), HPBackground))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF200A0A), HPBackground))
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        if (isSuccess) {
            StarRain()
            LottiePatronus(modifier = Modifier.align(Alignment.Center).size(280.dp))
            GoldenSnitchCanvas(modifier = Modifier.align(Alignment.TopEnd).size(110.dp).padding(top = 56.dp, end = 12.dp))
        } else {
            FloatingParticles(count = 10, color = HPWrong.copy(alpha = 0.4f))
            DementorEffect(visible = true, alpha = 0.28f)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(72.dp))
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(tween(600)) + fadeIn(tween(600))
                ) {
                    EndingBadge(isSuccess, accentColor)
                }
                Spacer(Modifier.height(24.dp))
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(800, delayMillis = 300))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = ending.title,
                            style = MaterialTheme.typography.displaySmall,
                            color = accentColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = ending.narrative,
                            style = MaterialTheme.typography.bodyLarge,
                            color = HPParchment,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        ScoreChip(ending.totalScore, accentColor)
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = HPGold.copy(alpha = 0.3f))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Your Journey",
                            style = MaterialTheme.typography.titleMedium,
                            color = HPGold
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            item {
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(600, delayMillis = 600))) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ending.decisionsOverview.forEach { entry ->
                            Icon(
                                imageVector = if (entry.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (entry.isCorrect) HPCorrect else HPWrong,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(500, delayMillis = 900))) {
                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HPMaroon)
                    ) {
                        Text("Play Again", style = MaterialTheme.typography.titleMedium, color = HPParchment)
                    }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun EndingBadge(isSuccess: Boolean, color: Color) {
    val scale by rememberInfiniteTransition(label = "badge").animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "badgeScale"
    )
    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(60.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(64.dp)
        )
    }
}

@Composable
private fun ScoreChip(score: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = 0.15f),
        border = ButtonDefaults.outlinedButtonBorder()
    ) {
        Text(
            text = "Final Score: $score",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
    }
}

