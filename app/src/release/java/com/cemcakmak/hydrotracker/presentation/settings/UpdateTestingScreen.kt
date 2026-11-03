package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.runtime.Composable
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.update.UpdateRepository

/** Release placeholder: the real Update Testing screen exists only in debug source sets. */
@Composable
fun UpdateTestingScreen(
    @Suppress("UNUSED_PARAMETER") updateRepository: UpdateRepository,
    @Suppress("UNUSED_PARAMETER") themePreferences: ThemePreferences = ThemePreferences(),
    @Suppress("UNUSED_PARAMETER") onNavigateBack: () -> Unit = {}
) = Unit
