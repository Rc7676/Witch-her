package com.hexfall.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.hexfall.core.MoonPhase
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * All of Hexfall's art is drawn in code: painted night skies, a moon that
 * tracks the combat's phase, silhouette monsters with glowing eyes, and
 * generative sigils on cards.
 */
object Art {
    val skyTop = Color(0xFF0B0817)
    val skyMid = Color(0xFF1C1433)
    val skyLow = Color(0xFF2E1E4A)
    val horizon = Color(0xFF3A2A5E)
    val moonGlow = Color(0xFFF5EFCF)
    val silhouette = Color(0xFF07050E)
    val mist = Color(0x336C57A8)
}

// --- Sky, moon, spire --------------------------------------------------

fun DrawScope.drawNightSky(starSeed: Int = 7) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Art.skyTop, Art.skyMid, Art.skyLow, Art.horizon),
        ),
    )
    val rng = Random(starSeed)
    repeat(70) {
        val x = rng.nextFloat() * size.width
        val y = rng.nextFloat() * size.height * 0.8f
        val r = 0.6f + rng.nextFloat() * 1.6f
        val alpha = 0.25f + rng.nextFloat() * 0.6f
        drawCircle(Color.White.copy(alpha = alpha), radius = r, center = Offset(x, y))
    }
    // Low mist bands.
    drawOval(
        color = Art.mist,
        topLeft = Offset(-size.width * 0.2f, size.height * 0.82f),
        size = Size(size.width * 1.4f, size.height * 0.3f),
    )
    // Vignette: darkened edges pull focus to the center.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Transparent, Color(0x59000000)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * 0.75f,
        ),
    )
}

fun DrawScope.drawMoon(phase: MoonPhase, center: Offset, radius: Float, glowScale: Float = 1f) {
    val bright = phase != MoonPhase.NEW
    if (bright) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Art.moonGlow.copy(alpha = 0.35f), Color.Transparent),
                center = center,
                radius = radius * 2.6f * glowScale,
            ),
            radius = radius * 2.6f * glowScale,
            center = center,
        )
    }
    when (phase) {
        MoonPhase.FULL -> drawCircle(Art.moonGlow, radius, center)
        MoonPhase.WAXING -> {
            drawCircle(Art.moonGlow, radius, center)
            drawCircle(Art.skyTop, radius, center + Offset(-radius * 0.55f, 0f))
        }
        MoonPhase.WANING -> {
            drawCircle(Art.moonGlow, radius, center)
            drawCircle(Art.skyTop, radius, center + Offset(radius * 0.55f, 0f))
        }
        MoonPhase.NEW -> {
            drawCircle(Art.skyTop, radius, center)
            drawCircle(
                Art.moonGlow.copy(alpha = 0.5f), radius, center,
                style = Stroke(width = radius * 0.08f),
            )
        }
    }
    // Craters on the lit face.
    if (phase == MoonPhase.FULL || phase == MoonPhase.WANING) {
        drawCircle(Color(0x22000000), radius * 0.18f, center + Offset(-radius * 0.25f, -radius * 0.2f))
        drawCircle(Color(0x1A000000), radius * 0.12f, center + Offset(radius * 0.1f, radius * 0.3f))
    }
}

/** A jagged tower silhouette rising from the bottom of the canvas. */
fun DrawScope.drawSpire(alpha: Float = 1f) {
    val w = size.width
    val h = size.height
    val color = Art.silhouette.copy(alpha = alpha)
    val path = Path().apply {
        moveTo(w * 0.62f, h)
        lineTo(w * 0.64f, h * 0.55f)
        lineTo(w * 0.60f, h * 0.53f)
        lineTo(w * 0.68f, h * 0.28f)
        lineTo(w * 0.66f, h * 0.27f)
        lineTo(w * 0.73f, h * 0.10f)
        lineTo(w * 0.78f, h * 0.26f)
        lineTo(w * 0.76f, h * 0.28f)
        lineTo(w * 0.84f, h * 0.52f)
        lineTo(w * 0.80f, h * 0.55f)
        lineTo(w * 0.86f, h)
        close()
    }
    drawPath(path, color)
    // A lit window near the top.
    drawCircle(Color(0xFFE8B84B).copy(alpha = alpha * 0.9f), size.minDimension * 0.012f,
        Offset(w * 0.735f, h * 0.17f))
}

