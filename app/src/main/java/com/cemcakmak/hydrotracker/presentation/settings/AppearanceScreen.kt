package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.animation.EnterExitState
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import android.content.Context
import android.os.Build
import android.view.RoundedCorner
import android.view.WindowManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.AppFont
import com.cemcakmak.hydrotracker.data.models.ColorSource
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.ui.theme.fontFamilyFor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceScreen(
    themePreferences: ThemePreferences = ThemePreferences(),
    isDynamicColorAvailable: Boolean = true,
    onColorSourceChange: (ColorSource) -> Unit = {},
    onDarkModeChange: (DarkModePreference) -> Unit = {},
    onPureBlackChange: (Boolean) -> Unit = {},
    onAppFontChange: (AppFont) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues()
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
    var showFontSheet by remember { mutableStateOf(false) }

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
                title = { Text("Appearance") },
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
                            contentDescription = "Back"
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ThemePreviewCard(themePreferences = themePreferences)

            DarkModeSection(
                darkMode = themePreferences.darkMode,
                onDarkModeChange = onDarkModeChange
            )

            ColorSection(
                themePreferences = themePreferences,
                isDynamicColorAvailable = isDynamicColorAvailable,
                onColorSourceChange = onColorSourceChange,
                onPureBlackChange = onPureBlackChange
            )

            FontSection(
                selectedFont = themePreferences.appFont,
                onOpenFontSheet = { showFontSheet = true }
            )
        }
    }

    if (showFontSheet) {
        FontBottomSheet(
            selectedFont = themePreferences.appFont,
            onAppFontChange = onAppFontChange,
            onDismiss = { showFontSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemePreviewCard(themePreferences: ThemePreferences) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 0.5f,
            animationSpec = tween(durationMillis = 1200)
        )
    }

    val isDark = when (themePreferences.darkMode) {
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val border = if (themePreferences.usePureBlack && isDark) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        shape = RoundedCornerShape(size = 30.dp),
        tonalElevation = 2.dp,
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "1,250 / 2,500 ml",
                style = MaterialTheme.typography.headlineMedium
            )

            LinearWavyProgressIndicator(
                progress = { animatedProgress.value },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
                trackStroke = WavyProgressIndicatorDefaults.linearTrackStroke,
                amplitude = WavyProgressIndicatorDefaults.indicatorAmplitude,
                wavelength = WavyProgressIndicatorDefaults.LinearDeterminateWavelength,
                waveSpeed = WavyProgressIndicatorDefaults.LinearDeterminateWavelength
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PreviewStatChip(label = "Entries", value = "5")
                PreviewStatChip(label = "First", value = "08:30")
                PreviewStatChip(label = "Latest", value = "14:45")
            }
        }
    }
}

private data class GroupCorners(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomStart: Dp,
    val bottomEnd: Dp
)

/** Per-corner radii for a card at [index] within a grouped list of [size] items. */
private fun groupCorners(index: Int, size: Int): GroupCorners {
    val outer = 30.dp
    val inner = 6.dp
    return when {
        size == 1 -> GroupCorners(outer, outer, outer, outer)
        index == 0 -> GroupCorners(outer, outer, inner, inner)
        index == size - 1 -> GroupCorners(inner, inner, outer, outer)
        else -> GroupCorners(inner, inner, inner, inner)
    }
}

