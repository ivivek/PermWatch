package com.linetra.permwatch.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linetra.permwatch.ui.atoms.Iris
import com.linetra.permwatch.ui.theme.LocalHolo
import com.linetra.permwatch.ui.theme.Mono

private data class Slide(val kicker: String, val title: String, val body: String)

private val SLIDES = listOf(
    Slide(
        kicker = "Signal",
        title = "A watchtower for your phone.",
        body = "PermWatch keeps a calm eye on what sees, hears, and follows you.",
    ),
    Slide(
        kicker = "Change",
        title = "You hear when something shifts.",
        body = "The moment an app reaches for more than it had, a quiet signal comes through.",
    ),
    Slide(
        kicker = "You",
        title = "You stay in control.",
        body = "Tap Manage to revoke in Android Settings. PermWatch never changes a permission itself.",
    ),
)

@Composable
fun Intro(onActivate: () -> Unit) {
    val palette = LocalHolo.current
    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navBars = WindowInsets.navigationBars.asPaddingValues()
    var step by remember { mutableIntStateOf(0) }
    val s = SLIDES[step]
    val isLast = step == SLIDES.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .drawBehind {
                // Top-left iridescent halo
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.accentC.copy(alpha = if (palette.isDark) 0.13f else 0.10f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.30f, size.height * 0.10f),
                        radius = size.maxDimension * 0.55f,
                    ),
                    size = Size(size.width, size.height),
                )
                // Bottom-right iridescent halo
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.accentB.copy(alpha = if (palette.isDark) 0.13f else 0.10f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.80f, size.height * 0.90f),
                        radius = size.maxDimension * 0.55f,
                    ),
                    size = Size(size.width, size.height),
                )
            }
            .padding(top = statusBars.calculateTopPadding(), bottom = navBars.calculateBottomPadding()),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top mark
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Iris(size = 22.dp, speedMillis = 22_000)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "PERMWATCH",
                    color = palette.inkSoft,
                    fontFamily = Mono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                )
            }

            // Hero iris — fills remaining space, centered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Iris(size = 190.dp, speedMillis = 24_000)
            }

            // Slide copy
            Column(modifier = Modifier.padding(horizontal = 28.dp)) {
                Text(
                    text = "${(step + 1).toString().padStart(2, '0')} · ${s.kicker.uppercase()}",
                    color = palette.accentA,
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.5.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = s.title,
                    color = palette.ink,
                    fontWeight = FontWeight.Medium,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.5).sp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = s.body,
                    color = palette.inkSoft,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    modifier = Modifier.widthIn(max = 320.dp),
                )
            }

            // Footer — pagination + button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Pagination(
                    count = SLIDES.size,
                    active = step,
                    onSelect = { step = it },
                )
                Spacer(Modifier.weight(1f))
                if (isLast) {
                    PrimaryButton(label = "Activate", onClick = onActivate)
                } else {
                    GhostButton(label = "Continue", onClick = { step += 1 })
                }
            }
        }
    }
}

@Composable
private fun Pagination(count: Int, active: Int, onSelect: (Int) -> Unit) {
    val palette = LocalHolo.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val isActive = i == active
            val width by animateDpAsState(
                targetValue = if (isActive) 28.dp else 14.dp,
                animationSpec = tween(durationMillis = 240),
                label = "dot-width-$i",
            )
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(width)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActive) Brush.linearGradient(listOf(palette.accentA, palette.accentB))
                        else Brush.linearGradient(listOf(palette.stroke, palette.stroke)),
                    )
                    .clickable(onClick = { onSelect(i) }),
            )
        }
    }
}

@Composable
private fun GhostButton(label: String, onClick: () -> Unit) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (palette.isDark) Color.White.copy(alpha = 0.06f) else palette.bgDeep)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            color = palette.ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
        )
        Arrow(color = palette.ink)
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(palette.accentA, palette.accentC)))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            color = palette.bgDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
        )
        Arrow(color = palette.bgDeep)
    }
}

@Composable
private fun Arrow(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(13.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.4.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            // horizontal line
            moveTo(size.width * 0.10f, size.height * 0.50f)
            lineTo(size.width * 0.90f, size.height * 0.50f)
            // arrowhead
            moveTo(size.width * 0.55f, size.height * 0.20f)
            lineTo(size.width * 0.90f, size.height * 0.50f)
            lineTo(size.width * 0.55f, size.height * 0.80f)
        }
        drawPath(path = path, color = color, style = stroke)
    }
}

