package com.trogo.permalert.data

/**
 * Flagged entry for the alert notification: app `label`/`packageName` has `newPerms` that are
 * granted now but were NOT in the user's baseline.
 */
data class FlaggedApp(
    val packageName: String,
    val label: String,
    val newPerms: Set<String>,
)

object AlertDiff {

    fun compute(
        current: List<InstalledAppPerms>,
        baseline: Map<String, Set<String>>,
        ignored: Set<String>,
    ): List<FlaggedApp> {
        val flagged = mutableListOf<FlaggedApp>()
        for (app in current) {
            if (app.packageName in ignored) continue
            if (app.grantedSensitive.isEmpty()) continue
            val known = baseline[app.packageName] ?: emptySet()
            val newPerms = app.grantedSensitive - known
            if (newPerms.isNotEmpty()) {
                flagged += FlaggedApp(app.packageName, app.label, newPerms)
            }
        }
        return flagged.sortedBy { it.label.lowercase() }
    }

    fun currentGrantsMap(apps: List<InstalledAppPerms>): Map<String, Set<String>> =
        apps.filter { it.grantedSensitive.isNotEmpty() }
            .associate { it.packageName to it.grantedSensitive }
}
