package com.cemcakmak.hydrotracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.cemcakmak.hydrotracker.R

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("ROND", 100f),
        )
    )
)

val HydroTypography = Typography().let { defaults ->
    Typography(
        displayLarge = defaults.displayLarge.copy(fontFamily = GoogleSansFlex),
        displayMedium = defaults.displayMedium.copy(fontFamily = GoogleSansFlex),
        displaySmall = defaults.displaySmall.copy(fontFamily = GoogleSansFlex),

        headlineLarge = defaults.headlineLarge.copy(fontFamily = GoogleSansFlex),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = GoogleSansFlex),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = GoogleSansFlex),

        titleLarge = defaults.titleLarge.copy(fontFamily = GoogleSansFlex),
        titleMedium = defaults.titleMedium.copy(fontFamily = GoogleSansFlex),
        titleSmall = defaults.titleSmall.copy(fontFamily = GoogleSansFlex),

        bodyLarge = defaults.bodyLarge.copy(fontFamily = GoogleSansFlex),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = GoogleSansFlex),
        bodySmall = defaults.bodySmall.copy(fontFamily = GoogleSansFlex),

        labelLarge = defaults.labelLarge.copy(fontFamily = GoogleSansFlex),
        labelMedium = defaults.labelMedium.copy(fontFamily = GoogleSansFlex),
        labelSmall = defaults.labelSmall.copy(fontFamily = GoogleSansFlex),

        displayLargeEmphasized = defaults.displayLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        displayMediumEmphasized = defaults.displayMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        displaySmallEmphasized = defaults.displaySmallEmphasized.copy(fontFamily = GoogleSansFlex),

        headlineLargeEmphasized = defaults.headlineLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        headlineMediumEmphasized = defaults.headlineMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        headlineSmallEmphasized = defaults.headlineSmallEmphasized.copy(fontFamily = GoogleSansFlex),

        titleLargeEmphasized = defaults.titleLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        titleMediumEmphasized = defaults.titleMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        titleSmallEmphasized = defaults.titleSmallEmphasized.copy(fontFamily = GoogleSansFlex),

        bodyLargeEmphasized = defaults.bodyLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        bodyMediumEmphasized = defaults.bodyMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        bodySmallEmphasized = defaults.bodySmallEmphasized.copy(fontFamily = GoogleSansFlex),

        labelLargeEmphasized = defaults.labelLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        labelMediumEmphasized = defaults.labelMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        labelSmallEmphasized = defaults.labelSmallEmphasized.copy(fontFamily = GoogleSansFlex),
    )
}
