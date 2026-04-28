package com.linetra.permwatch

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.linetra.permwatch.ui.AppScaffold
import com.linetra.permwatch.ui.History
import com.linetra.permwatch.ui.Intro
import com.linetra.permwatch.ui.MainViewModel
import com.linetra.permwatch.ui.Settings
import com.linetra.permwatch.ui.theme.LocalHolo
import com.linetra.permwatch.ui.theme.PermWatchTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    /** Set to true when an alert-tap intent is consumed, so the immediately-following
     *  [onResume] doesn't kick off a redundant second scan in parallel with the one started
     *  by [MainViewModel.onAlertTap]. Two concurrent scans would let the cheaper one finish
     *  first, briefly painting the list at the user's pre-tap scroll position before the
     *  alert-tap scan completes and the scroll signal lands. */
    private var skipNextResumeRefresh = false

    private val postNotifLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignore */ }

    private fun openAppDetailsSettings(packageName: String) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun activateOnboarding() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        vm.activate()
    }

    override fun onResume() {
        super.onResume()
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false
        } else {
            vm.refresh()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeAlertIntent(intent)
    }

    /** If [source] carries the alert-tap flag, kick off the scan-and-emit path on the VM and
     *  swallow the next [onResume] refresh so we don't run two concurrent scans. The extra is
     *  stripped so a config change doesn't re-fire the path on the same intent. */
    private fun consumeAlertIntent(source: Intent?) {
        if (source?.getBooleanExtra(EXTRA_FROM_ALERT, false) == true) {
            source.removeExtra(EXTRA_FROM_ALERT)
            skipNextResumeRefresh = true
            vm.onAlertTap()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) consumeAlertIntent(intent)
        enableEdgeToEdge()
        setContent {
            PermWatchTheme {
                val onboarded by vm.onboarded.collectAsState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalHolo.current.bg),
                ) {
                    when (onboarded) {
                        null -> Unit  // splash — palette bg only, prevents intro/scaffold flash
                        false -> Intro(onActivate = ::activateOnboarding)
                        true -> {
                            val state by vm.state.collectAsState()
                            var settingsOpen by rememberSaveable { mutableStateOf(false) }
                            var historyOpen by rememberSaveable { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                vm.scrollToAlert.collect {
                                    settingsOpen = false
                                    historyOpen = false
                                }
                            }
                            when {
                                settingsOpen -> {
                                    val unwatched by vm.unwatched.collectAsState()
                                    val intervalSeconds by vm.intervalSeconds.collectAsState()
                                    val ignoredApps by vm.ignoredApps.collectAsState()
                                    BackHandler { settingsOpen = false }
                                    Settings(
                                        unwatched = unwatched,
                                        intervalSeconds = intervalSeconds,
                                        ignoredApps = ignoredApps,
                                        onSetWatched = { perm, watched -> vm.setWatched(perm, watched) },
                                        onSetInterval = { seconds -> vm.setIntervalSeconds(seconds) },
                                        onSetIgnored = { pkg, ignored -> vm.toggleIgnore(pkg, ignored) },
                                        onBack = { settingsOpen = false },
                                    )
                                }
                                historyOpen -> {
                                    val events by vm.events.collectAsState()
                                    LaunchedEffect(Unit) { vm.markEventsRead() }
                                    BackHandler { historyOpen = false }
                                    History(
                                        events = events,
                                        onManage = { pkg -> openAppDetailsSettings(pkg) },
                                        onBack = { historyOpen = false },
                                    )
                                }
                                else -> {
                                    val unreadCount by vm.unreadEventCount.collectAsState()
                                    AppScaffold(
                                        state = state,
                                        onRescan = { vm.refresh() },
                                        onAcceptApp = { pkg -> vm.acceptApp(pkg) },
                                        onAcceptAll = { vm.acceptAll() },
                                        onToggleIgnore = { pkg, ignored -> vm.toggleIgnore(pkg, ignored) },
                                        onManage = { pkg -> openAppDetailsSettings(pkg) },
                                        onOpenSettings = { settingsOpen = true },
                                        onOpenHistory = { historyOpen = true },
                                        unreadEventCount = unreadCount,
                                        scrollToAlert = vm.scrollToAlert,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_FROM_ALERT = "from_alert"
    }
}
