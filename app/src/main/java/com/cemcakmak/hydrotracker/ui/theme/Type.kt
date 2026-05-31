package com.cemcakmak.hydrotracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.AppFont

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("ROND", 100f),
        )
    )
)

val Outfit = FontFamily(Font(R.font.outfit_variable))
val DmSans = FontFamily(Font(R.font.dm_sans))
val JetBrainsMono = FontFamily(Font(R.font.jetbrains_mono))

fun fontFamilyFor(appFont: AppFont): FontFamily = when (appFont) {
    AppFont.GOOGLE_SANS_FLEX -> GoogleSansFlex
    AppFont.SYSTEM -> FontFamily.Default
    AppFont.OUTFIT -> Outfit
    AppFont.DM_SANS -> DmSans
    AppFont.JETBRAINS_MONO -> JetBrainsMono
}

fun hydroTypography(fontFamily: FontFamily): Typography = Typography().let { defaults ->
    Typography(
        displayLarge = defaults.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = defaults.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = defaults.displaySmall.copy(fontFamily = fontFamily),

        headlineLarge = defaults.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = fontFamily),

        titleLarge = defaults.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = defaults.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = defaults.titleSmall.copy(fontFamily = fontFamily),

        bodyLarge = defaults.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = defaults.bodySmall.copy(fontFamily = fontFamily),

        labelLarge = defaults.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = defaults.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = defaults.labelSmall.copy(fontFamily = fontFamily),

        displayLargeEmphasized = defaults.displayLargeEmphasized.copy(fontFamily = fontFamily),
        displayMediumEmphasized = defaults.displayMediumEmphasized.copy(fontFamily = fontFamily),
        displaySmallEmphasized = defaults.displaySmallEmphasized.copy(fontFamily = fontFamily),

        headlineLargeEmphasized = defaults.headlineLargeEmphasized.copy(fontFamily = fontFamily),
        headlineMediumEmphasized = defaults.headlineMediumEmphasized.copy(fontFamily = fontFamily),
        headlineSmallEmphasized = defaults.headlineSmallEmphasized.copy(fontFamily = fontFamily),

        titleLargeEmphasized = defaults.titleLargeEmphasized.copy(fontFamily = fontFamily),
        titleMediumEmphasized = defaults.titleMediumEmphasized.copy(fontFamily = fontFamily),
        titleSmallEmphasized = defaults.titleSmallEmphasized.copy(fontFamily = fontFamily),

        bodyLargeEmphasized = defaults.bodyLargeEmphasized.copy(fontFamily = fontFamily),
        bodyMediumEmphasized = defaults.bodyMediumEmphasized.copy(fontFamily = fontFamily),
        bodySmallEmphasized = defaults.bodySmallEmphasized.copy(fontFamily = fontFamily),

        labelLargeEmphasized = defaults.labelLargeEmphasized.copy(fontFamily = fontFamily),
        labelMediumEmphasized = defaults.labelMediumEmphasized.copy(fontFamily = fontFamily),
        labelSmallEmphasized = defaults.labelSmallEmphasized.copy(fontFamily = fontFamily),
    )
}
