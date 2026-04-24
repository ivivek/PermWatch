package com.linetra.permwatch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.linetra.permwatch.R

@OptIn(ExperimentalTextApi::class)
private val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_var, FontWeight.Normal,  variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.space_grotesk_var, FontWeight.Medium,  variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.space_grotesk_var, FontWeight.SemiBold,variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.space_grotesk_var, FontWeight.Bold,    variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium,  FontWeight.Medium),
    Font(R.font.ibm_plex_mono_bold,    FontWeight.Bold),
)

val Display = SpaceGrotesk
val Mono = PlexMono

val PermWatchTypography = Typography(
    displayLarge   = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium,  fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium,  fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge     = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium,  fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    titleMedium    = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium,  fontSize = 16.sp, lineHeight = 20.sp),
    titleSmall     = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge      = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal,  fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium     = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal,  fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall      = TextStyle(fontFamily = Mono,    fontWeight = FontWeight.Normal,  fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
    labelLarge     = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium    = TextStyle(fontFamily = Mono,    fontWeight = FontWeight.Medium,  fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.5.sp),
    labelSmall     = TextStyle(fontFamily = Mono,    fontWeight = FontWeight.Medium,  fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 2.sp),
)