/** The witch: pointed hat, cloak, and a faint staff glow. */
fun DrawScope.drawWitch(tint: Color = Art.silhouette) {
    val w = size.width
    val h = size.height
    val hat = Path().apply {
        moveTo(w * 0.5f, h * 0.02f)
        lineTo(w * 0.66f, h * 0.36f)
        lineTo(w * 0.86f, h * 0.40f)
        lineTo(w * 0.14f, h * 0.40f)
        lineTo(w * 0.34f, h * 0.36f)
        close()
    }
    drawPath(hat, tint)
    drawCircle(tint, w * 0.13f, Offset(w * 0.5f, h * 0.5f))
    val cloak = Path().apply {
        moveTo(w * 0.5f, h * 0.52f)
        lineTo(w * 0.82f, h * 0.98f)
        lineTo(w * 0.18f, h * 0.98f)
        close()
    }
    drawPath(cloak, tint)
    // Staff with an ember at its tip.
    drawLine(tint, Offset(w * 0.86f, h * 0.45f), Offset(w * 0.86f, h * 0.95f),
        strokeWidth = w * 0.04f, cap = StrokeCap.Round)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(HexfallColors.purple.copy(alpha = 0.9f), Color.Transparent),
            center = Offset(w * 0.86f, h * 0.40f), radius = w * 0.16f,
        ),
        radius = w * 0.16f, center = Offset(w * 0.86f, h * 0.40f),
    )
    drawCircle(HexfallColors.purple, w * 0.05f, Offset(w * 0.86f, h * 0.40f))
}

// --- Monsters ----------------------------------------------------------

private fun auraColor(id: String): Color = when (id) {
    "moonfang_wolf" -> Color(0xFF9FB4D8)
    "bog_wisp" -> Color(0xFF63C9A8)
    "plague_rat" -> Color(0xFF8BB84B)
    "moon_moth" -> Color(0xFFC8B7E8)
    "grave_robber" -> Color(0xFFB89A5C)
    "thorn_sprite" -> Color(0xFF7CB05A)
    "hollow_armor" -> Color(0xFF8E9BB0)
    "coven_traitor" -> Color(0xFFA86CC9)
    "barrow_wight" -> Color(0xFF7FD4D0)
    "bone_colossus" -> Color(0xFFD8CBA8)
    "blood_alchemist" -> Color(0xFFD96A6A)
    "hollow_queen" -> Color(0xFFE0B85C)
    else -> Color(0xFF9B8AC0)
}

private fun eyeColor(id: String): Color = when (id) {
    "moonfang_wolf", "grave_robber" -> Color(0xFFF2C14E)
    "plague_rat", "thorn_sprite", "bog_wisp" -> Color(0xFF9FE84B)
    "blood_alchemist", "hollow_queen", "bone_colossus" -> Color(0xFFE84B4B)
    else -> Color(0xFF9FD8E8)
}

/**
 * Draws a stylized silhouette for the enemy [id]: a glowing aura, a dark
 * geometric figure, and bright eyes.
 */
