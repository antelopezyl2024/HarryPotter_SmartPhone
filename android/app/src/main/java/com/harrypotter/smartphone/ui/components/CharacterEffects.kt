package com.harrypotter.smartphone.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*

// ── Hogwarts castle silhouette ────────────────────────────────────────────────
// Dim gold castle outline for the bottom of the HomeScreen background.
@Composable
fun HogwartsSilhouette(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "hogwarts")
    val glow by transition.animateFloat(
        initialValue = 0.05f, targetValue = 0.13f,
        animationSpec = infiniteRepeatable(tween(3500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "castleGlow"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val c = Color(0xFFC9A227).copy(alpha = glow)
        drawCastle(w, h, c)
    }
}

private fun DrawScope.drawCastle(w: Float, h: Float, color: Color) {
    // Ground fill
    drawRect(color, topLeft = Offset(0f, h * 0.88f), size = Size(w, h * 0.12f))

    // Far-left small tower + battlements
    drawRect(color, topLeft = Offset(w * 0.04f, h * 0.68f), size = Size(w * 0.07f, h * 0.20f))
    drawBattlements(w * 0.04f, h * 0.68f, w * 0.07f, 3, color)

    // Left tower with pointed spire
    drawRect(color, topLeft = Offset(w * 0.14f, h * 0.50f), size = Size(w * 0.09f, h * 0.38f))
    drawPath(Path().apply {
        moveTo(w * 0.14f, h * 0.50f)
        lineTo(w * 0.185f, h * 0.26f)
        lineTo(w * 0.23f, h * 0.50f)
        close()
    }, color)

    // Connector wall
    drawRect(color, topLeft = Offset(w * 0.23f, h * 0.70f), size = Size(w * 0.07f, h * 0.18f))

    // Main central block + battlements
    drawRect(color, topLeft = Offset(w * 0.30f, h * 0.58f), size = Size(w * 0.40f, h * 0.30f))
    drawBattlements(w * 0.30f, h * 0.58f, w * 0.40f, 9, color)

    // Tallest central tower + great spire
    drawRect(color, topLeft = Offset(w * 0.42f, h * 0.30f), size = Size(w * 0.16f, h * 0.58f))
    drawPath(Path().apply {
        moveTo(w * 0.42f, h * 0.30f)
        lineTo(w * 0.50f, h * 0.05f)
        lineTo(w * 0.58f, h * 0.30f)
        close()
    }, color)

    // Right connector wall
    drawRect(color, topLeft = Offset(w * 0.70f, h * 0.72f), size = Size(w * 0.06f, h * 0.16f))

    // Right mid tower with spire
    drawRect(color, topLeft = Offset(w * 0.76f, h * 0.52f), size = Size(w * 0.09f, h * 0.36f))
    drawPath(Path().apply {
        moveTo(w * 0.76f, h * 0.52f)
        lineTo(w * 0.805f, h * 0.28f)
        lineTo(w * 0.85f, h * 0.52f)
        close()
    }, color)

    // Far-right small tower + battlements
    drawRect(color, topLeft = Offset(w * 0.87f, h * 0.66f), size = Size(w * 0.07f, h * 0.22f))
    drawBattlements(w * 0.87f, h * 0.66f, w * 0.07f, 3, color)
}

private fun DrawScope.drawBattlements(x: Float, topY: Float, w: Float, count: Int, color: Color) {
    val slot = w / count
    val mw = slot * 0.55f
    val mh = slot * 0.75f
    repeat(count) { i ->
        val bx = x + i * slot + (slot - mw) / 2f
        drawRect(color, topLeft = Offset(bx, topY - mh), size = Size(mw, mh))
    }
}

// ── Floating wizard silhouette ─────────────────────────────────────────────────
// Faint robed wizard with glowing wand tip — hovers as background atmosphere.
@Composable
fun FloatingWizardSilhouette(modifier: Modifier = Modifier, alpha: Float = 0.10f) {
    val transition = rememberInfiniteTransition(label = "wizard")
    val bobY by transition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "wizBob"
    )
    val sway by transition.animateFloat(
        initialValue = -2.5f, targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(3400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "wizSway"
    )
    val wandGlow by transition.animateFloat(
        initialValue = 0.35f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "wandGlow"
    )

    Canvas(modifier = modifier) {
        val cx = size.width * 0.84f
        val cy = size.height * 0.34f + bobY
        val sc = size.minDimension / 520f
        withTransform({ translate(cx, cy); rotate(sway) }) {
            drawWizard(sc, alpha, wandGlow)
        }
    }
}

private fun DrawScope.drawWizard(scale: Float, alpha: Float, wandGlow: Float) {
    val gold = Color(0xFFC9A227).copy(alpha = alpha)
    val tip = Color.White.copy(alpha = alpha * wandGlow * 0.9f)

    // Pointed hat
    drawPath(Path().apply {
        moveTo(0f, -88f * scale); lineTo(-24f * scale, -44f * scale); lineTo(24f * scale, -44f * scale); close()
    }, gold)
    drawRect(gold, topLeft = Offset(-36f * scale, -44f * scale), size = Size(72f * scale, 10f * scale))

    // Head
    drawCircle(gold, radius = 18f * scale, center = Offset(0f, -22f * scale))
    // Glasses (Harry's round frames)
    drawCircle(
        color = gold.copy(alpha = alpha * 0.6f),
        radius = 6f * scale,
        center = Offset(-8f * scale, -22f * scale),
        style = Stroke(width = 1.5f * scale)
    )
    drawCircle(
        color = gold.copy(alpha = alpha * 0.6f),
        radius = 6f * scale,
        center = Offset(8f * scale, -22f * scale),
        style = Stroke(width = 1.5f * scale)
    )
    // Bridge
    drawLine(
        gold.copy(alpha = alpha * 0.6f),
        Offset(-2f * scale, -22f * scale),
        Offset(2f * scale, -22f * scale),
        strokeWidth = 1.5f * scale
    )

    // Cloak body
    drawPath(Path().apply {
        moveTo(-12f * scale, -6f * scale)
        lineTo(-46f * scale, 78f * scale)
        lineTo(0f, 86f * scale)
        lineTo(46f * scale, 78f * scale)
        lineTo(12f * scale, -6f * scale)
        close()
    }, gold)

    // Raised arm
    drawPath(Path().apply {
        moveTo(-12f * scale, 8f * scale); lineTo(-44f * scale, -14f * scale)
    }, gold, style = Stroke(width = 5f * scale, cap = StrokeCap.Round))

    // Wand
    drawPath(Path().apply {
        moveTo(-44f * scale, -14f * scale); lineTo(-58f * scale, -28f * scale)
    }, gold, style = Stroke(width = 3f * scale, cap = StrokeCap.Round))

    // Wand tip glow
    val tx = -58f * scale; val ty = -28f * scale
    drawCircle(tip.copy(alpha = tip.alpha * 0.25f), radius = 16f * scale, center = Offset(tx, ty))
    drawCircle(tip, radius = 5f * scale, center = Offset(tx, ty))
}

// ── Dementor effect ────────────────────────────────────────────────────────────
// Dark hooded figure that fades in — wrong-answer feedback and FAIL ending.
@Composable
fun DementorEffect(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    alpha: Float = 0.22f
) {
    val fade by animateFloatAsState(
        targetValue = if (visible) alpha else 0f,
        animationSpec = tween(1400, easing = EaseInOutSine),
        label = "dementorFade"
    )
    val transition = rememberInfiniteTransition(label = "dementor")
    val sway by transition.animateFloat(
        initialValue = -7f, targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "dSway"
    )
    val wispPulse by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "wispPulse"
    )

    if (fade == 0f) return

    Canvas(modifier = modifier) {
        val cx = size.width * 0.11f + sway
        val cy = size.height * 0.28f
        val sc = size.minDimension / 580f
        translate(cx, cy) { drawDementor(sc, fade, wispPulse) }
    }
}

