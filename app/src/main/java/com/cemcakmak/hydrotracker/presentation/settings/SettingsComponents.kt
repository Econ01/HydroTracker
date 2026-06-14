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
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.presentation.common.groupCorners
import com.cemcakmak.hydrotracker.presentation.common.getGroupShape

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

    Scaffold(
        modifier = Modifier
            .padding(paddingValues)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .clip(RoundedCornerShape(cornerRadius)),
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
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
                .padding(horizontal = 16.dp)
                .then(if (scrollable) Modifier.padding(bottom = 24.dp) else Modifier),
            verticalArrangement = if (scrollable) Arrangement.spacedBy(24.dp) else Arrangement.Top,
            content = content
        )
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

@Composable
internal fun SettingsGroupCard(
    index: Int,
    size: Int,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = getGroupShape(index, size)
    val modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 2.dp)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            tonalElevation = 2.dp,
            modifier = modifier
        ) { content() }
    } else {
        Surface(
            shape = shape,
            tonalElevation = 2.dp,
            modifier = modifier
        ) { content() }
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
