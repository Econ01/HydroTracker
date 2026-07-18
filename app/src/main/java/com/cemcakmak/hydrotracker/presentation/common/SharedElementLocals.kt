package com.cemcakmak.hydrotracker.presentation.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

/**
 * Composition locals for shared element transitions.
 *
 * Provide these around screens that participate in shared element transitions.
 * Composables that support shared elements can consume them and fall back to
 * normal rendering when the locals are not present (e.g., in previews or
 * onboarding).
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
