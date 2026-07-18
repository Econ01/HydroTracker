package com.cemcakmak.hydrotracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.AppFont

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex = FontFamily(
    listOf(
        FontWeight.Normal, // 400
        FontWeight.Medium, // 500
        FontWeight.Bold,   // 700
    ).map { weight ->
        Font(
            resId = R.font.google_sans_flex,
            weight = weight,
            variationSettings = FontVariation.Settings(
                weight = weight,
                style = FontStyle.Normal,
                FontVariation.Setting("ROND", 100f)
            )
        )
    }
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
        // Display
        displayLarge = defaults.displayLarge.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal
        ),
        displayMedium = defaults.displayMedium.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal
        ),
        displaySmall = defaults.displaySmall.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal
        ),

        // Headline
        headlineLarge = defaults.headlineLarge.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal
        ),
        headlineMedium = defaults.headlineMedium.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal
        ),
        headlineSmall = defaults.headlineSmall.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal
        ),

        // Title
        titleLarge = defaults.titleLarge.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal
        ),
        titleMedium = defaults.titleMedium.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),
        titleSmall = defaults.titleSmall.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),

        // Body
        bodyLarge = defaults.bodyLarge.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal
        ),
        bodyMedium = defaults.bodyMedium.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal
        ),
        bodySmall = defaults.bodySmall.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal
        ),

        // Label
        labelLarge = defaults.labelLarge.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),
        labelMedium = defaults.labelMedium.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),
        labelSmall = defaults.labelSmall.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),

        // Display Emphasized
        displayLargeEmphasized = defaults.displayLargeEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),
        displayMediumEmphasized = defaults.displayMediumEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),
        displaySmallEmphasized = defaults.displaySmallEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),

        // Headline Emphasized
        headlineLargeEmphasized = defaults.headlineLargeEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),
        headlineMediumEmphasized = defaults.headlineMediumEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),
        headlineSmallEmphasized = defaults.headlineSmallEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),

        // Title Emphasized
        titleLargeEmphasized = defaults.titleLargeEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),
        titleMediumEmphasized = defaults.titleMediumEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold
        ),
        titleSmallEmphasized = defaults.titleSmallEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold
        ),

        // Body Emphasized
        bodyLargeEmphasized = defaults.bodyLargeEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),
        bodyMediumEmphasized = defaults.bodyMediumEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),
        bodySmallEmphasized = defaults.bodySmallEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),

        // Label Emphasized
        labelLargeEmphasized = defaults.labelLargeEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold
        ),
        labelMediumEmphasized = defaults.labelMediumEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold
        ),
        labelSmallEmphasized = defaults.labelSmallEmphasized.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold
        ),
    )
}
