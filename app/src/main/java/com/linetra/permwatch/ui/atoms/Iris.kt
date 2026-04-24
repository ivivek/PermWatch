package com.linetra.permwatch.ui.atoms

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.linetra.permwatch.ui.theme.LocalHolo

@Composable
fun Iris(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    speedMillis: Int = 14_000,
    still: Boolean = false,
) {
    val palette = LocalHolo.current

    val rotation = if (still) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "iris")
        val a by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(speedMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "iris-rotation",
        )
        a
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // Bloom — soft radial halo behind the ring, ~1.6x the iris size
        Box(
            modifier = Modifier
                .size(size * 1.6f)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.accentC.copy(alpha = if (palette.isDark) 0.33f else 0.20f),
                                palette.accentA.copy(alpha = if (palette.isDark) 0.18f else 0.10f),
                                Color.Transparent,
                            ),
                            center = Offset(this.size.width / 2f, this.size.height / 2f),
                            radius = this.size.width / 2f,
                        ),
                    )
                },
        )
        // Ring — sweep gradient, rotated
        Box(
            modifier = Modifier
                .size(size)
                .rotate(rotation)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        0f to palette.accentC,
                        0.33f to palette.accentA,
                        0.66f to palette.accentB,
                        1f to palette.accentC,
                    ),
                ),
        )
        // Pupil — dark inset disc
        Box(
            modifier = Modifier
                .size(size * 0.72f)
                .clip(CircleShape)
                .background(palette.bgDeep),
        )
        // Centre — bright ink dot
        Box(
            modifier = Modifier
                .size(size * 0.36f)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.ink.copy(alpha = 0.5f),
                                Color.Transparent,
                            ),
                            radius = this.size.width / 2f * 1.6f,
                        ),
                    )
                }
                .clip(CircleShape)
                .background(palette.ink),
        )
    }
}