private fun getGroupShape(index: Int, size: Int): Shape {
    val c = groupCorners(index, size)
    return RoundedCornerShape(
        topStart = c.topStart,
        topEnd = c.topEnd,
        bottomStart = c.bottomStart,
        bottomEnd = c.bottomEnd
    )
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        modifier = Modifier.padding(start = 4.dp),
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsGroupCard(
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DarkModeSection(
    darkMode: DarkModePreference,
    onDarkModeChange: (DarkModePreference) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader("Theme")
        val haptics = LocalHapticFeedback.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DarkModePreference.entries.forEach { preference ->
                val isSelected = darkMode == preference

                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = {
                        onDarkModeChange(preference)
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Crossfade(
                            targetState = isSelected,
                            animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                            label = "darkModeToggleIcon_${preference.name}"
                        ) { selected ->
                            Icon(
                                imageVector = when (preference) {
                                    DarkModePreference.SYSTEM -> if (selected) ImageVector.vectorResource(R.drawable.settings_filled) else ImageVector.vectorResource(R.drawable.settings)
                                    DarkModePreference.LIGHT -> if (selected) ImageVector.vectorResource(R.drawable.light_mode_filled) else ImageVector.vectorResource(R.drawable.light_mode)
                                    DarkModePreference.DARK -> if (selected) ImageVector.vectorResource(R.drawable.dark_mode_filled) else ImageVector.vectorResource(R.drawable.dark_mode)
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = when (preference) {
                                DarkModePreference.SYSTEM -> "System"
                                DarkModePreference.LIGHT -> "Light"
                                DarkModePreference.DARK -> "Dark"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSection(
    themePreferences: ThemePreferences,
    isDynamicColorAvailable: Boolean,
    onColorSourceChange: (ColorSource) -> Unit,
    onPureBlackChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader("Color")
        Column {
            // Build the rows for this section; add another entry here to grow the list.
            val rows = buildList<@Composable () -> Unit> {
                if (isDynamicColorAvailable) {
                    add {
                        DynamicColorsRow(
                            themePreferences = themePreferences,
                            onColorSourceChange = onColorSourceChange
                        )
                    }
                }
                add {
                    AmoledRow(
                        themePreferences = themePreferences,
                        onPureBlackChange = onPureBlackChange
                    )
                }
            }
            rows.forEachIndexed { index, row ->
                SettingsGroupCard(index = index, size = rows.size) {
                    row()
                }
            }
        }
    }
}

@Composable
private fun DynamicColorsRow(
    themePreferences: ThemePreferences,
    onColorSourceChange: (ColorSource) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Crossfade(
            targetState = themePreferences.colorSource == ColorSource.DYNAMIC_COLOR,
            animationSpec = tween(400),
            label = "paletteIcon"
        ) { isDynamic ->
            Icon(
                imageVector = if (isDynamic) ImageVector.vectorResource(R.drawable.palette_filled) else ImageVector.vectorResource(R.drawable.palette),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Dynamic Colors",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Colors from your wallpaper",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = themePreferences.colorSource == ColorSource.DYNAMIC_COLOR,
            onCheckedChange = { enabled ->
                haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                onColorSourceChange(
                    if (enabled) ColorSource.DYNAMIC_COLOR else ColorSource.HYDRO_THEME,
                )
            },
            thumbContent = if (themePreferences.colorSource == ColorSource.DYNAMIC_COLOR) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                null
            }
        )
    }
}

@Composable
private fun AmoledRow(
    themePreferences: ThemePreferences,
    onPureBlackChange: (Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Crossfade(
            targetState = themePreferences.usePureBlack,
            animationSpec = tween(400),
            label = "darkModeIcon"
        ) { isPureBlack ->
            Icon(
                imageVector = if (isPureBlack) ImageVector.vectorResource(R.drawable.dark_mode_filled) else ImageVector.vectorResource(R.drawable.dark_mode),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "AMOLED Mode",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "True black backgrounds in dark mode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = themePreferences.usePureBlack,
            onCheckedChange = { enabled ->
                onPureBlackChange(enabled)
                haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
            },
            thumbContent = if (themePreferences.usePureBlack) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                null
            }
        )
    }
}

@Composable
private fun FontSection(
    selectedFont: AppFont,
    onOpenFontSheet: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionHeader("Font")
        SettingsGroupCard(
            index = 0,
            size = 1,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onOpenFontSheet()
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.font_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = selectedFont.getDisplayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = fontFamilyFor(selectedFont),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontBottomSheet(
    selectedFont: AppFont,
    onAppFontChange: (AppFont) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val haptics = LocalHapticFeedback.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val fonts = AppFont.entries
            fonts.forEachIndexed { index, font ->
                FontOptionCard(
                    font = font,
                    index = index,
                    size = fonts.size,
                    selected = font == selectedFont,
                    onClick = {
                        onAppFontChange(font)
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontOptionCard(
    font: AppFont,
    index: Int,
    size: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Selected option morphs to a pill
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "fontSelection"
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
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "fontContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "fontContent"
    )
    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
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
            Text(
                text = font.getDisplayName(),
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = fontFamilyFor(font),
                color = contentColor
            )
        }
    }
}

@Composable
private fun PreviewStatChip(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ThemePreviewCardPreview() {
    HydroTrackerTheme {
        ThemePreviewCard(themePreferences = ThemePreferences())
    }
}

@Preview(showBackground = true)
@Composable
fun AppearanceScreenWithAppBarPreview() {
    var previewPreferences by remember {
        mutableStateOf(ThemePreferences())
    }

    HydroTrackerTheme(themePreferences = previewPreferences) {
        AppearanceScreen(
            themePreferences = previewPreferences,
            isDynamicColorAvailable = true,
            onColorSourceChange = { source ->
                previewPreferences = previewPreferences.copy(colorSource = source)
            },
            onDarkModeChange = { mode ->
                previewPreferences = previewPreferences.copy(darkMode = mode)
            },
            onPureBlackChange = { enabled ->
                previewPreferences = previewPreferences.copy(usePureBlack = enabled)
            }
        )
    }
}
