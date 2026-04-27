package com.linetra.permwatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linetra.permwatch.data.PermissionCategory
import com.linetra.permwatch.data.SensitivePermission
import com.linetra.permwatch.data.SensitivePermissions
import com.linetra.permwatch.ui.atoms.Glass
import com.linetra.permwatch.ui.atoms.Iris
import com.linetra.permwatch.ui.theme.LocalHolo
import com.linetra.permwatch.ui.theme.Mono

@Composable
fun Settings(
    unwatched: Set<String>,
    onSetWatched: (String, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val palette = LocalHolo.current
    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navBars = WindowInsets.navigationBars.asPaddingValues()

    val grouped = remember { SensitivePermissions.all.groupBy { it.category } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBars.calculateTopPadding()),
    ) {
        SettingsHeader(onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = 4.dp, bottom = 24.dp + navBars.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { Intro(palette = palette) }
            PermissionCategory.values().forEach { cat ->
                val perms = grouped[cat].orEmpty()
                if (perms.isEmpty()) return@forEach
                item(key = "head-${cat.name}") {
                    SectionHeader(title = stringResource(cat.displayRes))
                }
                item(key = "card-${cat.name}") {
                    PermGroup(
                        perms = perms,
                        unwatched = unwatched,
                        onSetWatched = onSetWatched,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackBox(onClick = onBack, color = palette.ink)
        Spacer(Modifier.width(6.dp))
        Text(
            text = "WATCHING",
            color = palette.inkSoft,
            fontFamily = Mono,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.5.sp,
        )
        Spacer(Modifier.weight(1f))
        Iris(size = 22.dp, speedMillis = 18_000)
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun BackBox(onClick: () -> Unit, color: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.4.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * 0.90f, size.height * 0.50f)
                lineTo(size.width * 0.10f, size.height * 0.50f)
                moveTo(size.width * 0.45f, size.height * 0.20f)
                lineTo(size.width * 0.10f, size.height * 0.50f)
                lineTo(size.width * 0.45f, size.height * 0.80f)
            }
            drawPath(path = path, color = color, style = stroke)
        }
    }
}

@Composable
private fun Intro(palette: com.linetra.permwatch.ui.theme.HoloPalette) {
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
        Text(
            text = "Choose the signal.",
            color = palette.ink,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            letterSpacing = (-0.4).sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Anything you turn off no longer fires alerts. Turning a permission back on " +
                "silently accepts existing grants — only future ones will signal.",
            color = palette.inkSoft,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(palette.accentA, palette.accentB))),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title.uppercase(),
            color = palette.inkSoft,
            fontFamily = Mono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun PermGroup(
    perms: List<SensitivePermission>,
    unwatched: Set<String>,
    onSetWatched: (String, Boolean) -> Unit,
) {
    val palette = LocalHolo.current
    Glass(modifier = Modifier.fillMaxWidth()) {
        Column {
            perms.forEachIndexed { idx, perm ->
                if (idx > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(0.5.dp)
                            .background(palette.stroke),
                    )
                }
                PermRow(
                    perm = perm,
                    watched = perm.manifestName !in unwatched,
                    onSetWatched = { on -> onSetWatched(perm.manifestName, on) },
                )
            }
        }
    }
}

@Composable
private fun PermRow(
    perm: SensitivePermission,
    watched: Boolean,
    onSetWatched: (Boolean) -> Unit,
) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSetWatched(!watched) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = perm.shortLabel,
                color = palette.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = perm.manifestName.substringAfterLast('.'),
                color = palette.inkMute,
                fontFamily = Mono,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        ToggleSwitch(on = watched)
    }
}

@Composable
private fun ToggleSwitch(on: Boolean) {
    val palette = LocalHolo.current
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (on) Brush.linearGradient(listOf(palette.accentA, palette.accentC))
                else Brush.linearGradient(
                    listOf(
                        if (palette.isDark) Color.White.copy(alpha = 0.10f) else palette.bgDeep,
                        if (palette.isDark) Color.White.copy(alpha = 0.10f) else palette.bgDeep,
                    ),
                ),
            ),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(palette.ink),
        )
    }
}

