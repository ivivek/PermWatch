package com.linetra.permwatch.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.linetra.permwatch.worker.ScanCadence

@Composable
fun Settings(
    unwatched: Set<String>,
    intervalSeconds: Long,
    ignoredApps: List<IgnoredApp>,
    onSetWatched: (String, Boolean) -> Unit,
    onSetInterval: (Long) -> Unit,
    onSetIgnored: (String, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val palette = LocalHolo.current
    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navBars = WindowInsets.navigationBars.asPaddingValues()

    val grouped = remember { SensitivePermissions.all.groupBy { it.category } }
    var cadenceSheetOpen by rememberSaveable { mutableStateOf(false) }
    var ignoredSheetOpen by rememberSaveable { mutableStateOf(false) }

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
            item(key = "head-cadence") { SectionHeader(title = "How often") }
            item(key = "card-cadence") {
                CadenceSummaryRow(
                    intervalSeconds = intervalSeconds,
                    onClick = { cadenceSheetOpen = true },
                )
            }
            item(key = "head-ignored") { SectionHeader(title = "Ignored apps") }
            item(key = "card-ignored") {
                IgnoredSummaryRow(
                    count = ignoredApps.size,
                    onClick = { ignoredSheetOpen = true },
                )
            }
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

    if (cadenceSheetOpen) {
        CadenceSheet(
            intervalSeconds = intervalSeconds,
            onSelect = { seconds ->
                onSetInterval(seconds)
                cadenceSheetOpen = false
            },
            onDismiss = { cadenceSheetOpen = false },
        )
    }

    if (ignoredSheetOpen) {
        IgnoredAppsSheet(
            apps = ignoredApps,
            onRewatch = { pkg -> onSetIgnored(pkg, false) },
            onDismiss = { ignoredSheetOpen = false },
        )
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
private fun CadenceSummaryRow(intervalSeconds: Long, onClick: () -> Unit) {
    val palette = LocalHolo.current
    val label = ScanCadence.presets.firstOrNull { it.seconds == intervalSeconds }?.label
        ?: "${intervalSeconds}s"
    Glass(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = palette.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            ChevronRightGlyph(color = palette.inkSoft)
        }
    }
}

@Composable
private fun ChevronRightGlyph(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.4.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.32f, size.height * 0.20f)
            lineTo(size.width * 0.70f, size.height * 0.50f)
            lineTo(size.width * 0.32f, size.height * 0.80f)
        }
        drawPath(path, color = color, style = stroke)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadenceSheet(
    intervalSeconds: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalHolo.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val navBars = WindowInsets.navigationBars.asPaddingValues()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bgDeep,
        contentColor = palette.ink,
        contentWindowInsets = { WindowInsets.statusBars },
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.stroke),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = navBars.calculateBottomPadding()),
        ) {
            Text(
                text = "HOW OFTEN",
                color = palette.inkSoft,
                fontFamily = Mono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "card") {
                    Glass(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            ScanCadence.presets.forEachIndexed { idx, preset ->
                                if (idx > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(0.5.dp)
                                            .background(palette.stroke),
                                    )
                                }
                                CadencePresetRow(
                                    preset = preset,
                                    selected = preset.seconds == intervalSeconds,
                                    onClick = { onSelect(preset.seconds) },
                                )
                            }
                        }
                    }
                }
                item(key = "caption") {
                    Text(
                        text = "Faster cadences use more battery. Android may delay scans on idle devices.",
                        color = palette.inkMute,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CadencePresetRow(
    preset: ScanCadence.Preset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = preset.label,
            color = palette.ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        RadioDot(selected = selected)
    }
}

@Composable
private fun IgnoredSummaryRow(count: Int, onClick: () -> Unit) {
    val palette = LocalHolo.current
    val label = when (count) {
        0 -> "None"
        1 -> "1 app"
        else -> "$count apps"
    }
    Glass(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = if (count == 0) palette.inkMute else palette.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            ChevronRightGlyph(color = palette.inkSoft)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IgnoredAppsSheet(
    apps: List<IgnoredApp>,
    onRewatch: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalHolo.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val navBars = WindowInsets.navigationBars.asPaddingValues()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bgDeep,
        contentColor = palette.ink,
        contentWindowInsets = { WindowInsets.statusBars },
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.stroke),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = navBars.calculateBottomPadding()),
        ) {
            Text(
                text = "IGNORED APPS",
                color = palette.inkSoft,
                fontFamily = Mono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (apps.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = "Nothing ignored. Use the toggle on any app card to mute it \u2014 it will move here.",
                            color = palette.inkSoft,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                        )
                    }
                } else {
                    items(apps, key = { it.packageName }) { app ->
                        Glass(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(
                                    fadeInSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                                    fadeOutSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                                    placementSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                                ),
                        ) {
                            IgnoredAppRow(app = app, onRewatch = { onRewatch(app.packageName) })
                        }
                    }
                    item(key = "caption") {
                        Text(
                            text = "Toggle on to resume watching. The app will reappear in the main feed if it holds any watched permissions.",
                            color = palette.inkMute,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IgnoredAppRow(app: IgnoredApp, onRewatch: () -> Unit) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRewatch)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = app.label,
                color = palette.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = app.packageName,
                color = palette.inkMute,
                fontFamily = Mono,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        ToggleSwitch(on = false)
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val palette = LocalHolo.current
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(
                if (selected) Brush.linearGradient(listOf(palette.accentA, palette.accentC))
                else Brush.linearGradient(
                    listOf(
                        if (palette.isDark) Color.White.copy(alpha = 0.06f) else palette.bgDeep,
                        if (palette.isDark) Color.White.copy(alpha = 0.06f) else palette.bgDeep,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(palette.bgDeep),
            )
        }
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

