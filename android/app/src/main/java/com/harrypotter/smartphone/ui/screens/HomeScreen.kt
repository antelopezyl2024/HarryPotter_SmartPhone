package com.harrypotter.smartphone.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harrypotter.smartphone.data.model.DLCSummary
import com.harrypotter.smartphone.ui.components.FloatingParticles
import com.harrypotter.smartphone.ui.components.HogwartsSilhouette
import com.harrypotter.smartphone.ui.components.LottieSnitch
import com.harrypotter.smartphone.ui.components.WandTrace
import com.harrypotter.smartphone.ui.theme.*
import com.harrypotter.smartphone.viewmodel.GameUiState

@Composable
fun HomeScreen(
    state: GameUiState,
    onSelectDLC: (String) -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(HPBackground, Color(0xFF1A1A2E), HPBackground))
            )
    ) {
        FloatingParticles(count = 22)
        HogwartsSilhouette(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.42f)
        )

        when (state) {
            is GameUiState.Loading -> LoadingOverlay()
            is GameUiState.DLCSelection -> DLCSelectionContent(state.dlcs, onSelectDLC)
            is GameUiState.Error -> ErrorContent(state.message, onRetry)
            else -> Unit
        }
    }
}

@Composable
private fun DLCSelectionContent(dlcs: List<DLCSummary>, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        PulsingTitle()

        Spacer(Modifier.height(12.dp))
        // Lottie spinning snitch (falls back gracefully if JSON is invalid)
        LottieSnitch(modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(4.dp))
        
        Text(
            text = "Choose your adventure",
            style = MaterialTheme.typography.titleLarge,
            color = HPParchment,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))
        WandTrace(modifier = Modifier.fillMaxWidth().height(6.dp))

        Spacer(Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(dlcs) { dlc ->
                DLCCard(dlc, onSelect)
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun PulsingTitle() {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    Text(
        text = "⚡ Hogwarts Awaits ⚡",
        style = MaterialTheme.typography.displaySmall,
        color = HPGold.copy(alpha = glowAlpha),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun DLCCard(dlc: DLCSummary, onSelect: (String) -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Card(
        onClick = { onSelect(dlc.dlcId) },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C2E)),
        border = BorderStroke(1.dp, HPGold.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = dlc.title,
                style = MaterialTheme.typography.titleMedium,
                color = HPGold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = dlc.description,
                style = MaterialTheme.typography.bodyMedium,
                color = HPParchment.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Max Score: ${dlc.maxScore}",
                    style = MaterialTheme.typography.labelMedium,
                    color = HPVeilBlue
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Begin →",
                    style = MaterialTheme.typography.labelLarge,
                    color = HPGold
                )
            }
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = HPGold)
            Spacer(Modifier.height(16.dp))
            Text("Summoning the magic…", color = HPParchment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium, color = HPWrong)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = HPParchment.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = HPMaroon)
            ) {
                Text("Try Again", color = HPParchment)
            }
        }
    }
}
