package com.linetra.permissionalerts.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linetra.permissionalerts.data.EventKind
import com.linetra.permissionalerts.data.PermEvent
import com.linetra.permissionalerts.data.SensitivePermissions
import com.linetra.permissionalerts.ui.atoms.Glass
import com.linetra.permissionalerts.ui.atoms.Iris
import com.linetra.permissionalerts.ui.theme.LocalHolo
import com.linetra.permissionalerts.ui.theme.Mono
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun History(
    events: List<PermEvent>,
    onManage: (String) -> Unit,
    onBack: () -> Unit,
) {
    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navBars = WindowInsets.navigationBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBars.calculateTopPadding()),
    ) {
        HistoryHeader(onBack = onBack)
        if (events.isEmpty()) {
            EmptyHistory(modifier = Modifier.padding(bottom = navBars.calculateBottomPadding()))
        } else {
            EventList(
                events = events,
                onManage = onManage,
                bottomInset = navBars.calculateBottomPadding(),
            )
        }
    }
}

@Composable
private fun EventList(
    events: List<PermEvent>,
    onManage: (String) -> Unit,
    bottomInset: androidx.compose.ui.unit.Dp,
) {
    val grouped = remember(events) { groupByDay(events) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp + bottomInset),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        grouped.forEach { (header, dayEvents) ->
            item(key = "head-$header") {
                DayHeader(label = header)
            }
            item(key = "card-$header") {
                Glass(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        dayEvents.forEachIndexed { idx, e ->
                            if (idx > 0) Divider()
                            EventRow(
                                event = e,
                                time = timeFormat.format(Date(e.tsMillis)),
                                onClick = { onManage(e.packageName) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(label: String) {
    val palette = LocalHolo.current
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
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
            text = label.uppercase(),
            color = palette.inkSoft,
            fontFamily = Mono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun EventRow(event: PermEvent, time: String, onClick: () -> Unit) {
    val palette = LocalHolo.current
    val granted = event.kind == EventKind.GRANTED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppAvatar(label = event.label, packageName = event.packageName, size = 36.dp)
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = event.label,
                    color = palette.ink,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = time,
                    color = palette.inkMute,
                    fontFamily = Mono,
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KindBadge(granted = granted)
                Text(
                    text = SensitivePermissions.labelFor(event.perm) +
                        if (event.isSystem) " · system" else "",
                    color = palette.inkSoft,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun KindBadge(granted: Boolean) {
    val palette = LocalHolo.current
    val bg = if (granted) {
        Brush.linearGradient(
            listOf(
                palette.accentA.copy(alpha = 0.20f),
                palette.accentB.copy(alpha = 0.20f),
            ),
        )
    } else {
        Brush.linearGradient(
            listOf(
                if (palette.isDark) Color.White.copy(alpha = 0.06f) else palette.bgDeep,
                if (palette.isDark) Color.White.copy(alpha = 0.06f) else palette.bgDeep,
            ),
        )
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = if (granted) "GRANTED" else "REVOKED",
            color = if (granted) palette.ink else palette.inkSoft,
            fontFamily = Mono,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun Divider() {
    val palette = LocalHolo.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.5.dp)
            .background(palette.stroke),
    )
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    val palette = LocalHolo.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Iris(size = 54.dp, speedMillis = 18_000)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No events yet.",
                color = palette.ink,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Permission grants and revocations will be logged here as scans run.",
                color = palette.inkSoft,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HistoryHeader(onBack: () -> Unit) {
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
            text = "HISTORY",
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
        Canvas(modifier = Modifier.size(16.dp)) {
            val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
            val path = Path().apply {
                moveTo(size.width * 0.90f, size.height * 0.50f)
                lineTo(size.width * 0.20f, size.height * 0.50f)
                moveTo(size.width * 0.40f, size.height * 0.30f)
                lineTo(size.width * 0.20f, size.height * 0.50f)
                lineTo(size.width * 0.40f, size.height * 0.70f)
            }
            drawPath(path, color = color, style = stroke)
        }
    }
}

private fun groupByDay(events: List<PermEvent>): List<Pair<String, List<PermEvent>>> {
    val today = Calendar.getInstance().toDayKey()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.toDayKey()
    val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
    val cal = Calendar.getInstance()
    val out = mutableListOf<Pair<String, MutableList<PermEvent>>>()
    var currentKey: Int? = null
    for (e in events) {
        cal.timeInMillis = e.tsMillis
        val key = cal.toDayKey()
        if (key != currentKey) {
            val label = when (key) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> dateFormat.format(Date(e.tsMillis))
            }
            out += label to mutableListOf()
            currentKey = key
        }
        out.last().second += e
    }
    return out
}

private fun Calendar.toDayKey(): Int =
    get(Calendar.YEAR) * 1000 + get(Calendar.DAY_OF_YEAR)
