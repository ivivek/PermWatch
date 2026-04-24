package com.linetra.permwatch.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linetra.permwatch.data.SensitivePermissions
import com.linetra.permwatch.ui.atoms.Glass
import com.linetra.permwatch.ui.atoms.Iris
import com.linetra.permwatch.ui.theme.LocalHolo
import com.linetra.permwatch.ui.theme.Mono

private enum class TabId(val label: String) { User("User"), System("System") }

@Composable
fun AppScaffold(
    state: UiState,
    onRescan: () -> Unit,
    onAcceptApp: (String) -> Unit,
    onAcceptAll: () -> Unit,
    onToggleIgnore: (String, Boolean) -> Unit,
    onManage: (String) -> Unit,
) {
    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navBars = WindowInsets.navigationBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBars.calculateTopPadding()),
    ) {
        MainHeader(onRescan = onRescan, scanning = state.loading)

        if (state.loading && state.rows.isEmpty()) {
            LoadingList()
        } else {
            AppListWithTabs(
                state = state,
                onAcceptApp = onAcceptApp,
                onAcceptAll = onAcceptAll,
                onToggleIgnore = onToggleIgnore,
                onManage = onManage,
                bottomInset = navBars.calculateBottomPadding(),
            )
        }
    }
}

// ── Header ─────────────────────────────────────────────

@Composable
private fun MainHeader(onRescan: () -> Unit, scanning: Boolean) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Iris(size = 22.dp, speedMillis = 16_000)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "PermWatch",
            color = palette.inkSoft,
            fontFamily = Mono,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.5.sp,
        )
        Spacer(Modifier.weight(1f))
        IconBox(onClick = onRescan) {
            val rot = if (scanning) {
                val t = rememberInfiniteTransition(label = "scan-rot")
                val v by t.animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "scan-rot-anim",
                )
                v
            } else 0f
            Box(modifier = Modifier.rotate(rot)) {
                RefreshGlyph(color = palette.ink)
            }
        }
    }
}

@Composable
private fun IconBox(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun RefreshGlyph(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.3.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        val s = size
        // Arc: roughly 270° of a circle inset by 15% — the "swoop"
        drawArc(
            color = color,
            startAngle = -50f,
            sweepAngle = 280f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(s.width * 0.10f, s.height * 0.10f),
            size = androidx.compose.ui.geometry.Size(s.width * 0.80f, s.height * 0.80f),
            style = stroke,
        )
        // Arrow head — short L at top right
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(s.width * 0.85f, s.height * 0.15f)
            lineTo(s.width * 0.85f, s.height * 0.42f)
            moveTo(s.width * 0.85f, s.height * 0.15f)
            lineTo(s.width * 0.55f, s.height * 0.15f)
        }
        drawPath(path, color = color, style = stroke)
    }
}

// ── List + tabs ────────────────────────────────────────

@Composable
private fun AppListWithTabs(
    state: UiState,
    onAcceptApp: (String) -> Unit,
    onAcceptAll: () -> Unit,
    onToggleIgnore: (String, Boolean) -> Unit,
    onManage: (String) -> Unit,
    bottomInset: androidx.compose.ui.unit.Dp,
) {
    val userApps = remember(state.rows) { state.rows.filter { !it.isSystem } }
    val systemApps = remember(state.rows) { state.rows.filter { it.isSystem } }
    var selected by rememberSaveable { mutableStateOf(TabId.User) }
    val visible = if (selected == TabId.User) userApps else systemApps

    Column(Modifier.fillMaxSize()) {
        AlertStrip(count = state.alertCount, onAcceptAll = onAcceptAll)
        Tabs(
            value = selected,
            onChange = { selected = it },
            userCount = userApps.size,
            systemCount = systemApps.size,
        )
        if (visible.isEmpty()) {
            EmptyState(tab = selected)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 20.dp + bottomInset),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(visible, key = { it.packageName }) { row ->
                    AppCard(
                        row = row,
                        onAccept = { onAcceptApp(row.packageName) },
                        onToggleIgnore = { ignored -> onToggleIgnore(row.packageName, ignored) },
                        onManage = { onManage(row.packageName) },
                    )
                }
            }
        }
    }
}

// ── Alert strip ────────────────────────────────────────

