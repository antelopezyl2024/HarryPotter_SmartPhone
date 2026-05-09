package com.harrypotter.smartphone.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harrypotter.smartphone.ui.theme.*
import com.harrypotter.smartphone.viewmodel.HermioneMessage
import com.harrypotter.smartphone.viewmodel.HermioneViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HermioneScreen(
    viewModel: HermioneViewModel,
    playthroughId: String?,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(state.messages.size - 1) }
        }
    }

    state.error?.let { error ->
        LaunchedEffect(error) {
            // error is shown inline; clear after displaying
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hermione's Notes", color = HPGold, style = MaterialTheme.typography.titleMedium)
                        Text("Ask anything about the Wizarding World", style = MaterialTheme.typography.labelSmall, color = HPParchment.copy(alpha = 0.6f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = HPGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D1A))
            )
        },
        containerColor = HPBackground,
        bottomBar = {
            HermioneInputBar(
                value = input,
                onValueChange = { input = it },
                isLoading = state.isLoading,
                onSend = {
                    val q = input.trim()
                    if (q.isNotEmpty()) {
                        input = ""
                        viewModel.sendQuery(q, playthroughId)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(HPBackground, Color(0xFF141428))))
                .padding(padding)
        ) {
            if (state.messages.isEmpty() && !state.isLoading) {
                WelcomeHint()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(state.messages) { msg ->
                        MessageBubble(msg)
                    }
                    if (state.isLoading) {
                        item { TypingIndicator() }
                    }
                    state.error?.let { err ->
                        item {
                            Text(
                                text = "Error: $err",
                                color = HPWrong,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: HermioneMessage) {
    val alignment = if (msg.isUser) Alignment.End else Alignment.Start
    val bgColor = if (msg.isUser) HPMaroon.copy(alpha = 0.8f) else Color(0xFF1C1C2E)
    val textColor = HPParchment
    val shape = if (msg.isUser) {
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!msg.isUser) {
            Text(
                text = "✨ Hermione",
                style = MaterialTheme.typography.labelSmall,
                color = HPGold,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Surface(
            shape = shape,
            color = bgColor,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = msg.text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(12.dp)
            )
        }
        if (!msg.isUser && msg.sources.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            msg.sources.forEach { src ->
                Text(
                    text = "📖 ${src.title}",
                    style = MaterialTheme.typography.labelSmall,
                    color = HPVeilBlue.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier
            .background(Color(0xFF1C1C2E), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { i ->
            val infiniteTransition = rememberInfiniteTransition(label = "dot$i")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = i * 200),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "dotAlpha$i"
            )
            Text("•", color = HPParchment.copy(alpha = alpha), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun HermioneInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit
) {
    Surface(
        color = Color(0xFF0D0D1A),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 300) onValueChange(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Hermione…", color = HPParchment.copy(alpha = 0.4f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HPGold,
                    unfocusedBorderColor = HPGold.copy(alpha = 0.3f),
                    focusedTextColor = HPParchment,
                    unfocusedTextColor = HPParchment
                ),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isLoading
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (value.isNotBlank() && !isLoading) HPGold else HPGold.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun WelcomeHint() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("✨", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Ask Hermione anything about the Wizarding World",
                style = MaterialTheme.typography.bodyLarge,
                color = HPParchment.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "She won't reveal the right answer, but she'll guide you.",
                style = MaterialTheme.typography.bodySmall,
                color = HPParchment.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
