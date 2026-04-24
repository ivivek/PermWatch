package com.linetra.permwatch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

    suspend fun currentBaseline(): Map<String, Set<String>> = baseline.first()
    suspend fun currentIgnored(): Set<String> = ignored.first()
    suspend fun isOnboarded(): Boolean = onboarded.first()

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

    suspend fun setIgnored(pkg: String, ignored: Boolean) {
        ds.edit {
            val set = decodeSet(it[KEY_IGNORED]).toMutableSet()
            if (ignored) set += pkg else set -= pkg
            it[KEY_IGNORED] = encodeSet(set)
        }
    }

    companion object {
        private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
        private val KEY_BASELINE = stringPreferencesKey("baseline_v1")
        private val KEY_IGNORED = stringPreferencesKey("ignored_v1")

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
