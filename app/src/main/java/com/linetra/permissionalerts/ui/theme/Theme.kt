package com.linetra.permissionalerts.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity

@Composable
fun PermissionAlertsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) HoloDark else HoloLight

    val scheme = if (darkTheme) {
        darkColorScheme(
            background    = palette.bg,
            surface       = palette.bg,
            surfaceVariant= palette.bgDeep,
            onBackground  = palette.ink,
            onSurface     = palette.ink,
            onSurfaceVariant = palette.inkSoft,
            primary       = palette.accentC,
            onPrimary     = palette.bgDeep,
            secondary     = palette.accentA,
            tertiary      = palette.accentB,
            error         = palette.warn,
            outline       = palette.stroke,
        )
    } else {
        lightColorScheme(
            background    = palette.bg,
            surface       = palette.bg,
            surfaceVariant= palette.bgDeep,
            onBackground  = palette.ink,
            onSurface     = palette.ink,
            onSurfaceVariant = palette.inkSoft,
            primary       = palette.accentA,
            onPrimary     = Color.White,
            secondary     = palette.accentC,
            tertiary      = palette.accentB,
            error         = palette.warn,
            outline       = palette.stroke,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalHolo provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PermissionAlertsTypography,
            content = content,
        )
    }
}
