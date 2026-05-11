package com.harrypotter.smartphone.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.harrypotter.smartphone.data.model.*
import com.harrypotter.smartphone.ui.components.DementorEffect
import com.harrypotter.smartphone.ui.components.FloatingParticles
import com.harrypotter.smartphone.ui.components.FloatingWizardSilhouette
import com.harrypotter.smartphone.ui.components.SparkleBurst
import com.harrypotter.smartphone.ui.components.WandTrace
import com.harrypotter.smartphone.ui.theme.*
import com.harrypotter.smartphone.viewmodel.GameUiState
import kotlinx.coroutines.delay

private const val TOTAL_SCENES = 5

@Composable
fun SceneScreen(
    state: GameUiState,
    onSubmitChoice: (String, String, String, Scene) -> Unit,
    onSubmitFreeText: (String, String, String, Scene) -> Unit,
    onProceed: (SubmitDecisionResponse, String) -> Unit,
    onHermioneClick: () -> Unit,
    onGameEnd: () -> Unit,
    onRetry: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HPBackground, Color(0xFF141428), HPBackground)))
    ) {
        FloatingParticles(count = 12, color = HPGold.copy(alpha = 0.7f))
        when (state) {
            is GameUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = HPGold)
                    Spacer(Modifier.height(16.dp))
                    Text("Summoning the magic…", color = HPParchment, style = MaterialTheme.typography.bodyMedium)
                }
            }

            is GameUiState.InScene -> SceneContent(
                playthrough = state.playthrough,
                scene = state.scene,
                onSubmitChoice = onSubmitChoice,
                onSubmitFreeText = onSubmitFreeText,
                onHermioneClick = onHermioneClick
            )

            is GameUiState.Feedback -> FeedbackContent(
                playthrough = state.playthrough,
                response = state.response,
                prevScene = state.prevScene,
                onProceed = onProceed
            )

            is GameUiState.Error -> SceneErrorContent(message = state.message, onRetry = onRetry)

            else -> Unit
        }
    }
}

