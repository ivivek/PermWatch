package com.trogo.permalert

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.trogo.permalert.ui.AppScaffold
import com.trogo.permalert.ui.MainViewModel

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            PermalertTheme {
                Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
                    val state by vm.state.collectAsState()
                    LaunchedEffect(Unit) { vm.refresh() }
                    AppScaffold(
                        state = state,
                        onRescan = { vm.refresh() },
                        onCompleteOnboarding = { vm.completeOnboarding() },
                        onAcceptApp = { pkg -> vm.acceptApp(pkg) },
                        onAcceptAll = { vm.acceptAll() },
                        onToggleIgnore = { pkg, ignored -> vm.toggleIgnore(pkg, ignored) },
                        onManage = { pkg -> openAppDetailsSettings(pkg) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PermalertTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) darkColorScheme() else lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
