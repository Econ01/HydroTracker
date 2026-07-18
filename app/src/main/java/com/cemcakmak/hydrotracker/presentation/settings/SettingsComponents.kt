/*
 *
 *  * HydroTracker - A modern and private water intake tracking application
 *  * Copyright (c) 2026 Ali Cem Çakmak
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import android.content.Context
import android.os.Build
import android.view.RoundedCorner
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.EdgeEffect
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.presentation.common.effect.BackdropBlurState
import com.cemcakmak.hydrotracker.presentation.common.effect.BackdropBlurStyle
import com.cemcakmak.hydrotracker.presentation.common.effect.BackdropProgressive
import com.cemcakmak.hydrotracker.presentation.common.effect.backdropBlur
import com.cemcakmak.hydrotracker.presentation.common.effect.backdropSource
import com.cemcakmak.hydrotracker.presentation.common.effect.rememberBackdropBlurState
import com.cemcakmak.hydrotracker.presentation.common.groupCorners
import com.cemcakmak.hydrotracker.presentation.common.shapes.PillShape
import com.cemcakmak.hydrotracker.presentation.common.shapes.SquircleShape
import com.cemcakmak.hydrotracker.ui.theme.LocalThemePreferences

/**
 * Shared UI building blocks for the settings sub-screens (Appearance, Display & Locale, …).
 * Kept here so every sub-screen uses the same scaffold, section headers and grouped-card list.
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsDetailScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(),
    scrollable: Boolean = true,
    themePreferences: ThemePreferences = ThemePreferences(),
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val haptics = LocalHapticFeedback.current

    val isPreview = LocalInspectionMode.current
    val animatedContentScope = if (isPreview) null else LocalNavAnimatedContentScope.current
    val isExiting = animatedContentScope?.transition?.targetState == EnterExitState.PostExit

    val context = LocalContext.current
    val density = LocalDensity.current
    val deviceCornerRadius = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val insets = windowManager.currentWindowMetrics.windowInsets
            val corner = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
            corner?.let { with(density) { it.radius.toDp() } }
        } else null
    } ?: 24.dp

    val cornerRadius = if (isExiting) deviceCornerRadius else 0.dp

    // Scroll haptic logic
    var wasExpanded by remember { mutableStateOf(true) }
    var wasCollapsed by remember { mutableStateOf(false) }

    LaunchedEffect(scrollBehavior.state) {
        snapshotFlow { scrollBehavior.state.collapsedFraction }
            .collect { fraction ->
                val isExpanded = fraction == 0f
                val isCollapsed = fraction == 1f

                if (isExpanded && !wasExpanded) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                if (isCollapsed && !wasCollapsed) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }

                wasExpanded = isExpanded
                wasCollapsed = isCollapsed
            }
    }

    val edgeEffectStyle = themePreferences.edgeEffect.let {
        if (it == EdgeEffect.BLURRED && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            EdgeEffect.TRANSPARENT
        } else {
            it
        }
    }

    // Backdrop captured for the frosted top band
    val backdropState = rememberBackdropBlurState()
    var contentPadding by remember { mutableStateOf(PaddingValues()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .clip(SquircleShape(CornerSize(cornerRadius))),
            topBar = {
                LargeFlexibleTopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        val collapsedFraction = scrollBehavior.state.collapsedFraction
                        val buttonWidth = (40 - collapsedFraction * 8).dp
                        FilledIconButton(
                            onClick = {
                                onNavigateBack()
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            },
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledIconButtonColors(),
                            modifier = Modifier.size(width = buttonWidth, height = 40.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.arrow_back_filled),
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            contentPadding = innerPadding
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (edgeEffectStyle == EdgeEffect.BLURRED) {
                            Modifier
                                .backdropSource(backdropState)
                                .background(MaterialTheme.colorScheme.surface)
                        } else {
                            Modifier
                        }
                    )
                    .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
                    .padding(horizontal = 16.dp)
                    .then(if (scrollable) Modifier.padding(bottom = 24.dp) else Modifier),
                verticalArrangement = if (scrollable) Arrangement.spacedBy(24.dp) else Arrangement.Top,
            ) {
                Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))
                content()
            }

            TopEdgeEffect(
                style = edgeEffectStyle,
                backdropState = backdropState,
                paddingValues = contentPadding,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
internal fun TopEdgeEffect(
    style: EdgeEffect,
    backdropState: BackdropBlurState,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    val bandHeight = paddingValues.calculateTopPadding()

    when (style) {
        EdgeEffect.TRANSPARENT -> Unit
        EdgeEffect.SCRIM -> {
            val scrimColor = MaterialTheme.colorScheme.surface
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(bandHeight + 20.dp)
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(scrimColor, Color.Transparent)
                            )
                        )
                    }
            )
        }
        EdgeEffect.BLURRED -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(bandHeight)
                        .backdropBlur(
                            state = backdropState,
                            style = BackdropBlurStyle(
                                blurRadius = 20.dp,
                                progressive = BackdropProgressive(
                                    startFraction = 0f,
                                    endFraction = 1f
                                ),
                                tint = MaterialTheme.colorScheme.surface.copy(0.4f)
                            )
                        )
                )
            }
        }
    }
}

@Composable
internal fun SettingsSectionHeader(title: String) {
    Text(
        modifier = Modifier.padding(start = 4.dp),
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * The shared AMOLED hairline border for grouped settings cards. Returns null unless the app is
 * in a dark theme with both AMOLED mode and the surface-borders preference enabled, so every
 * settings group gets the same treatment without each screen wiring it up. Reads the active
 * preferences from [LocalThemePreferences], provided by `HydroTrackerTheme`.
 */
