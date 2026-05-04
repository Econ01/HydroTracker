package com.cemcakmak.hydrotracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.AppFont

// Dmsans as primary font (system will auto-fallback if needed)
@OptIn(ExperimentalTextApi::class)
val RobotoFlexVariable = FontFamily(
    Font(
        resId = R.font.roboto_flex_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 400f),
            FontVariation.Setting("wdth", 100f),
            FontVariation.Setting("opsz", 32f)
        )
    )
)

// Emphasized variant for hero titles / onboarding
@OptIn(ExperimentalTextApi::class)
val RobotoFlexEmphasized = FontFamily(
    Font(
        resId = R.font.roboto_flex_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 700f),
            FontVariation.Setting("wdth", 115f),
            FontVariation.Setting("SOFT", 20f),
            FontVariation.Setting("opsz", 72f)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val Dmsans = FontFamily(
    Font(
        resId = R.font.dmsans_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 400f), // Standard weight for body
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val JetbrainsMono = FontFamily(
    Font(
        resId = R.font.jetbrainmonovar,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 400f),
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex = FontFamily(
    Font(
        resId = R.font.googlesans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 400f),
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val Outfit = FontFamily(
    Font(
        resId = R.font.outfit_var,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 400f),
        )
    )
)

fun getTypography(appFont: AppFont): Typography {
    val fontFamily = when (appFont) {
        AppFont.ROBOTO -> RobotoFlexVariable
        AppFont.DMSANS -> Dmsans
        AppFont.JETBRAINS_MONO -> JetbrainsMono
        AppFont.GOOGLE_SANS -> GoogleSansFlex
        AppFont.OUTFIT -> Outfit
    }

    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),

        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),

        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),

        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),

        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

val HydroTypography = getTypography(AppFont.ROBOTO)
