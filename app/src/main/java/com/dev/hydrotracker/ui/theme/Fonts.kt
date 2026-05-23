package com.dev.hydrotracker.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.dev.hydrotracker.R

@OptIn(ExperimentalTextApi::class)
val RobotoFlexWelcome = FontFamily(
    Font(
        resId = R.font.roboto_flex_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 700f),  // Emphasis via boldness
            FontVariation.Setting("wdth", 120f),  // Wider for presence
            FontVariation.Setting("SOFT", 20f),   // Gentle softening
            FontVariation.Setting("opsz", 72f),   // Optimized for large display
        )
    )
)
