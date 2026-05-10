package com.harrypotter.smartphone.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.*

// Floating golden particles drifting upward — used as background overlay
@Composable
fun FloatingParticles(
    modifier: Modifier = Modifier,
    count: Int = 18,
    color: Color = Color(0xFFC9A227)
) {
    val particles = remember {
        List(count) {
            ParticleState(
                startX = (10..90).random() / 100f,
                startY = (0..120).random() / 100f,
                size = (2..5).random().toFloat(),
                speed = (3..8).random() / 10f,
                phase = (0..1000).random() / 1000f,
                wobble = (3..8).random() / 10f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "particleProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val t = ((progress + p.phase) % 1f)
            val y = size.height * (1f - t * p.speed * 2f)
            if (y < 0 || y > size.height) return@forEach
            val x = size.width * p.startX + sin(t * 2 * PI.toFloat() * p.wobble) * 30f
            val alpha = when {
                t < 0.15f -> t / 0.15f
                t > 0.75f -> 1f - (t - 0.75f) / 0.25f
                else -> 1f
            } * 0.5f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = p.size,
                center = Offset(x, y)
            )
        }
    }
}

// Star/sparkle burst — shown on correct feedback
@Composable
fun SparkleBurst(
    modifier: Modifier = Modifier,
    trigger: Boolean,
    color: Color = Color(0xFFC9A227)
) {
    val progress by animateFloatAsState(
        targetValue = if (trigger) 1f else 0f,
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "sparkle"
    )

    Canvas(modifier = modifier) {
        if (progress == 0f) return@Canvas
        val cx = size.width / 2
        val cy = size.height / 2
        val rays = 12
        repeat(rays) { i ->
            val angle = (i * 360f / rays) + progress * 30f
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val len = size.minDimension * 0.5f * progress
            val alpha = (1f - progress) * 0.8f
            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(cx + cos(rad) * len * 0.2f, cy + sin(rad) * len * 0.2f),
                end = Offset(cx + cos(rad) * len, cy + sin(rad) * len),
                strokeWidth = 3f * (1f - progress * 0.5f)
            )
            // small dot at end of each ray
            drawCircle(
                color = color.copy(alpha = alpha * 0.7f),
                radius = 4f * (1f - progress),
                center = Offset(cx + cos(rad) * len, cy + sin(rad) * len)
            )
        }
    }
}

// Wand-trace shimmer drawn across a horizontal line — used under scene title
@Composable
fun WandTrace(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFC9A227)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wand")
    val sweep by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = EaseInOutSine),
            RepeatMode.Restart
        ),
        label = "wandSweep"
    )

    Canvas(modifier = modifier) {
        val y = size.height / 2
        // base dim line
        drawLine(
            color = color.copy(alpha = 0.15f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        // bright moving head
        val cx = size.width * sweep
        for (i in 1..40) {
            val dx = i * size.width * 0.012f
            val alpha = (1f - i / 40f) * 0.7f
            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset((cx - dx).coerceIn(0f, size.width), y),
                end = Offset(cx.coerceIn(0f, size.width), y),
                strokeWidth = 2.5f
            )
        }
        // bright tip
        if (cx in 0f..size.width) {
            drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 3f, center = Offset(cx, y))
        }
    }
}

// Raining golden stars — SUCCESS ending background
@Composable
fun StarRain(modifier: Modifier = Modifier) {
    val stars = remember {
        List(30) {
            ParticleState(
                startX = (0..100).random() / 100f,
                startY = -(0..50).random() / 100f,
                size = (2..4).random().toFloat(),
                speed = (4..9).random() / 10f,
                phase = (0..1000).random() / 1000f,
                wobble = (1..3).random() / 10f
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "starRain")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "starT"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        stars.forEach { s ->
            val progress = ((t + s.phase) % 1f)
            val y = size.height * progress * s.speed * 2.5f
            if (y > size.height) return@forEach
            val x = size.width * s.startX + sin(progress * 6.28f * s.wobble) * 20f
            val alpha = when {
                progress < 0.1f -> progress / 0.1f
                progress > 0.7f -> 1f - (progress - 0.7f) / 0.3f
                else -> 1f
            } * 0.6f
            drawStar(
                center = Offset(x, y),
                radius = s.size,
                color = Color(0xFFC9A227).copy(alpha = alpha)
            )
        }
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val points = 5
    val innerRadius = radius * 0.4f
    val path = androidx.compose.ui.graphics.Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = Math.toRadians((i * 180.0 / points) - 90.0).toFloat()
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

private data class ParticleState(
    val startX: Float,
    val startY: Float,
    val size: Float,
    val speed: Float,
    val phase: Float,
    val wobble: Float
)