@Composable
private fun AlertStrip(count: Int, onAcceptAll: () -> Unit) {
    val palette = LocalHolo.current
    if (count == 0) {
        Glass(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Pulse dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(palette.accentA),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Field is quiet",
                        color = palette.ink,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "No new access since your last baseline.",
                        color = palette.inkMute,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    } else {
        Glass(
            hi = true,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Iris(size = 40.dp, speedMillis = 10_000)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    GradientText(
                        text = "SIGNAL · SINCE BASELINE",
                        fontFamily = Mono,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        gradient = Brush.linearGradient(listOf(palette.accentA, palette.accentB)),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (count == 1) "$count app has new access." else "$count apps have new access.",
                        color = palette.ink,
                        fontWeight = FontWeight.Medium,
                        fontSize = 19.sp,
                        letterSpacing = (-0.3).sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
                GhostButton(onClick = onAcceptAll, label = "Accept all")
            }
        }
    }
}

// ── Tabs ───────────────────────────────────────────────

@Composable
private fun Tabs(
    value: TabId,
    onChange: (TabId) -> Unit,
    userCount: Int,
    systemCount: Int,
) {
    val palette = LocalHolo.current
    Glass(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 14.dp)
            .fillMaxWidth(),
        cornerRadius = 14.dp,
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            TabButton(
                modifier = Modifier.weight(1f),
                active = value == TabId.User,
                label = "User",
                count = userCount,
                onClick = { onChange(TabId.User) },
                palette = palette,
            )
            Spacer(Modifier.width(4.dp))
            TabButton(
                modifier = Modifier.weight(1f),
                active = value == TabId.System,
                label = "System",
                count = systemCount,
                onClick = { onChange(TabId.System) },
                palette = palette,
            )
        }
    }
}

@Composable
private fun TabButton(
    modifier: Modifier,
    active: Boolean,
    label: String,
    count: Int,
    onClick: () -> Unit,
    palette: com.linetra.permwatch.ui.theme.HoloPalette,
) {
    val bg = if (active) {
        Brush.linearGradient(
            colors = listOf(
                palette.accentA.copy(alpha = 0.13f),
                palette.accentB.copy(alpha = 0.13f),
            ),
        )
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (active) palette.ink else palette.inkMute,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.3.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.toString(),
            fontFamily = Mono,
            fontSize = 10.sp,
            color = if (active) palette.inkSoft else palette.inkMute,
        )
    }
}

// ── Empty state ────────────────────────────────────────

@Composable
private fun EmptyState(tab: TabId) {
    val palette = LocalHolo.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Iris(size = 54.dp, speedMillis = 18_000)
            Spacer(Modifier.height(16.dp))
            Text(
                "Nothing sensitive here.",
                color = palette.ink,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "No ${if (tab == TabId.User) "installed" else "system"} apps currently hold any of the watched permissions.",
                color = palette.inkSoft,
                fontSize = 12.sp,
                modifier = Modifier.width(260.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// ── Loading skeleton ───────────────────────────────────

@Composable
private fun LoadingList() {
    val palette = LocalHolo.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(palette.accentA, palette.accentB)),
                    ),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "SWEEPING FIELD",
                color = palette.accentA,
                fontFamily = Mono,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )
        }
        repeat(4) {
            Glass(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surfaceHi),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Name + pkg lines
                        Box(
                            modifier = Modifier
                                .height(11.dp)
                                .fillMaxWidth(0.55f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.surfaceHi),
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .fillMaxWidth(0.78f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.surface),
                        )
                        // Chip placeholders
                        Spacer(Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(60.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(palette.surface),
                            )
                            Box(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(48.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(palette.surface),
                            )
                            Box(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(68.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(palette.surface),
                            )
                        }
                        // Divider
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(palette.stroke),
                        )
                        // Bottom row — toggle pill + manage placeholder
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width(28.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(palette.surfaceHi),
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .height(10.dp)
                                    .width(60.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.surface),
                            )
                            Spacer(Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .height(12.dp)
                                    .width(54.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.surface),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── App card ───────────────────────────────────────────

@Composable
private fun AppCard(
    row: AppRow,
    onAccept: () -> Unit,
    onToggleIgnore: (Boolean) -> Unit,
    onManage: () -> Unit,
) {
    val palette = LocalHolo.current
    val hasAlert = row.hasAlert
    val ignored = row.isIgnored
    val newPerms = row.newPerms
    val baseline = row.granted - newPerms

    val alertBorder = if (hasAlert) {
        Brush.linearGradient(
            listOf(
                palette.accentA.copy(alpha = if (palette.isDark) 0.55f else 0.65f),
                palette.accentB.copy(alpha = if (palette.isDark) 0.55f else 0.65f),
            ),
        )
    } else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .let { if (ignored) it.alpha(0.5f) else it },
    ) {
        Glass(
            hi = hasAlert,
            borderBrush = alertBorder,
            borderWidth = if (hasAlert) 1.dp else 0.5.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                AppAvatar(label = row.label, packageName = row.packageName, palette = palette)
                Column(Modifier.weight(1f)) {
                    // Name + N new
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = row.label,
                            color = palette.ink,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (hasAlert) {
                            GradientText(
                                text = "${newPerms.size} NEW",
                                fontFamily = Mono,
                                fontSize = 10.sp,
                                letterSpacing = 1.5.sp,
                                gradient = Brush.linearGradient(listOf(palette.accentA, palette.accentB)),
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = row.packageName + if (row.isSystem) " · system" else "",
                        color = palette.inkMute,
                        fontFamily = Mono,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(10.dp))
                    PermChips(newPerms = newPerms, baseline = baseline)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(palette.stroke),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WatchToggle(on = !ignored, onChange = { onToggleIgnore(ignored) })
                        Spacer(Modifier.weight(1f))
                        ManageButton(onClick = onManage)
                        if (hasAlert) {
                            Spacer(Modifier.width(10.dp))
                            AcceptButton(onClick = onAccept)
                        }
                    }
                }
            }
        }
        if (hasAlert) {
            // Glowing left edge marker — full card height minus padding
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(vertical = 14.dp)
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(listOf(palette.accentA, palette.accentB)),
                    ),
            )
        }
    }
}

