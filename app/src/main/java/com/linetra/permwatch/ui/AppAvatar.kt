package com.linetra.permwatch.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.linetra.permwatch.ui.theme.HoloPalette
import com.linetra.permwatch.ui.theme.LocalHolo

@Composable
internal fun AppAvatar(
    label: String,
    packageName: String,
    size: Dp = 42.dp,
) {
    val palette = LocalHolo.current
    val context = LocalContext.current
    val icon: ImageBitmap? = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap(width = 144, height = 144)
                .asImageBitmap()
        }.getOrNull()
    }
    val shape = RoundedCornerShape(12.dp)
    val avatarModifier = Modifier
        .size(size)
        .clip(shape)
        .border(0.5.dp, palette.stroke, shape)
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = avatarModifier,
        )
    } else {
        FallbackAvatar(label = label, packageName = packageName, palette = palette, modifier = avatarModifier, fontSize = (size.value * 0.43f).sp)
    }
}

@Composable
private fun FallbackAvatar(
    label: String,
    packageName: String,
    palette: HoloPalette,
    modifier: Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit,
) {
    val initial = label.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
    val hue = ((packageName.hashCode().toLong() and 0xFFFFFFFFL) % 360L).toFloat()
    val c1 = Color.hsl(hue, saturation = 0.55f, lightness = if (palette.isDark) 0.42f else 0.55f)
    val c2 = Color.hsl((hue + 40f) % 360f, saturation = 0.50f, lightness = if (palette.isDark) 0.30f else 0.45f)
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(c1, c2))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
            letterSpacing = (-0.3).sp,
        )
    }
}
