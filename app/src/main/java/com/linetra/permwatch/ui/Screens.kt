package com.linetra.permwatch.ui

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
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
import com.linetra.permwatch.data.PermissionCategory
import com.linetra.permwatch.data.PermissionGroup
import com.linetra.permwatch.data.PermissionGroups
import com.linetra.permwatch.data.SensitivePermissions
import com.linetra.permwatch.ui.theme.HoloPalette
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
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    unreadEventCount: Int = 0,
    scrollToAlert: Flow<Unit> = emptyFlow(),
) {
    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navBars = WindowInsets.navigationBars.asPaddingValues()
    val palette = LocalHolo.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Wrap the toggle so ignoring (true) shows a snackbar with Undo. Re-watching skips the
    // snackbar — the action itself is the undo path (e.g. Settings → Ignored apps).
    val handleToggleIgnore: (String, Boolean) -> Unit = { pkg, ignored ->
        onToggleIgnore(pkg, ignored)
        if (ignored) {
            val label = state.rows.firstOrNull { it.packageName == pkg }?.label ?: pkg
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Ignored $label",
                    actionLabel = "Undo",
                    withDismissAction = false,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onToggleIgnore(pkg, false)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBars.calculateTopPadding()),
        ) {
            MainHeader(
                onRescan = onRescan,
                scanning = state.loading,
                onOpenHistory = onOpenHistory,
                unreadEventCount = unreadEventCount,
                onOpenSettings = onOpenSettings,
            )

            if (state.loading && state.rows.isEmpty()) {
                LoadingList()
            } else {
                AppListWithTabs(
                    state = state,
                    onAcceptApp = onAcceptApp,
                    onAcceptAll = onAcceptAll,
                    onToggleIgnore = handleToggleIgnore,
                    onManage = onManage,
                    bottomInset = navBars.calculateBottomPadding(),
                    scrollToAlert = scrollToAlert,
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBars.calculateBottomPadding() + 8.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = palette.surfaceHi,
                contentColor = palette.ink,
                actionColor = palette.accentA,
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}

// ── Header ─────────────────────────────────────────────

@Composable
private fun MainHeader(
    onRescan: () -> Unit,
    scanning: Boolean,
    onOpenHistory: () -> Unit,
    unreadEventCount: Int,
    onOpenSettings: () -> Unit,
) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(8.dp))
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
        IconBox(onClick = onOpenHistory) {
            Box(modifier = Modifier.size(18.dp)) {
                BellGlyph(color = palette.ink)
                if (unreadEventCount > 0) {
                    BellBadge(
                        count = unreadEventCount,
                        palette = palette,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 5.dp, y = (-4).dp),
                    )
                }
            }
        }
        IconBox(onClick = onOpenSettings) {
            GearGlyph(color = palette.ink)
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
private fun BellBadge(count: Int, palette: HoloPalette, modifier: Modifier = Modifier) {
    val label = if (count > 99) "99+" else count.toString()
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 12.dp, minHeight = 12.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(palette.accentA, palette.accentB)))
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = palette.bgDeep,
            fontFamily = Mono,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            lineHeight = 8.sp,
        )
    }
}

@Composable
private fun BellGlyph(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.3.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        val w = size.width
        val h = size.height
        // Body: tall dome arches from straight sides up and over the top, then a wider lip
        // at the base. Proportions tuned so the dome dominates the silhouette rather than
        // the previous flat-top look.
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.22f, h * 0.72f)
            lineTo(w * 0.22f, h * 0.48f)
            cubicTo(
                w * 0.22f, h * 0.16f,
                w * 0.78f, h * 0.16f,
                w * 0.78f, h * 0.48f,
            )
            lineTo(w * 0.78f, h * 0.72f)
            lineTo(w * 0.92f, h * 0.80f)
            lineTo(w * 0.08f, h * 0.80f)
            close()
        }
        drawPath(path, color = color, style = stroke)
        // Clapper — small arc hanging under the lip.
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.40f, h * 0.82f),
            size = androidx.compose.ui.geometry.Size(w * 0.20f, h * 0.12f),
            style = stroke,
        )
        // Top knob.
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.06f),
            end = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.16f),
            strokeWidth = stroke.width,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
