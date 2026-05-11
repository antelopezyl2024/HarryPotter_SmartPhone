package com.harrypotter.smartphone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harrypotter.smartphone.ui.components.FloatingParticles
import com.harrypotter.smartphone.ui.components.HogwartsSilhouette
import com.harrypotter.smartphone.ui.components.WandTrace
import com.harrypotter.smartphone.ui.theme.*

@Composable
fun ProductionScreen(onBack: () -> Unit) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚡ Coming Soon ⚡",
                style = MaterialTheme.typography.displaySmall,
                color = HPGold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
            WandTrace(modifier = Modifier.fillMaxWidth().height(6.dp))
            Spacer(Modifier.height(24.dp))

            Text(
                text = "Content is currently in production.\nPlease watch for release announcements.",
                style = MaterialTheme.typography.titleLarge,
                color = HPParchment,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = HPMaroon),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Return to Great Hall",
                    color = HPParchment,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}
