package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StonicCyan
import com.example.ui.theme.StonicHotPinkGlow
import com.example.ui.theme.StonicNeonPink
import com.example.ui.theme.StonicPurple
import com.example.voice.AssistantState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StonicAiOrb(
    state: AssistantState,
    soundLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbInfiniteLoop")

    // Idle breathing pulse
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreathing"
    )

    // Continuous rotation for outer tech ring
    val slowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SlowRotation"
    )

    // Fast rotation for thinking state
    val fastRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FastRotation"
    )

    // Glow pulse intensity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    // Speaking rhythm wave oscillation
    val speakingWave by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SpeakingWave"
    )

    val dynamicSoundAnim = remember { Animatable(0f) }
    LaunchedEffect(soundLevel) {
        dynamicSoundAnim.animateTo(
            targetValue = soundLevel,
            animationSpec = tween(80, easing = LinearEasing)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("stonic_ai_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension * 0.32f

            val effectiveScale = when (state) {
                AssistantState.IDLE -> breathingScale
                AssistantState.LISTENING -> 1.0f + (dynamicSoundAnim.value * 0.28f)
                AssistantState.THINKING -> 1.0f + (glowAlpha * 0.10f)
                AssistantState.SPEAKING -> speakingWave
                AssistantState.ERROR -> 1.0f + (glowAlpha * 0.05f)
            }

            val currentRadius = baseRadius * effectiveScale

            // 1. Outer ambient neon glow aura
            val outerGlowColor = when (state) {
                AssistantState.IDLE -> StonicNeonPink.copy(alpha = 0.22f * glowAlpha)
                AssistantState.LISTENING -> StonicCyan.copy(alpha = 0.35f + (dynamicSoundAnim.value * 0.3f))
                AssistantState.THINKING -> StonicPurple.copy(alpha = 0.40f * glowAlpha)
                AssistantState.SPEAKING -> StonicHotPinkGlow.copy(alpha = 0.45f)
                AssistantState.ERROR -> Color(0xFFFF3366).copy(alpha = 0.50f * glowAlpha)
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        outerGlowColor,
                        outerGlowColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.85f
                ),
                radius = currentRadius * 1.85f,
                center = center
            )

            // 2. Holographic Ring 1 (Outer cybernetic arc ring)
            val rotAngle1 = if (state == AssistantState.THINKING) fastRotation else slowRotation
            rotate(degrees = rotAngle1, pivot = center) {
                val ring1Radius = currentRadius * 1.35f
                // Arc segments
                drawArc(
                    color = StonicNeonPink.copy(alpha = 0.75f),
                    startAngle = 0f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - ring1Radius, center.y - ring1Radius),
                    size = androidx.compose.ui.geometry.Size(ring1Radius * 2, ring1Radius * 2),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = StonicCyan.copy(alpha = 0.65f),
                    startAngle = 120f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - ring1Radius, center.y - ring1Radius),
                    size = androidx.compose.ui.geometry.Size(ring1Radius * 2, ring1Radius * 2),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = StonicNeonPink.copy(alpha = 0.75f),
                    startAngle = 230f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(center.x - ring1Radius, center.y - ring1Radius),
                    size = androidx.compose.ui.geometry.Size(ring1Radius * 2, ring1Radius * 2),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Satellite dot nodes
                for (angle in listOf(40f, 160f, 280f)) {
                    val rad = Math.toRadians(angle.toDouble())
                    val dotX = center.x + ring1Radius * cos(rad).toFloat()
                    val dotY = center.y + ring1Radius * sin(rad).toFloat()
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }
            }

            // 3. Counter-rotating inner ring
            val rotAngle2 = if (state == AssistantState.THINKING) -fastRotation * 1.4f else -slowRotation * 0.7f
            rotate(degrees = rotAngle2, pivot = center) {
                val ring2Radius = currentRadius * 1.15f
                drawArc(
                    color = StonicPurple.copy(alpha = 0.60f),
                    startAngle = 45f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(center.x - ring2Radius, center.y - ring2Radius),
                    size = androidx.compose.ui.geometry.Size(ring2Radius * 2, ring2Radius * 2),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = StonicNeonPink.copy(alpha = 0.80f),
                    startAngle = 210f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(center.x - ring2Radius, center.y - ring2Radius),
                    size = androidx.compose.ui.geometry.Size(ring2Radius * 2, ring2Radius * 2),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Reactive Waveform Spikes (during Listening or Speaking)
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                val spikeCount = 28
                val stepAngle = (2 * PI / spikeCount).toFloat()
                for (i in 0 until spikeCount) {
                    val angle = i * stepAngle
                    val waveVariance = if (state == AssistantState.LISTENING) {
                        sin((angle * 4 + rotAngle1).toDouble()).toFloat() * dynamicSoundAnim.value * 28.dp.toPx()
                    } else {
                        cos((angle * 5 + rotAngle2).toDouble()).toFloat() * (speakingWave - 0.9f) * 40.dp.toPx()
                    }

                    val startR = currentRadius * 1.05f
                    val endR = startR + 8.dp.toPx() + waveVariance.coerceAtLeast(0f)

                    val startX = center.x + startR * cos(angle)
                    val startY = center.y + startR * sin(angle)
                    val endX = center.x + endR * cos(angle)
                    val endY = center.y + endR * sin(angle)

                    val spikeColor = if (i % 2 == 0) StonicNeonPink else StonicCyan
                    drawLine(
                        color = spikeColor.copy(alpha = 0.85f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 5. High-Energy Spherical Core with Radial Gradient
            val coreColors = when (state) {
                AssistantState.IDLE -> listOf(
                    Color.White.copy(alpha = 0.9f),
                    StonicNeonPink,
                    StonicHotPinkGlow,
                    Color(0xFF380829),
                    Color(0xFF0F0514)
                )
                AssistantState.LISTENING -> listOf(
                    Color.White,
                    StonicCyan,
                    StonicNeonPink,
                    Color(0xFF003844),
                    Color(0xFF09090D)
                )
                AssistantState.THINKING -> listOf(
                    Color.White,
                    StonicPurple,
                    StonicNeonPink,
                    Color(0xFF280B3B),
                    Color(0xFF09090D)
                )
                AssistantState.SPEAKING -> listOf(
                    Color.White,
                    StonicHotPinkGlow,
                    StonicNeonPink,
                    Color(0xFF4A0E32),
                    Color(0xFF09090D)
                )
                AssistantState.ERROR -> listOf(
                    Color.White,
                    Color(0xFFFF3366),
                    Color(0xFFFF0055),
                    Color(0xFF4A0014),
                    Color(0xFF09090D)
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = coreColors,
                    center = Offset(center.x - currentRadius * 0.25f, center.y - currentRadius * 0.25f),
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = center
            )

            // 6. Highlight specular reflection crest
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.70f),
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - currentRadius * 0.35f, center.y - currentRadius * 0.35f),
                    radius = currentRadius * 0.45f
                ),
                radius = currentRadius * 0.45f,
                center = Offset(center.x - currentRadius * 0.35f, center.y - currentRadius * 0.35f)
            )

            // 7. Neon Core Border
            drawCircle(
                color = StonicNeonPink.copy(alpha = 0.85f),
                radius = currentRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