private fun DrawScope.drawDementor(scale: Float, alpha: Float, wispPulse: Float) {
    val body = Color(0xFF3A0055).copy(alpha = alpha)
    val void_ = Color(0xFF08000F).copy(alpha = alpha * 1.4f)
    val wisp = Color(0xFF7700CC).copy(alpha = alpha * wispPulse)

    // Cowl shape
    drawPath(Path().apply {
        moveTo(0f, -115f * scale)
        cubicTo(-58f * scale, -115f * scale, -78f * scale, -42f * scale, -68f * scale, 28f * scale)
        cubicTo(-68f * scale, 72f * scale, -36f * scale, 94f * scale, 0f, 98f * scale)
        cubicTo(36f * scale, 94f * scale, 68f * scale, 72f * scale, 68f * scale, 28f * scale)
        cubicTo(78f * scale, -42f * scale, 58f * scale, -115f * scale, 0f, -115f * scale)
        close()
    }, body)

    // Dark hollow face
    drawPath(Path().apply {
        moveTo(0f, -68f * scale)
        cubicTo(-30f * scale, -68f * scale, -40f * scale, -22f * scale, -35f * scale, 14f * scale)
        cubicTo(-35f * scale, 40f * scale, -18f * scale, 52f * scale, 0f, 52f * scale)
        cubicTo(18f * scale, 52f * scale, 35f * scale, 40f * scale, 35f * scale, 14f * scale)
        cubicTo(40f * scale, -22f * scale, 30f * scale, -68f * scale, 0f, -68f * scale)
        close()
    }, void_)

    // Tattered flowing cloak
    drawPath(Path().apply {
        moveTo(-68f * scale, 28f * scale)
        lineTo(-98f * scale, 230f * scale)
        lineTo(-74f * scale, 208f * scale)
        lineTo(-62f * scale, 238f * scale)
        lineTo(-38f * scale, 210f * scale)
        lineTo(-18f * scale, 240f * scale)
        lineTo(0f, 214f * scale)
        lineTo(18f * scale, 240f * scale)
        lineTo(38f * scale, 210f * scale)
        lineTo(62f * scale, 238f * scale)
        lineTo(74f * scale, 208f * scale)
        lineTo(98f * scale, 230f * scale)
        lineTo(68f * scale, 28f * scale)
        close()
    }, body)

    // Wispy smoke beneath
    repeat(5) { i ->
        drawCircle(
            wisp.copy(alpha = wisp.alpha * (1f - i * 0.18f)),
            radius = (16f - i * 2.5f) * scale,
            center = Offset((-38f + i * 19f) * scale, (244f + i * 20f) * scale)
        )
    }
}

