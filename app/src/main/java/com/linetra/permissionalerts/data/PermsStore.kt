package com.linetra.permissionalerts.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.permsDataStore: DataStore<Preferences> by preferencesDataStore(name = "perms")

/**
 * Persists the user's "accepted" baseline (pkg -> set of permission names the user has approved)
 * and the set of ignored packages. Alerts for a package are: currentGranted - baseline[pkg],
 * skipped entirely for ignored packages.
 */
class PermsStore(context: Context) {

    private val ds = context.applicationContext.permsDataStore

    val onboarded: Flow<Boolean> = ds.data.map { it[KEY_ONBOARDED] ?: false }

    val baseline: Flow<Map<String, Set<String>>> =
        ds.data.map { decodePkgPermMap(it[KEY_BASELINE]) }

    val ignored: Flow<Set<String>> =
        ds.data.map { decodeSet(it[KEY_IGNORED]) }

    /** Manifest names of sensitive permissions the user has explicitly opted out of watching.
     *  Empty by default — first-install behavior watches the full SensitivePermissions catalog. */
    val unwatched: Flow<Set<String>> =
        ds.data.map { decodeSet(it[KEY_UNWATCHED]) }

    suspend fun currentBaseline(): Map<String, Set<String>> = baseline.first()
    suspend fun currentIgnored(): Set<String> = ignored.first()
    suspend fun currentUnwatched(): Set<String> = unwatched.first()
    suspend fun isOnboarded(): Boolean = onboarded.first()

    val lastAlertCount: Flow<Int> = ds.data.map { it[KEY_LAST_ALERT_COUNT] ?: 0 }
    suspend fun currentLastAlertCount(): Int = lastAlertCount.first()
    suspend fun setLastAlertCount(count: Int) {
        ds.edit { it[KEY_LAST_ALERT_COUNT] = count }
    }

    val intervalSeconds: Flow<Long> = ds.data.map { it[KEY_INTERVAL_SECONDS] ?: DEFAULT_INTERVAL_SECONDS }
    suspend fun currentIntervalSeconds(): Long = intervalSeconds.first()
    suspend fun setIntervalSeconds(seconds: Long) {
        ds.edit { it[KEY_INTERVAL_SECONDS] = seconds }
    }

    /** Per-package set of *all* sensitive perms granted at the previous scan. Diffed against the
     *  next scan's grants to produce [PermEvent]s. Distinct from [baseline] — baseline tracks
     *  user-acknowledged grants; snapshot tracks raw OS state. */
    val lastSnapshot: Flow<Map<String, Set<String>>> =
        ds.data.map { decodePkgPermMap(it[KEY_LAST_SNAPSHOT]) }
    suspend fun currentLastSnapshot(): Map<String, Set<String>> = lastSnapshot.first()

    /** Bounded history of permission state changes, newest-last in storage. The Flow re-orders
     *  to newest-first for UI consumption. */
    val events: Flow<List<PermEvent>> =
        ds.data.map { EventLogEncoding.decode(it[KEY_EVENTS]).asReversed() }
    suspend fun currentEvents(): List<PermEvent> = events.first()

    /** Timestamp (ms) of the newest event the user has seen — anything strictly newer is unread.
     *  Bumped to the latest event's ts when the History screen opens. */
    val lastSeenEventTs: Flow<Long> = ds.data.map { it[KEY_LAST_SEEN_EVENT_TS] ?: 0L }
    suspend fun setLastSeenEventTs(ts: Long) {
        ds.edit { it[KEY_LAST_SEEN_EVENT_TS] = ts }
    }

    /**
     * Diff [current] vs the stored snapshot, append any resulting events (capped at
     * [EVENT_LOG_CAP]), and replace the snapshot. Skips event emission on the very first call
     * after install/update — there's no prior snapshot to diff against, and we don't want to
     * synthesize a flood of GRANTED events for every existing grant. [labelLookup] /
     * [systemLookup] supply display data for [PermEvent.label] / [PermEvent.isSystem].
     */
    suspend fun recordEventsForScan(
        current: Map<String, Set<String>>,
        labelLookup: Map<String, String>,
        systemLookup: Map<String, Boolean>,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        ds.edit {
            val prevRaw = it[KEY_LAST_SNAPSHOT]
            it[KEY_LAST_SNAPSHOT] = encodePkgPermMap(current)
            if (prevRaw.isNullOrEmpty()) return@edit
            val prev = decodePkgPermMap(prevRaw)
            val newEvents = EventDiff.diff(prev, current, labelLookup, systemLookup, nowMillis)
            if (newEvents.isEmpty()) return@edit
            val existing = EventLogEncoding.decode(it[KEY_EVENTS])
            val combined = (existing + newEvents).let { all ->
                if (all.size <= EVENT_LOG_CAP) all else all.subList(all.size - EVENT_LOG_CAP, all.size)
            }
            it[KEY_EVENTS] = EventLogEncoding.encode(combined)
        }
    }

