package com.linetra.permwatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linetra.permwatch.R
import com.linetra.permwatch.data.SensitivePermissions

private enum class Tab(val label: String) { User("User"), System("System") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    state: UiState,
    onRescan: () -> Unit,
    onAcceptApp: (String) -> Unit,
    onAcceptAll: () -> Unit,
    onToggleIgnore: (String, Boolean) -> Unit,
    onManage: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onRescan) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_rescan))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingView(padding)
        } else {
            AppListWithTabs(
                padding = padding,
                state = state,
                onAcceptApp = onAcceptApp,
                onAcceptAll = onAcceptAll,
                onToggleIgnore = onToggleIgnore,
                onManage = onManage,
            )
        }
    }
}

@Composable
private fun LoadingView(padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AppListWithTabs(
    padding: PaddingValues,
    state: UiState,
    onAcceptApp: (String) -> Unit,
    onAcceptAll: () -> Unit,
    onToggleIgnore: (String, Boolean) -> Unit,
    onManage: (String) -> Unit,
) {
    val userApps = remember(state.rows) { state.rows.filter { !it.isSystem } }
    val systemApps = remember(state.rows) { state.rows.filter { it.isSystem } }
    var selected by rememberSaveable { mutableStateOf(Tab.User) }
    val visible = if (selected == Tab.User) userApps else systemApps

    Column(Modifier.fillMaxSize().padding(padding)) {
        HeaderBar(alertCount = state.alertCount, total = state.rows.size, onAcceptAll = onAcceptAll)
        HorizontalDivider()
        TabRow(selectedTabIndex = selected.ordinal) {
            Tab.entries.forEachIndexed { index, tab ->
                val count = if (tab == Tab.User) userApps.size else systemApps.size
                Tab(
                    selected = selected.ordinal == index,
                    onClick = { selected = tab },
                    text = { Text("${tab.label} ($count)") },
                )
            }
        }
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No ${selected.label.lowercase()} apps with sensitive permissions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible, key = { it.packageName }) { row ->
                    AppCard(
                        row = row,
                        onAccept = { onAcceptApp(row.packageName) },
                        onToggleIgnore = { ignored -> onToggleIgnore(row.packageName, ignored) },
                        onManage = { onManage(row.packageName) },
                        showActions = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBar(alertCount: Int, total: Int, onAcceptAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (alertCount == 0) "No new alerts" else "$alertCount alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (alertCount == 0) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.error,
            )
            Text(
                "$total apps with sensitive permissions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (alertCount > 0) {
            TextButton(onClick = onAcceptAll) { Text("Accept all") }
        }
    }
}

@Composable
private fun AppCard(
    row: AppRow,
    onAccept: () -> Unit,
    onToggleIgnore: (Boolean) -> Unit,
    onManage: () -> Unit,
    showActions: Boolean,
) {
    val cardColor = when {
        row.isIgnored -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        row.hasAlert -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        else -> CardDefaults.cardColors()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        row.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        row.packageName + if (row.isSystem) " · system" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (showActions) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (row.isIgnored) "Ignored" else "Watch",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.size(4.dp))
                        Switch(
                            checked = !row.isIgnored,
                            onCheckedChange = { watched -> onToggleIgnore(!watched) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            PermChips(granted = row.granted, newPerms = row.newPerms)
            if (showActions) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onManage) { Text("Manage") }
                    if (row.hasAlert) {
                        Spacer(Modifier.size(4.dp))
                        OutlinedButton(onClick = onAccept) { Text("Accept") }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PermChips(granted: Set<String>, newPerms: Set<String>) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        granted.sorted().forEach { perm ->
            val isNew = perm in newPerms
            AssistChip(
                onClick = {},
                label = { Text(SensitivePermissions.labelFor(perm)) },
                colors = if (isNew)
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        labelColor = Color.White,
                    )
                else AssistChipDefaults.assistChipColors(),
            )
        }
    }
}
