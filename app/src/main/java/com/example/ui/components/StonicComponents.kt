package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StonicBackground
import com.example.ui.theme.StonicBorderPink
import com.example.ui.theme.StonicBorderSubtle
import com.example.ui.theme.StonicCyan
import com.example.ui.theme.StonicNeonPink
import com.example.ui.theme.StonicPurple
import com.example.ui.theme.StonicSurfaceGlass
import com.example.ui.theme.StonicTextPrimary
import com.example.voice.AssistantState

@Composable
fun StonicGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = StonicBorderPink,
    backgroundColor: Color = StonicSurfaceGlass,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Box(
        modifier = clickableModifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.6f),
                        borderColor.copy(alpha = 0.15f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun StonicStatusChip(
    state: AssistantState,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "StatusPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val (label, statusColor) = when (state) {
        AssistantState.IDLE -> Pair("STONIC ONLINE • READY", StonicNeonPink)
        AssistantState.LISTENING -> Pair("LISTENING TO VOICE...", StonicCyan)
        AssistantState.THINKING -> Pair("ANALYZING WITH GEMINI...", StonicPurple)
        AssistantState.SPEAKING -> Pair("STONIC SPEAKING...", StonicNeonPink)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(30.dp)),
        color = Color(0xCC0E0E18)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = pulseAlpha))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = StonicTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
    }
}

@Composable
fun StonicNeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    enabled: Boolean = true
) {
    val bgBrush = if (isPrimary) {
        Brush.horizontalGradient(
            colors = listOf(StonicNeonPink, Color(0xFFFF1493))
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF1E1E2E), Color(0xFF161622))
        )
    }

    val borderBrush = if (isPrimary) {
        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.5f), StonicNeonPink))
    } else {
        Brush.horizontalGradient(listOf(StonicBorderPink, StonicBorderSubtle))
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (enabled) bgBrush else Brush.linearGradient(listOf(Color.DarkGray, Color.Gray)))
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPrimary) Color.White else StonicNeonPink,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