fun DrawScope.drawEnemyFigure(id: String) {
    val w = size.width
    val h = size.height
    val aura = auraColor(id)
    val body = Art.silhouette

    drawCircle(
        brush = Brush.radialGradient(
            listOf(aura.copy(alpha = 0.4f), Color.Transparent),
            center = Offset(w / 2f, h * 0.55f), radius = w * 0.55f,
        ),
        radius = w * 0.55f, center = Offset(w / 2f, h * 0.55f),
    )

    fun eyes(cx: Float, cy: Float, spread: Float, r: Float) {
        drawCircle(eyeColor(id), r, Offset(cx - spread, cy))
        drawCircle(eyeColor(id), r, Offset(cx + spread, cy))
    }

    when (id) {
        "moonfang_wolf" -> {
            val ears = Path().apply {
                moveTo(w * 0.30f, h * 0.38f); lineTo(w * 0.34f, h * 0.12f); lineTo(w * 0.46f, h * 0.32f)
                moveTo(w * 0.70f, h * 0.38f); lineTo(w * 0.66f, h * 0.12f); lineTo(w * 0.54f, h * 0.32f)
                close()
            }
            drawPath(ears, body)
            drawCircle(body, w * 0.24f, Offset(w * 0.5f, h * 0.48f))
            val snout = Path().apply {
                moveTo(w * 0.38f, h * 0.55f); lineTo(w * 0.5f, h * 0.80f); lineTo(w * 0.62f, h * 0.55f); close()
            }
            drawPath(snout, body)
            eyes(w * 0.5f, h * 0.45f, w * 0.10f, w * 0.035f)
        }
        "bog_wisp" -> {
            drawCircle(body, w * 0.26f, Offset(w * 0.5f, h * 0.45f))
            drawCircle(body.copy(alpha = 0.8f), w * 0.13f, Offset(w * 0.30f, h * 0.68f))
            drawCircle(body.copy(alpha = 0.6f), w * 0.08f, Offset(w * 0.68f, h * 0.72f))
            drawCircle(aura.copy(alpha = 0.5f), w * 0.10f, Offset(w * 0.5f, h * 0.45f))
            eyes(w * 0.5f, h * 0.42f, w * 0.09f, w * 0.030f)
        }
        "plague_rat" -> {
            drawOval(body, Offset(w * 0.22f, h * 0.42f), Size(w * 0.52f, h * 0.36f))
            drawCircle(body, w * 0.13f, Offset(w * 0.72f, h * 0.50f))
            drawCircle(body, w * 0.06f, Offset(w * 0.68f, h * 0.36f))
            drawCircle(body, w * 0.06f, Offset(w * 0.80f, h * 0.38f))
            drawLine(body, Offset(w * 0.22f, h * 0.62f), Offset(w * 0.04f, h * 0.42f),
                strokeWidth = w * 0.035f, cap = StrokeCap.Round)
            eyes(w * 0.74f, h * 0.48f, w * 0.045f, w * 0.025f)
        }
        "moon_moth" -> {
            drawOval(body, Offset(w * 0.10f, h * 0.28f), Size(w * 0.34f, h * 0.42f))
            drawOval(body, Offset(w * 0.56f, h * 0.28f), Size(w * 0.34f, h * 0.42f))
            drawOval(aura.copy(alpha = 0.35f), Offset(w * 0.16f, h * 0.34f), Size(w * 0.2f, h * 0.24f))
            drawOval(aura.copy(alpha = 0.35f), Offset(w * 0.64f, h * 0.34f), Size(w * 0.2f, h * 0.24f))
            drawOval(body, Offset(w * 0.44f, h * 0.30f), Size(w * 0.12f, h * 0.46f))
            eyes(w * 0.5f, h * 0.30f, w * 0.045f, w * 0.028f)
        }
        "grave_robber", "coven_traitor" -> {
            val hood = Path().apply {
                moveTo(w * 0.5f, h * 0.10f)
                lineTo(w * 0.78f, h * 0.52f)
                lineTo(w * 0.72f, h * 0.95f)
                lineTo(w * 0.28f, h * 0.95f)
                lineTo(w * 0.22f, h * 0.52f)
                close()
            }
            drawPath(hood, body)
            drawCircle(Color.Black, w * 0.15f, Offset(w * 0.5f, h * 0.40f))
            if (id == "coven_traitor") {
                drawLine(body, Offset(w * 0.12f, h * 0.30f), Offset(w * 0.12f, h * 0.92f),
                    strokeWidth = w * 0.035f, cap = StrokeCap.Round)
                drawCircle(aura, w * 0.05f, Offset(w * 0.12f, h * 0.26f))
            }
            eyes(w * 0.5f, h * 0.40f, w * 0.06f, w * 0.028f)
        }
        "thorn_sprite" -> {
            for (i in 0 until 8) {
                val angle = i * Math.PI.toFloat() / 4f
                val sx = w * 0.5f + cos(angle) * w * 0.30f
                val sy = h * 0.5f + sin(angle) * w * 0.30f
                drawLine(body, Offset(w * 0.5f, h * 0.5f), Offset(sx, sy),
                    strokeWidth = w * 0.045f, cap = StrokeCap.Round)
            }
            drawCircle(body, w * 0.20f, Offset(w * 0.5f, h * 0.5f))
            eyes(w * 0.5f, h * 0.47f, w * 0.075f, w * 0.03f)
        }
        "hollow_armor" -> {
            drawRoundRect(body, Offset(w * 0.34f, h * 0.14f), Size(w * 0.32f, h * 0.28f),
                CornerRadius(w * 0.08f))
            val plume = Path().apply {
                moveTo(w * 0.5f, h * 0.14f); lineTo(w * 0.42f, h * 0.02f); lineTo(w * 0.58f, h * 0.02f); close()
            }
            drawPath(plume, auraColor(id).copy(alpha = 0.8f))
            val torso = Path().apply {
                moveTo(w * 0.30f, h * 0.44f); lineTo(w * 0.70f, h * 0.44f)
                lineTo(w * 0.62f, h * 0.95f); lineTo(w * 0.38f, h * 0.95f); close()
            }
            drawPath(torso, body)
            drawLine(eyeColor(id), Offset(w * 0.40f, h * 0.28f), Offset(w * 0.60f, h * 0.28f),
                strokeWidth = w * 0.028f, cap = StrokeCap.Round)
        }
        "barrow_wight" -> {
            drawCircle(body, w * 0.16f, Offset(w * 0.5f, h * 0.24f))
            val shroud = Path().apply {
                moveTo(w * 0.34f, h * 0.34f)
                lineTo(w * 0.66f, h * 0.34f)
                lineTo(w * 0.74f, h * 0.72f)
                lineTo(w * 0.62f, h * 0.66f)
                lineTo(w * 0.56f, h * 0.95f)
                lineTo(w * 0.46f, h * 0.70f)
                lineTo(w * 0.34f, h * 0.90f)
                lineTo(w * 0.28f, h * 0.62f)
                close()
            }
            drawPath(shroud, body.copy(alpha = 0.92f))
            eyes(w * 0.5f, h * 0.22f, w * 0.055f, w * 0.028f)
        }
        "bone_colossus" -> {
            drawCircle(body, w * 0.11f, Offset(w * 0.5f, h * 0.16f))
            drawRoundRect(body, Offset(w * 0.18f, h * 0.24f), Size(w * 0.64f, h * 0.30f),
                CornerRadius(w * 0.10f))
            drawRoundRect(body, Offset(w * 0.10f, h * 0.28f), Size(w * 0.14f, h * 0.55f),
                CornerRadius(w * 0.06f))
            drawRoundRect(body, Offset(w * 0.76f, h * 0.28f), Size(w * 0.14f, h * 0.55f),
                CornerRadius(w * 0.06f))
            drawRoundRect(body, Offset(w * 0.30f, h * 0.52f), Size(w * 0.40f, h * 0.40f),
                CornerRadius(w * 0.08f))
            eyes(w * 0.5f, h * 0.15f, w * 0.045f, w * 0.026f)
        }
        "blood_alchemist" -> {
            val robe = Path().apply {
                moveTo(w * 0.5f, h * 0.10f)
                lineTo(w * 0.72f, h * 0.50f)
                lineTo(w * 0.66f, h * 0.95f)
                lineTo(w * 0.34f, h * 0.95f)
                lineTo(w * 0.28f, h * 0.50f)
                close()
            }
            drawPath(robe, body)
            drawCircle(Color.Black, w * 0.13f, Offset(w * 0.5f, h * 0.34f))
            drawCircle(auraColor(id).copy(alpha = 0.9f), w * 0.09f, Offset(w * 0.82f, h * 0.62f))
            drawLine(body, Offset(w * 0.82f, h * 0.50f), Offset(w * 0.82f, h * 0.56f),
                strokeWidth = w * 0.05f)
            eyes(w * 0.5f, h * 0.34f, w * 0.05f, w * 0.026f)
        }
        "hollow_queen" -> {
            val crown = Path().apply {
                moveTo(w * 0.30f, h * 0.22f)
                lineTo(w * 0.34f, h * 0.06f); lineTo(w * 0.42f, h * 0.18f)
                lineTo(w * 0.50f, h * 0.02f); lineTo(w * 0.58f, h * 0.18f)
                lineTo(w * 0.66f, h * 0.06f); lineTo(w * 0.70f, h * 0.22f)
                close()
            }
            drawPath(crown, auraColor(id))
            drawCircle(body, w * 0.14f, Offset(w * 0.5f, h * 0.34f))
            val gown = Path().apply {
                moveTo(w * 0.5f, h * 0.44f)
                lineTo(w * 0.86f, h * 0.96f)
                lineTo(w * 0.14f, h * 0.96f)
                close()
            }
            drawPath(gown, body)
            eyes(w * 0.5f, h * 0.33f, w * 0.055f, w * 0.028f)
        }
        else -> {
            drawCircle(body, w * 0.26f, Offset(w * 0.5f, h * 0.5f))
            eyes(w * 0.5f, h * 0.46f, w * 0.09f, w * 0.03f)
        }
    }
}