    suspend fun setOnboarded(value: Boolean) {
        ds.edit { it[KEY_ONBOARDED] = value }
    }

    suspend fun replaceBaseline(newBaseline: Map<String, Set<String>>) {
        ds.edit { it[KEY_BASELINE] = encodePkgPermMap(newBaseline) }
    }

    suspend fun acceptCurrentAsBaseline(current: Map<String, Set<String>>) {
        replaceBaseline(current)
    }

    suspend fun acceptForPackage(pkg: String, currentPerms: Set<String>) {
        ds.edit {
            val map = decodePkgPermMap(it[KEY_BASELINE]).toMutableMap()
            map[pkg] = currentPerms
            it[KEY_BASELINE] = encodePkgPermMap(map)
        }
    }

    /**
     * Drop revoked perms and uninstalled packages from the baseline. Run after each scan so a
     * later re-grant counts as new (the perm has left the baseline).
     */
    suspend fun pruneBaselineToCurrent(current: Map<String, Set<String>>) {
        ds.edit {
            val pruned = decodePkgPermMap(it[KEY_BASELINE])
                .mapNotNull { (pkg, perms) ->
                    val now = current[pkg] ?: return@mapNotNull null
                    val intersected = perms intersect now
                    if (intersected.isEmpty()) null else pkg to intersected
                }
                .toMap()
            it[KEY_BASELINE] = encodePkgPermMap(pruned)
        }
    }

    suspend fun setIgnored(pkg: String, ignored: Boolean) {
        ds.edit {
            val set = decodeSet(it[KEY_IGNORED]).toMutableSet()
            if (ignored) set += pkg else set -= pkg
            it[KEY_IGNORED] = encodeSet(set)
        }
    }

    suspend fun setUnwatched(perm: String, unwatched: Boolean) {
        ds.edit {
            val set = decodeSet(it[KEY_UNWATCHED]).toMutableSet()
            if (unwatched) set += perm else set -= perm
            it[KEY_UNWATCHED] = encodeSet(set)
        }
    }

    /** For each package whose current grants include `perm`, add `perm` to its baseline entry.
     *  Used when re-enabling watch for a permission so existing grants don't fire as new. */
    suspend fun mergeIntoBaseline(perm: String, current: Map<String, Set<String>>) {
        ds.edit {
            val map = decodePkgPermMap(it[KEY_BASELINE]).toMutableMap()
            for ((pkg, perms) in current) {
                if (perm in perms) {
                    map[pkg] = (map[pkg] ?: emptySet()) + perm
                }
            }
            it[KEY_BASELINE] = encodePkgPermMap(map)
        }
    }

    companion object {
        private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
        private val KEY_BASELINE = stringPreferencesKey("baseline_v1")
        private val KEY_IGNORED = stringPreferencesKey("ignored_v1")
        private val KEY_UNWATCHED = stringPreferencesKey("unwatched_v1")
        private val KEY_LAST_ALERT_COUNT = intPreferencesKey("last_alert_count")
        private val KEY_INTERVAL_SECONDS = longPreferencesKey("interval_seconds")
        private val KEY_LAST_SNAPSHOT = stringPreferencesKey("last_snapshot_v1")
        private val KEY_EVENTS = stringPreferencesKey("events_v1")
        private val KEY_LAST_SEEN_EVENT_TS = longPreferencesKey("last_seen_event_ts")

        const val DEFAULT_INTERVAL_SECONDS: Long = 900L
        const val EVENT_LOG_CAP: Int = 500

        // Encoding: pkg|perm1,perm2\npkg2|perm3  — newlines/pipes/commas are not legal in permission names or package names
        fun encodePkgPermMap(map: Map<String, Set<String>>): String =
            map.entries.joinToString("\n") { (pkg, perms) ->
                "$pkg|${perms.joinToString(",")}"
            }

        fun decodePkgPermMap(s: String?): Map<String, Set<String>> {
            if (s.isNullOrEmpty()) return emptyMap()
            return s.lineSequence().mapNotNull { line ->
                val idx = line.indexOf('|')
                if (idx <= 0) return@mapNotNull null
                val pkg = line.substring(0, idx)
                val perms = line.substring(idx + 1)
                    .split(',')
                    .filter { it.isNotEmpty() }
                    .toSet()
                pkg to perms
            }.toMap()
        }

        fun encodeSet(set: Set<String>): String = set.joinToString("\n")
        fun decodeSet(s: String?): Set<String> =
            if (s.isNullOrEmpty()) emptySet()
            else s.lineSequence().filter { it.isNotEmpty() }.toSet()
    }
}