@Composable
internal fun amoledGroupBorder(): BorderStroke? {
    val themePreferences = LocalThemePreferences.current
    val isDark = when (themePreferences.darkMode) {
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
    }
    return if (themePreferences.usePureBlack && themePreferences.showAmoledBorders && isDark) {
        BorderStroke(0.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    } else {
        null
    }
}

@Composable
internal fun SettingsGroupCard(
    index: Int,
    size: Int,
    isPill: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = if (isPill) PillShape else getShapeForIndex(index, size)
    val border = amoledGroupBorder()
    val modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 2.dp)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            tonalElevation = 2.dp,
            border = border,
            modifier = modifier
        ) { content() }
    } else {
        Surface(
            shape = shape,
            tonalElevation = 2.dp,
            border = border,
            modifier = modifier
        ) { content() }
    }
}

internal fun getShapeForIndex(
    index: Int,
    size: Int,
    outerRadius: Dp = 30.dp,
    innerRadius: Dp = 6.dp
): Shape {
    return when {
        size == 1 -> SquircleShape(
            topStart = CornerSize(outerRadius),
            topEnd = CornerSize(outerRadius),
            bottomStart = CornerSize(outerRadius),
            bottomEnd = CornerSize(outerRadius)
        )
        index == 0 -> SquircleShape(
            topStart = CornerSize(outerRadius),
            topEnd = CornerSize(outerRadius),
            bottomStart = CornerSize(innerRadius),
            bottomEnd = CornerSize(innerRadius)
        )
        index == size - 1 -> SquircleShape(
            topStart = CornerSize(innerRadius),
            topEnd = CornerSize(innerRadius),
            bottomStart = CornerSize(outerRadius),
            bottomEnd = CornerSize(outerRadius)
        )
        else -> RoundedCornerShape(innerRadius)
    }
}

/** A grouped card whose selection animates to a primary-coloured pill. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SelectableOptionCard(
    index: Int,
    size: Int,
    selected: Boolean,
    onClick: () -> Unit,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    content: @Composable (contentColor: Color) -> Unit
) {
    // Selected option morphs to a pill and turns primary.
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "optionSelection"
    )
    val corners = groupCorners(index, size)
    val pill = 28.dp
    val shape = RoundedCornerShape(
        topStart = lerp(corners.topStart, pill, progress),
        topEnd = lerp(corners.topEnd, pill, progress),
        bottomStart = lerp(corners.bottomStart, pill, progress),
        bottomEnd = lerp(corners.bottomEnd, pill, progress)
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) selectedContainerColor else unselectedContainerColor,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "optionContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else unselectedContentColor,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "optionContent"
    )
    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content(contentColor)
        }
    }
}
