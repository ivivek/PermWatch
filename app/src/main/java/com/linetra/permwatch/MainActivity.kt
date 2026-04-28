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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.linetra.permwatch.ui.AppScaffold
import com.linetra.permwatch.ui.Intro
import com.linetra.permwatch.ui.MainViewModel
import com.linetra.permwatch.ui.Settings
import com.linetra.permwatch.ui.theme.LocalHolo
import com.linetra.permwatch.ui.theme.PermWatchTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

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
        vm.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                            if (settingsOpen) {
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
                            } else {
                                AppScaffold(
                                    state = state,
                                    onRescan = { vm.refresh() },
                                    onAcceptApp = { pkg -> vm.acceptApp(pkg) },
                                    onAcceptAll = { vm.acceptAll() },
                                    onToggleIgnore = { pkg, ignored -> vm.toggleIgnore(pkg, ignored) },
                                    onManage = { pkg -> openAppDetailsSettings(pkg) },
                                    onOpenSettings = { settingsOpen = true },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