@Composable
fun EnemyFigure(id: String, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawEnemyFigure(id) }
}

// --- Card sigils --------------------------------------------------------

/**
 * A generative arcane emblem, deterministic per card id: star, crescent,
 * rune strokes, or orbits, tinted by card type.
 */
fun DrawScope.drawSigil(seed: Int, tint: Color) {
    val rng = Random(seed)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension * 0.38f

    drawCircle(
        brush = Brush.radialGradient(
            listOf(tint.copy(alpha = 0.35f), Color.Transparent),
            center = Offset(cx, cy), radius = r * 1.9f,
        ),
        radius = r * 1.9f, center = Offset(cx, cy),
    )

    when (rng.nextInt(4)) {
        0 -> { // Star polygon.
            val points = 5 + rng.nextInt(3)
            val inner = r * 0.42f
            val path = Path()
            for (i in 0 until points * 2) {
                val angle = (i * Math.PI / points - Math.PI / 2).toFloat()
                val rad = if (i % 2 == 0) r else inner
                val x = cx + cos(angle) * rad
                val y = cy + sin(angle) * rad
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, tint)
            drawCircle(Color.White.copy(alpha = 0.85f), r * 0.10f, Offset(cx, cy))
        }
        1 -> { // Crescent.
            drawCircle(tint, r, Offset(cx, cy))
            drawCircle(Color(0xFF141020), r * 0.92f, Offset(cx + r * 0.45f, cy - r * 0.18f))
            drawCircle(Color.White.copy(alpha = 0.8f), r * 0.07f, Offset(cx - r * 0.5f, cy + r * 0.3f))
        }
        2 -> { // Rune strokes.
            val strokes = 3 + rng.nextInt(3)
            repeat(strokes) {
                val x1 = cx + (rng.nextFloat() - 0.5f) * r * 2f
                val y1 = cy + (rng.nextFloat() - 0.5f) * r * 2f
                val x2 = cx + (rng.nextFloat() - 0.5f) * r * 2f
                val y2 = cy + (rng.nextFloat() - 0.5f) * r * 2f
                drawLine(tint, Offset(x1, y1), Offset(x2, y2),
                    strokeWidth = r * 0.14f, cap = StrokeCap.Round)
            }
            drawCircle(tint, r * 0.14f, Offset(cx, cy))
        }
        else -> { // Orbits.
            drawCircle(tint, r, Offset(cx, cy), style = Stroke(width = r * 0.10f))
            drawCircle(tint.copy(alpha = 0.6f), r * 0.6f, Offset(cx, cy),
                style = Stroke(width = r * 0.08f))
            val angle = rng.nextFloat() * 6.28f
            drawCircle(Color.White.copy(alpha = 0.9f), r * 0.12f,
                Offset(cx + cos(angle) * r, cy + sin(angle) * r))
            drawCircle(tint, r * 0.16f, Offset(cx, cy))
        }
    }
}