private fun GearGlyph(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.3.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        val w = size.width
        val h = size.height
        // Three horizontal sliders, each with a small filled knob at different position
        val ys = floatArrayOf(h * 0.25f, h * 0.50f, h * 0.75f)
        val knobX = floatArrayOf(w * 0.30f, w * 0.65f, w * 0.45f)
        val knobR = w * 0.085f
        for (i in 0 until 3) {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(w * 0.10f, ys[i]),
                end = androidx.compose.ui.geometry.Offset(w * 0.90f, ys[i]),
                strokeWidth = stroke.width,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawCircle(
                color = color,
                radius = knobR,
                center = androidx.compose.ui.geometry.Offset(knobX[i], ys[i]),
            )
        }
    }
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
    scrollToAlert: Flow<Unit>,
) {
    // Ignored apps are managed from Settings → Ignored apps. They never appear in the main
    // feed — the feed is the signal stream, ignoring something clears it.
    val userApps = remember(state.rows) { state.rows.filter { !it.isSystem && !it.isIgnored } }
    val systemApps = remember(state.rows) { state.rows.filter { it.isSystem && !it.isIgnored } }
    var selected by rememberSaveable { mutableStateOf(TabId.User) }

    var groupFilters by rememberSaveable(saver = PermFilterSaver) { mutableStateOf(emptySet<String>()) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }

    val listState = rememberLazyListState()
    // The VM emits on [scrollToAlert] only after a notification-tap-driven scan has finished,
    // so the lists below reflect the post-tap rows by the time the collector fires. We pick
    // the tab where the alert actually lives, clear filters that might hide it, and reset
    // scroll. userApps/systemApps are plain vals (rebuilt each composition), so the long-
    // lived collector closure has to read them via rememberUpdatedState or it'll see the
    // stale list captured at LaunchedEffect launch time. selected/groupFilters/sheetOpen are
    // already snapshot-state delegates, so direct reads are fresh.
    val latestUserApps by rememberUpdatedState(userApps)
    val latestSystemApps by rememberUpdatedState(systemApps)
    LaunchedEffect(Unit) {
        scrollToAlert.collect {
            val selectedTabHasAlert =
                (if (selected == TabId.User) latestUserApps else latestSystemApps).any { it.hasAlert }
            val target = when {
                selectedTabHasAlert -> selected
                latestUserApps.any { it.hasAlert } -> TabId.User
                latestSystemApps.any { it.hasAlert } -> TabId.System
                else -> selected
            }
            if (target != selected) selected = target
            if (groupFilters.isNotEmpty()) groupFilters = emptySet()
            sheetOpen = false
            listState.scrollToItem(0)
        }
    }

    // Perms granted by at least one app in the current scan.
    val availablePerms = remember(state.rows) {
        state.rows.flatMapTo(HashSet()) { it.granted }
    }
    // Groups with at least one member granted somewhere in the current scan.
    val availableGroups = remember(availablePerms) {
        PermissionGroups.all.filter { g -> g.members.any { it in availablePerms } }
    }
    val availableGroupIds = remember(availableGroups) { availableGroups.map { it.id }.toSet() }
    // Drop selections for groups that vanished (all members revoked / app uninstalled).
    val activeFilters = remember(groupFilters, availableGroupIds) {
        groupFilters intersect availableGroupIds
    }
    // Union of every member perm across the active groups — used for fast row matching.
    val activePerms = remember(activeFilters) {
        activeFilters.flatMapTo(HashSet()) { id -> PermissionGroups.byId[id]?.members.orEmpty() }
    }

    val tabApps = if (selected == TabId.User) userApps else systemApps
    val matches: (AppRow) -> Boolean = { row -> row.granted.any { it in activePerms } }
    val visible = if (activeFilters.isNotEmpty()) tabApps.filter(matches) else tabApps
    val userCount = if (activeFilters.isNotEmpty()) userApps.count(matches) else userApps.size
    val systemCount = if (activeFilters.isNotEmpty()) systemApps.count(matches) else systemApps.size

    Column(Modifier.fillMaxSize()) {
        AlertStrip(count = state.alertCount, onAcceptAll = onAcceptAll)
        Tabs(
            value = selected,
            onChange = { selected = it },
            userCount = userCount,
            systemCount = systemCount,
            filterActiveCount = activeFilters.size,
            onOpenFilter = { sheetOpen = true },
        )
        if (visible.isEmpty()) {
            EmptyState(
                tab = selected,
                filterCount = activeFilters.size,
                singleFilterLabel = activeFilters.singleOrNull()?.let { PermissionGroups.byId[it]?.label },
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 20.dp + bottomInset),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(visible, key = { it.packageName }) { row ->
                    AppCard(
                        row = row,
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            fadeOutSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                            placementSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                        ),
                        onAccept = { onAcceptApp(row.packageName) },
                        onToggleIgnore = { ignored -> onToggleIgnore(row.packageName, ignored) },
                        onManage = { onManage(row.packageName) },
                    )
                }
            }
        }
    }

    if (sheetOpen) {
        PermFilterSheet(
            available = availableGroups,
            selected = activeFilters,
            onToggle = { id, on ->
                groupFilters = if (on) groupFilters + id else groupFilters - id
            },
            onClearAll = { groupFilters = emptySet() },
            onDismiss = { sheetOpen = false },
        )
    }
}

private val PermFilterSaver = listSaver<MutableState<Set<String>>, String>(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toSet()) },
)

// ── Alert strip ────────────────────────────────────────

