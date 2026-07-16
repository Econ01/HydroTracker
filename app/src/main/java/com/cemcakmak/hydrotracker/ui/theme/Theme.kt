package com.cemcakmak.hydrotracker.ui.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.ColorSource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.MaterialColors
import com.google.android.material.color.utilities.TonalPalette

// HydroTracker Light Colour Scheme
// internal: also consumed by the Glance widget theme as the non-dynamic fallback.
internal val HydroLightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
)

// HydroTracker Dark Colour Scheme
// internal: also consumed by the Glance widget theme as the non-dynamic fallback.
internal val HydroDarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
)

// Seed colours for the extended palettes.
private val SuccessSeed = Color(0xFF1E8E3E)
private val WarningSeed = Color(0xFFFF9500)
private val WaterSeed = Color(0xFF00B4D8)
private val CoffeeSeed = Color(0xFF5D4037)
private val EnergyDrinkSeed = Color(0xFFFF3B30)
private val FruitJuiceSeed = Color(0xFFFF9500)
private val MilkSeed = Color(0xFFF2EFE9)
private val ORSSeed = Color(0xFF2EC4B6)
private val SoftDrinkSeed = Color(0xFFE71D36)
private val SportsDrinkSeed = Color(0xFF9B5DE5)
private val TeaSeed = Color(0xFFD4A373)