@Composable
fun SigilArt(seed: Int, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFF141020))
        drawSigil(seed, tint)
    }
}

// --- Scene scaffold ------------------------------------------------------

/**
 * Full-screen night backdrop; most screens sit on top of this. The sky
 * paints edge-to-edge behind the system bars while [content] is inset
 * inside the safe-drawing area so nav buttons never overlap the UI.
 */
@Composable
fun NightScene(
    moon: MoonPhase? = null,
    spire: Boolean = false,
    starSeed: Int = 7,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawNightSky(starSeed)
            if (moon != null) {
                drawMoon(moon, Offset(size.width * 0.82f, size.height * 0.10f),
                    size.minDimension * 0.075f)
            }
            if (spire) drawSpire(alpha = 0.85f)
        }
        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            content()
        }
    }
}

/** Small inline moon icon used in headers. */
@Composable
fun MoonIcon(phase: MoonPhase, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawMoon(phase, Offset(size.width / 2f, size.height / 2f), size.minDimension * 0.42f)
    }
}

/** Dashed, softly glowing path segment used on the map. */
fun DrawScope.drawMapEdge(from: Offset, to: Offset, highlight: Boolean) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
    drawLine(
        color = if (highlight) HexfallColors.gold.copy(alpha = 0.8f) else Color(0x44EADFC8),
        start = from, end = to,
        strokeWidth = if (highlight) 4f else 3f,
        pathEffect = dash,
        cap = StrokeCap.Round,
    )
}