@Composable
private fun AlertStrip(count: Int, onAcceptAll: () -> Unit) {
    val palette = LocalHolo.current
    if (count == 0) return
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

// ── Tabs ───────────────────────────────────────────────

@Composable
private fun Tabs(
    value: TabId,
    onChange: (TabId) -> Unit,
    userCount: Int,
    systemCount: Int,
    filterActiveCount: Int,
    onOpenFilter: () -> Unit,
) {
    val palette = LocalHolo.current
    Glass(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 14.dp)
            .fillMaxWidth(),
        cornerRadius = 14.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            Spacer(Modifier.width(4.dp))
            FilterTabButton(
                activeCount = filterActiveCount,
                onClick = onOpenFilter,
                palette = palette,
            )
        }
    }
}

@Composable
private fun FilterTabButton(
    activeCount: Int,
    onClick: () -> Unit,
    palette: HoloPalette,
) {
    val active = activeCount > 0
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
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterGlyph(color = if (active) palette.ink else palette.inkMute)
        if (active) {
            Text(
                text = activeCount.toString(),
                fontFamily = Mono,
                fontSize = 10.sp,
                color = palette.inkSoft,
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
    palette: HoloPalette,
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
private fun EmptyState(tab: TabId, filterCount: Int = 0, singleFilterLabel: String? = null) {
    val palette = LocalHolo.current
    val tabWord = if (tab == TabId.User) "installed" else "system"
    val body = when {
        filterCount == 0 -> "No $tabWord apps currently hold any of the watched permissions."
        filterCount == 1 && singleFilterLabel != null ->
            "No $tabWord apps currently hold the \u201C$singleFilterLabel\u201D permission."
        else -> "No $tabWord apps match the selected permissions."
    }
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
                text = body,
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
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .fillMaxWidth()
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
                AppAvatar(label = row.label, packageName = row.packageName)
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
                        WatchToggle(on = !ignored, onChange = { onToggleIgnore(!ignored) })
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
            // Glowing left edge marker — sized off the Glass card via matchParentSize so it
            // doesn't depend on intrinsic-height measurement (FlowRow above mis-reports it).
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
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
}

// ── Permission filter (sheet + glyph) ──────────────────

@Composable
private fun FilterGlyph(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(11.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.3.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.10f, h * 0.18f)
            lineTo(w * 0.90f, h * 0.18f)
            lineTo(w * 0.58f, h * 0.55f)
            lineTo(w * 0.58f, h * 0.90f)
            lineTo(w * 0.42f, h * 0.78f)
            lineTo(w * 0.42f, h * 0.55f)
            close()
        }
        drawPath(path, color = color, style = stroke)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermFilterSheet(
    available: List<PermissionGroup>,
    selected: Set<String>,
    onToggle: (String, Boolean) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalHolo.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val groupedByCategory = remember(available) { available.groupBy { it.category } }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "FILTER \u00B7 BY PERMISSION",
                    color = palette.inkSoft,
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f),
                )
                if (selected.isNotEmpty()) {
                    Text(
                        text = "Clear all",
                        color = palette.inkSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onClearAll)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PermissionCategory.values().forEach { cat ->
                    val groups = groupedByCategory[cat].orEmpty()
                    if (groups.isEmpty()) return@forEach
                    item(key = "head-${cat.name}") {
                        FilterSectionHeader(title = stringResource(cat.displayRes))
                    }
                    item(key = "card-${cat.name}") {
                        Glass(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                groups.forEachIndexed { idx, group ->
                                    if (idx > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                                .height(0.5.dp)
                                                .background(palette.stroke),
                                        )
                                    }
                                    GroupCheckRow(
                                        group = group,
                                        checked = group.id in selected,
                                        onToggle = { on -> onToggle(group.id, on) },
                                    )
                                }
                            }
                        }
                    }
                }
                if (available.isEmpty()) {
                    item {
                        Text(
                            text = "Nothing to filter \u2014 no apps in the current scan hold any watched permissions.",
                            color = palette.inkSoft,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
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
private fun GroupCheckRow(
    group: PermissionGroup,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = group.label,
                color = palette.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            if (group.members.size > 1) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${group.members.size} permissions",
                    color = palette.inkMute,
                    fontFamily = Mono,
                    fontSize = 10.sp,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        FilterCheckBox(checked = checked)
    }
}

@Composable
private fun FilterCheckBox(checked: Boolean) {
    val palette = LocalHolo.current
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (checked) Brush.linearGradient(listOf(palette.accentA, palette.accentC))
                else Brush.linearGradient(
                    listOf(
                        if (palette.isDark) Color.White.copy(alpha = 0.06f) else palette.bgDeep,
                        if (palette.isDark) Color.White.copy(alpha = 0.06f) else palette.bgDeep,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.6.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                )
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.18f, size.height * 0.55f)
                    lineTo(size.width * 0.42f, size.height * 0.78f)
                    lineTo(size.width * 0.85f, size.height * 0.30f)
                }
                drawPath(path, color = palette.bgDeep, style = stroke)
            }
        }
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
            text = "Accept",
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