@Composable
private fun AppAvatar(label: String, packageName: String, palette: com.linetra.permwatch.ui.theme.HoloPalette) {
    val initial = label.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
    val hue = ((packageName.hashCode().toLong() and 0xFFFFFFFFL) % 360L).toFloat()
    val c1 = Color.hsl(hue, saturation = 0.55f, lightness = if (palette.isDark) 0.42f else 0.55f)
    val c2 = Color.hsl((hue + 40f) % 360f, saturation = 0.50f, lightness = if (palette.isDark) 0.30f else 0.45f)
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(c1, c2))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            letterSpacing = (-0.3).sp,
        )
    }
}

// ── Chips ──────────────────────────────────────────────

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PermChips(newPerms: Set<String>, baseline: Set<String>) {
    val palette = LocalHolo.current
    if (newPerms.isEmpty() && baseline.isEmpty()) {
        Text(
            "no sensitive permissions",
            color = palette.inkMute,
            fontSize = 11.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        )
        return
    }
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        newPerms.sorted().forEach { perm ->
            NewChip(label = SensitivePermissions.labelFor(perm))
        }
        baseline.sorted().forEach { perm ->
            BaselineChip(label = SensitivePermissions.labelFor(perm))
        }
    }
}

@Composable
private fun NewChip(label: String) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.accentA.copy(alpha = 0.20f),
                        palette.accentB.copy(alpha = 0.20f),
                    ),
                ),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(palette.accentA, palette.accentB))),
        )
        Text(
            text = label,
            color = palette.ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
private fun BaselineChip(label: String) {
    val palette = LocalHolo.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (palette.isDark) Color.White.copy(alpha = 0.04f) else palette.bgDeep)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            color = palette.inkSoft,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
        )
    }
}

// ── Buttons ────────────────────────────────────────────

@Composable
private fun WatchToggle(on: Boolean, onChange: () -> Unit) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (on) Brush.linearGradient(listOf(palette.accentA, palette.accentC))
                    else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.10f))),
                ),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.5.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(palette.ink),
            )
        }
        Text(
            text = if (on) "Watching" else "Ignored",
            color = if (on) palette.inkSoft else palette.inkMute,
            fontSize = 11.sp,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
private fun ManageButton(onClick: () -> Unit) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Manage",
            color = palette.inkSoft,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun AcceptButton(onClick: () -> Unit) {
    val palette = LocalHolo.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(listOf(palette.accentA, palette.accentB)),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = "Got it",
            color = palette.bgDeep,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
        )
    }
}

@Composable
private fun GhostButton(onClick: () -> Unit, label: String) {
    val palette = LocalHolo.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (palette.isDark) Color.White.copy(alpha = 0.06f) else palette.bgDeep)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            color = palette.ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
        )
    }
}

// ── Helpers ────────────────────────────────────────────

@Composable
private fun GradientText(
    text: String,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    fontSize: androidx.compose.ui.unit.TextUnit,
    letterSpacing: androidx.compose.ui.unit.TextUnit,
    gradient: Brush,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(brush = gradient)) {
                append(text)
            }
        },
        fontFamily = fontFamily,
        fontSize = fontSize,
        letterSpacing = letterSpacing,
        fontWeight = FontWeight.Medium,
    )
}