// ── Golden Snitch (Canvas) ─────────────────────────────────────────────────────
// Bobbing winged orb with flapping wings — used on HomeScreen and success ending.
@Composable
fun GoldenSnitchCanvas(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "snitch")
    val bobY by transition.animateFloat(
        initialValue = -18f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(1100, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "sY"
    )
    val driftX by transition.animateFloat(
        initialValue = -22f, targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(1750, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "sX"
    )
    val wingFlap by transition.animateFloat(
        initialValue = 0.22f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(180, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "flap"
    )
    val glow by transition.animateFloat(
        initialValue = 0.5f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(550, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "sGlow"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2 + driftX
        val cy = size.height / 2 + bobY
        val r = size.minDimension * 0.17f
        val gold = Color(0xFFC9A227)

        // Outer glow
        drawCircle(gold.copy(alpha = 0.07f * glow), radius = r * 3.4f, center = Offset(cx, cy))
        drawCircle(gold.copy(alpha = 0.16f * glow), radius = r * 2.1f, center = Offset(cx, cy))

        // Left wing
        drawPath(Path().apply {
            moveTo(cx - r, cy)
            cubicTo(cx - r * 2.3f, cy - r * 2.3f * wingFlap, cx - r * 4.0f, cy - r * 1.7f * wingFlap, cx - r * 4.4f, cy)
            cubicTo(cx - r * 4.0f, cy + r * 0.6f * wingFlap, cx - r * 2.3f, cy + r * 0.6f * wingFlap, cx - r, cy)
            close()
        }, gold.copy(alpha = 0.58f))

        // Right wing
        drawPath(Path().apply {
            moveTo(cx + r, cy)
            cubicTo(cx + r * 2.3f, cy - r * 2.3f * wingFlap, cx + r * 4.0f, cy - r * 1.7f * wingFlap, cx + r * 4.4f, cy)
            cubicTo(cx + r * 4.0f, cy + r * 0.6f * wingFlap, cx + r * 2.3f, cy + r * 0.6f * wingFlap, cx + r, cy)
            close()
        }, gold.copy(alpha = 0.58f))

        // Body
        drawCircle(gold, radius = r, center = Offset(cx, cy))
        // Specular highlights
        drawCircle(Color.White.copy(alpha = 0.42f), radius = r * 0.34f, center = Offset(cx - r * 0.29f, cy - r * 0.29f))
        drawCircle(Color.White.copy(alpha = 0.12f), radius = r * 0.55f, center = Offset(cx - r * 0.14f, cy - r * 0.14f))
    }
}

// ── Lottie wrappers ────────────────────────────────────────────────────────────

@Composable
fun LottieSnitch(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("snitch.json"))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(composition, { progress }, modifier = modifier)
}

@Composable
fun LottiePatronus(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("patronus.json"))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(composition, { progress }, modifier = modifier)
}
