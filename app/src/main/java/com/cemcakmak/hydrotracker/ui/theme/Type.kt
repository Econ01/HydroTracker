package com.cemcakmak.hydrotracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import com.cemcakmak.hydrotracker.R

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
            FontVariation.Setting("wght", 700f),
            FontVariation.Setting("wdth", 115f),
            FontVariation.Setting("SOFT", 20f),
            FontVariation.Setting("opsz", 72f)
        )
    )
)

val HydroTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Dmsans,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Dmsans,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = Dmsans,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // Custom: Emphasized style for onboarding banners, hero headers
    displayLargeEmphasized = TextStyle(
        fontFamily = Dmsans,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp
    ),

    headlineLarge = TextStyle(
        fontFamily = Dmsans,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Dmsans,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Dmsans,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    titleLarge = TextStyle(
        fontFamily = Dmsans,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Dmsans,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Dmsans,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = Dmsans,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Dmsans,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Dmsans,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    labelLarge = TextStyle(
        fontFamily = Dmsans,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Dmsans,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Dmsans,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
