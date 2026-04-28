package com.linetra.permissionalerts.ui.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.linetra.permissionalerts.ui.theme.LocalHolo

@Composable
fun Glass(
    modifier: Modifier = Modifier,
    hi: Boolean = false,
    cornerRadius: Dp = 16.dp,
    borderBrush: Brush? = null,
    borderWidth: Dp = 0.5.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val palette = LocalHolo.current
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val bg = if (hi) palette.surfaceHi else palette.surface
    val brush: Brush = borderBrush ?: SolidColor(if (hi) palette.strokeHi else palette.stroke)

    val base = modifier
        .clip(shape)
        .background(bg)
        .border(BorderStroke(borderWidth, brush), shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Box(modifier = base) { content() }
}
