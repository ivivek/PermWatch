package com.linetra.permissionalerts.data

enum class EventKind { GRANTED, REVOKED }

data class PermEvent(
    val tsMillis: Long,
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val perm: String,
    val kind: EventKind,
)

object EventDiff {

    /**
     * Diff a previous per-package grant snapshot against the current one and emit a [PermEvent]
     * for every (pkg, perm) tuple that flipped. Both maps are pkg → set-of-sensitive-perms; we
     * intentionally diff the unfiltered sensitive set (not the user's watched subset) so that
     * toggling a permission's watch state in Settings doesn't shape the historical record.
     *
     * [labelLookup] resolves a display label for the pkg if it's still installed; otherwise the
     * package name is used as the label fallback (uninstalled apps disappear from [labelLookup]).
     */
    fun diff(
        prev: Map<String, Set<String>>,
        current: Map<String, Set<String>>,
        labelLookup: Map<String, String>,
        systemLookup: Map<String, Boolean>,
        nowMillis: Long,
    ): List<PermEvent> {
        val out = mutableListOf<PermEvent>()
        val keys = prev.keys + current.keys
        for (pkg in keys) {
            val before = prev[pkg].orEmpty()
            val after = current[pkg].orEmpty()
            val granted = after - before
            val revoked = before - after
            if (granted.isEmpty() && revoked.isEmpty()) continue
            val label = labelLookup[pkg] ?: pkg
            val isSystem = systemLookup[pkg] ?: false
            granted.forEach { perm ->
                out += PermEvent(nowMillis, pkg, label, isSystem, perm, EventKind.GRANTED)
            }
            revoked.forEach { perm ->
                out += PermEvent(nowMillis, pkg, label, isSystem, perm, EventKind.REVOKED)
            }
        }
        return out
    }
}

internal object EventLogEncoding {

    // ts|kind|pkg|isSystem|perm|label  — one record per line. Label is sanitized to strip
    // separators since app labels are user-controlled (third-party packages can do anything);
    // pkg/perm names are restricted to ASCII identifiers and the ts/kind/bool fields are fixed.
    fun encode(events: List<PermEvent>): String =
        events.joinToString("\n") { e ->
            buildString {
                append(e.tsMillis); append('|')
                append(e.kind.name); append('|')
                append(e.packageName); append('|')
                append(if (e.isSystem) '1' else '0'); append('|')
                append(e.perm); append('|')
                append(sanitize(e.label))
            }
        }

    fun decode(s: String?): List<PermEvent> {
        if (s.isNullOrEmpty()) return emptyList()
        return s.lineSequence().mapNotNull { line ->
            if (line.isEmpty()) return@mapNotNull null
            val parts = line.split('|', limit = 6)
            if (parts.size < 6) return@mapNotNull null
            val ts = parts[0].toLongOrNull() ?: return@mapNotNull null
            val kind = runCatching { EventKind.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
            PermEvent(
                tsMillis = ts,
                packageName = parts[2],
                label = parts[5],
                isSystem = parts[3] == "1",
                perm = parts[4],
                kind = kind,
            )
        }.toList()
    }

    private fun sanitize(s: String): String =
        s.replace('|', ' ').replace('\n', ' ').replace('\r', ' ')
}