private data class CustomColorRoles(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

private fun Color.harmonizedWith(primary: Color): Color {
    val harmonizedArgb = MaterialColors.harmonize(this.toArgb(), primary.toArgb())
    return Color(harmonizedArgb)
}

@SuppressLint("RestrictedApi")
private fun TonalPalette.toLightRoles() = CustomColorRoles(
    color = Color(tone(40)),
    onColor = Color(tone(100)),
    colorContainer = Color(tone(90)),
    onColorContainer = Color(tone(10))
)

@SuppressLint("RestrictedApi")
private fun TonalPalette.toDarkRoles() = CustomColorRoles(
    color = Color(tone(80)),
    onColor = Color(tone(20)),
    colorContainer = Color(tone(30)),
    onColorContainer = Color(tone(90))
)

@SuppressLint("RestrictedApi")
private fun extendedColorScheme(
    successSeed: Color,
    warningSeed: Color,
    waterSeed: Color,
    coffeeSeed: Color,
    energyDrinkSeed: Color,
    fruitJuiceSeed: Color,
    milkSeed: Color,
    oralRehydrationSolutionSeed: Color,
    softDrinkSeed: Color,
    sportsDrinkSeed: Color,
    teaSeed: Color,
    primaryColor: Color,
    isDark: Boolean
): ExtendedColorScheme {
    val successHarmonized = successSeed.harmonizedWith(primaryColor)
    val warningHarmonized = warningSeed.harmonizedWith(primaryColor)
    val waterHarmonized = waterSeed.harmonizedWith(primaryColor)
    val coffeeHarmonized = coffeeSeed.harmonizedWith(primaryColor)
    val energyDrinkHarmonized = energyDrinkSeed.harmonizedWith(primaryColor)
    val fruitJuiceHarmonized = fruitJuiceSeed.harmonizedWith(primaryColor)
    val milkHarmonized = milkSeed.harmonizedWith(primaryColor)
    val oralRehydrationSolutionHarmonized = oralRehydrationSolutionSeed.harmonizedWith(primaryColor)
    val softDrinkHarmonized = softDrinkSeed.harmonizedWith(primaryColor)
    val sportsDrinkHarmonized = sportsDrinkSeed.harmonizedWith(primaryColor)
    val teaHarmonized = teaSeed.harmonizedWith(primaryColor)

    val successRoles = TonalPalette.fromInt(successHarmonized.toArgb())
        .let { if (isDark) it.toDarkRoles() else it.toLightRoles() }

    val warningRoles = TonalPalette.fromInt(warningHarmonized.toArgb())
        .let { if (isDark) it.toDarkRoles() else it.toLightRoles() }

    val waterRoles = TonalPalette.fromInt(waterHarmonized.toArgb())
        .let { if (isDark) it.toDarkRoles() else it.toLightRoles() }

    val coffeeRoles = TonalPalette.fromInt(coffeeHarmonized.toArgb())
        .let { if (isDark) it.toDarkRoles() else it.toLightRoles() }

    val energyDrinkRoles = TonalPalette.fromInt(energyDrinkHarmonized.toArgb())
        .let { if (isDark) it.toDarkRoles() else it.toLightRoles() }

    val fruitJuiceRoles = TonalPalette.fromInt(fruitJuiceHarmonized.toArgb())
        .let { if (isDark) it.toDarkRoles() else it.toLightRoles() }

    val milkRoles = TonalPalette.fromInt(milkHarmonized.toArgb())
        .let { if (isDark) it.toDarkRoles() else it.toLightRoles() }

    val oralRehydrationSolutionRoles = TonalPalette.fromInt(oralRehydrationSolutionHarmonized.toArgb())
        .let { if (isDark) it.toDarkRoles() else it.toLightRoles() }

    val softDrinkRoles = TonalPalette.fromInt(softDrinkHarmonized.toArgb())
        .let { if (isDark) it.toDarkRoles() else it.toLightRoles() }

    val sportsDrinkRoles = TonalPalette.fromInt(sportsDrinkHarmonized.toArgb())
        .let { if (isDark) it.toDarkRoles() else it.toLightRoles() }

    val teaRoles = TonalPalette.fromInt(teaHarmonized.toArgb())
        .let { if (isDark) it.toDarkRoles() else it.toLightRoles() }

    return ExtendedColorScheme(
        success = successRoles.color,
        onSuccess = successRoles.onColor,
        successContainer = successRoles.colorContainer,
        onSuccessContainer = successRoles.onColorContainer,
        warning = warningRoles.color,
        onWarning = warningRoles.onColor,
        warningContainer = warningRoles.colorContainer,
        onWarningContainer = warningRoles.onColorContainer,
        water = waterRoles.color,
        onWater = waterRoles.onColor,
        waterContainer = waterRoles.colorContainer,
        onWaterContainer = waterRoles.onColorContainer,
        coffee = coffeeRoles.color,
        onCoffee = coffeeRoles.onColor,
        coffeeContainer = coffeeRoles.colorContainer,
        onCoffeeContainer = coffeeRoles.onColorContainer,
        energyDrink = energyDrinkRoles.color,
        onEnergyDrink = energyDrinkRoles.onColor,
        energyDrinkContainer = energyDrinkRoles.colorContainer,
        onEnergyDrinkContainer = energyDrinkRoles.onColorContainer,
        fruitJuice = fruitJuiceRoles.color,
        onFruitJuice = fruitJuiceRoles.onColor,
        fruitJuiceContainer = fruitJuiceRoles.colorContainer,
        onFruitJuiceContainer = fruitJuiceRoles.onColorContainer,
        milk = milkRoles.color,
        onMilk = milkRoles.onColor,
        milkContainer = milkRoles.colorContainer,
        onMilkContainer = milkRoles.onColorContainer,
        oralRehydrationSolution = oralRehydrationSolutionRoles.color,
        onOralRehydrationSolution = oralRehydrationSolutionRoles.onColor,
        oralRehydrationSolutionContainer = oralRehydrationSolutionRoles.colorContainer,
        onOralRehydrationSolutionContainer = oralRehydrationSolutionRoles.onColorContainer,
        softDrink = softDrinkRoles.color,
        onSoftDrink = softDrinkRoles.onColor,
        softDrinkContainer = softDrinkRoles.colorContainer,
        onSoftDrinkContainer = softDrinkRoles.onColorContainer,
        sportsDrink = sportsDrinkRoles.color,
        onSportsDrink = sportsDrinkRoles.onColor,
        sportsDrinkContainer = sportsDrinkRoles.colorContainer,
        onSportsDrinkContainer = sportsDrinkRoles.onColorContainer,
        tea = teaRoles.color,
        onTea = teaRoles.onColor,
        teaContainer = teaRoles.colorContainer,
        onTeaContainer = teaRoles.onColorContainer
    )
}

/**
 * The active [ThemePreferences], provided by [HydroTrackerTheme] so deeply nested components
 * (e.g. shared settings cards) can react to appearance settings without explicit parameters.
 */
val LocalThemePreferences = compositionLocalOf { ThemePreferences() }

@Composable
fun HydroTrackerTheme(
    themePreferences: ThemePreferences = ThemePreferences(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Determine dark mode based on user preference
    val darkTheme = when (themePreferences.darkMode) {
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
    }

    // Choose colour scheme based on user preference
    val baseColorScheme = when (themePreferences.colorSource) {
        ColorSource.DYNAMIC_COLOR -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) HydroDarkColorScheme else HydroLightColorScheme
            }
        }

        ColorSource.HYDRO_THEME -> {
            if (darkTheme) HydroDarkColorScheme else HydroLightColorScheme
        }
    }

    // Apply pure black mode override if enabled and in dark theme
    val colorScheme = if (themePreferences.usePureBlack && darkTheme) {
        baseColorScheme.copy(
            surface = Color.Black,
            background = Color.Black
        )
    } else {
        baseColorScheme
    }

    val extendedColors = extendedColorScheme(
        successSeed = SuccessSeed,
        warningSeed = WarningSeed,
        waterSeed = WaterSeed,
        coffeeSeed = CoffeeSeed,
        energyDrinkSeed = EnergyDrinkSeed,
        fruitJuiceSeed = FruitJuiceSeed,
        milkSeed = MilkSeed,
        oralRehydrationSolutionSeed = ORSSeed,
        softDrinkSeed = SoftDrinkSeed,
        sportsDrinkSeed = SportsDrinkSeed,
        teaSeed = TeaSeed,
        primaryColor = colorScheme.primary,
        isDark = darkTheme
    )

    // Typography follows the user's selected font, rebuilt only when the font changes
    val typography = remember(themePreferences.appFont) {
        hydroTypography(fontFamilyFor(themePreferences.appFont))
    }

    // Keep the system bar icon colours in sync with the theme. The activity is no longer
    // recreated on a theme change (configChanges), so enableEdgeToEdge() in onCreate can't
    // refresh this — drive it reactively here instead.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalExtendedColorScheme provides extendedColors,
        LocalThemePreferences provides themePreferences
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            motionScheme = MotionScheme.expressive(),
            content = content
        )
    }
}