// ── Scene content ─────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
private fun SceneContent(
    playthrough: Playthrough,
    scene: Scene,
    onSubmitChoice: (String, String, String, Scene) -> Unit,
    onSubmitFreeText: (String, String, String, Scene) -> Unit,
    onHermioneClick: () -> Unit
) {
    var freeText by remember(scene.sceneId) { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    // Logic for Scene 1 sequence: Video intro then content
    var isContentVisible by remember(scene.sceneId) { mutableStateOf(scene.order != 1) }

    val exoPlayer = remember(scene.sceneId) {
        if (scene.order == 1) {
            ExoPlayer.Builder(context).build().apply {
                val uri = Uri.parse("asset:///videos/S1background.mov")
                setMediaItem(MediaItem.fromUri(uri))
                // Play once initially to act as intro
                repeatMode = Player.REPEAT_MODE_OFF
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            // Video finished: show text and set to loop
                            isContentVisible = true
                            repeatMode = Player.REPEAT_MODE_ALL
                            playWhenReady = true
                        }
                    }
                })
            }
        } else null
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Layer
        if (scene.order == 1 && exoPlayer != null) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // Overlay to ensure text readability after fade-in
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isContentVisible) 0.45f else 0.1f))
            )
        } else {
            FloatingWizardSilhouette(modifier = Modifier.fillMaxSize())
        }

        // Foreground Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp)
        ) {
            Spacer(Modifier.height(56.dp))

            SceneProgressBar(currentScene = scene.order, totalScenes = TOTAL_SCENES, score = playthrough.totalScore)

            Spacer(Modifier.height(20.dp))

            // Text and choices fade in only after intro animation if Scene 1
            AnimatedVisibility(
                visible = isContentVisible,
                enter = fadeIn(tween(1000))
            ) {
                Column {
                    Text(
                        text = "Scene ${scene.order}: ${scene.title}",
                        style = MaterialTheme.typography.titleLarge,
                        color = HPGold
                    )
                    Spacer(Modifier.height(6.dp))
                    WandTrace(modifier = Modifier.fillMaxWidth().height(6.dp))

                    Spacer(Modifier.height(12.dp))

                    NarrativeCard(narrative = scene.narrative, isTransparent = scene.order == 1)

                    Spacer(Modifier.height(24.dp))

                    val mode = scene.decision.mode
                    val showChoice = (mode == "CHOICE" || mode == "EITHER") && scene.decision.choice != null
                    val showFreeText = (mode == "FREE_TEXT" || mode == "EITHER") && scene.decision.freeText != null

                    if (showChoice) {
                        Text(
                            text = "Choose your path:",
                            style = MaterialTheme.typography.labelLarge,
                            color = HPParchment.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(12.dp))
                        scene.decision.choice!!.options.forEachIndexed { index, option ->
                            val label = ('A' + index).toString()
                            SpellButton(
                                label = label,
                                text = option.text,
                                onClick = { onSubmitChoice(playthrough.playthroughId, scene.sceneId, option.id, scene) }
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    if (showFreeText) {
                        if (showChoice) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = HPGold.copy(alpha = 0.2f))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "— or speak your mind —",
                                style = MaterialTheme.typography.labelSmall,
                                color = HPParchment.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        val config = scene.decision.freeText!!
                        Column {
                            Text(
                                text = config.prompt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = HPParchment.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = freeText,
                                onValueChange = { if (it.length <= config.maxChars) freeText = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Your response…", color = HPParchment.copy(alpha = 0.4f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HPGold,
                                    unfocusedBorderColor = HPGold.copy(alpha = 0.3f),
                                    focusedTextColor = HPParchment,
                                    unfocusedTextColor = HPParchment
                                ),
                                maxLines = 4,
                                trailingIcon = {
                                    val haptic = LocalHapticFeedback.current
                                    IconButton(onClick = {
                                        if (freeText.isNotBlank()) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSubmitFreeText(playthrough.playthroughId, scene.sceneId, freeText, scene)
                                        }
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Submit", tint = HPGold)
                                    }
                                },
                                supportingText = {
                                    Text(
                                        "${freeText.length}/${config.maxChars}",
                                        color = HPParchment.copy(alpha = 0.4f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onHermioneClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = HPMaroon,
            contentColor = HPGold,
            shape = CircleShape
        ) {
            Text("✨", style = MaterialTheme.typography.titleLarge)
        }
    }
}

// ── Feedback content ──────────────────────────────────────────────────────────

@Composable
private fun FeedbackContent(
    playthrough: Playthrough,
    response: SubmitDecisionResponse,
    prevScene: Scene,
    onProceed: (SubmitDecisionResponse, String) -> Unit
) {
    val isCorrect = response.isCorrect
    val accentColor = if (isCorrect) HPCorrect else HPWrong

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Shake state for wrong answers
    var shake by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!isCorrect) {
            delay(550)
            shake = true
            delay(500)
            shake = false
        }
    }
    val shakeOffset by animateFloatAsState(
        targetValue = if (shake) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.2f, stiffness = Spring.StiffnessHigh),
        label = "shake"
    )
    val shakeX = if (!isCorrect) (shakeOffset * 18f * kotlin.math.sin(shakeOffset * 18f)).toInt() else 0

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCorrect) FloatingParticles(count = 16, color = HPCorrect.copy(alpha = 0.6f))
        DementorEffect(modifier = Modifier.fillMaxSize(), visible = !isCorrect, alpha = 0.24f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            AnimatedVisibility(visible = visible, enter = scaleIn(tween(500)) + fadeIn(tween(500))) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.offset { IntOffset(shakeX, 0) }
                ) {
                    if (isCorrect) {
                        SparkleBurst(modifier = Modifier.size(200.dp), trigger = visible, color = HPCorrect)
                    }
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isCorrect) "✓" else "✗",
                                style = MaterialTheme.typography.displayMedium,
                                color = accentColor
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, delayMillis = 200))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isCorrect) "Well done!" else "Not quite…",
                        style = MaterialTheme.typography.headlineMedium,
                        color = accentColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (response.deltaScore > 0) "+${response.deltaScore} points" else "No points",
                        style = MaterialTheme.typography.titleMedium,
                        color = HPGold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(visible = visible, enter = fadeIn(tween(500, delayMillis = 350))) {
                NarrativeCard(narrative = response.narrativeFeedback)
            }

            Spacer(Modifier.height(32.dp))

            AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, delayMillis = 600))) {
                Button(
                    onClick = { onProceed(response, playthrough.playthroughId) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HPMaroon)
                ) {
                    Text(
                        text = if (response.nextScene != null) "Continue →" else "See Your Fate",
                        style = MaterialTheme.typography.titleMedium,
                        color = HPParchment
                    )
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SceneErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("⚠", fontSize = 48.sp, color = HPWrong)
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = HPParchment.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = HPMaroon),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Try Again", color = HPParchment, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
fun SceneProgressBar(currentScene: Int, totalScenes: Int, score: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1C1C2E))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚡", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$score pts",
            style = MaterialTheme.typography.labelLarge,
            color = HPGold
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(totalScenes) { i ->
                val filled = i < currentScene
                val isCurrent = i == currentScene - 1
                val dotSize by animateDpAsState(
                    targetValue = if (isCurrent) 10.dp else 7.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "dot$i"
                )
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> HPGold
                                filled -> HPGold.copy(alpha = 0.5f)
                                else -> HPGold.copy(alpha = 0.15f)
                            }
                        )
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$currentScene/$totalScenes",
            style = MaterialTheme.typography.labelSmall,
            color = HPParchment.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun NarrativeCard(narrative: String, isTransparent: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isTransparent) Color.Black.copy(alpha = 0.35f) else Color(0xFF1C1C2E),
        border = BorderStroke(1.dp, HPGold.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = narrative,
            style = MaterialTheme.typography.bodyLarge,
            color = HPParchment,
            modifier = Modifier.padding(20.dp),
            lineHeight = 24.sp
        )
    }
}

@Composable
fun SpellButton(label: String, text: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    OutlinedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, HPGold.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF1C1C2E).copy(alpha = 0.9f),
            contentColor = HPParchment
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = HPGold.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, HPGold),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        color = HPGold,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        fontSize = 18.sp
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
                softWrap = true
            )
        }
    }
}

@Composable
fun ScoreBar(score: Int) = SceneProgressBar(currentScene = 1, totalScenes = TOTAL_SCENES, score = score)
