package com.cemcakmak.hydrotracker.widgets

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.glance.GlanceComposable
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProvider
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider
import com.cemcakmak.hydrotracker.ui.theme.HydroDarkColorScheme
import com.cemcakmak.hydrotracker.ui.theme.HydroLightColorScheme

/**
 * Colour theming for the Glance home-screen widgets.
 *
 * On Android 12+ (API 31) the widgets follow the system's Material You dynamic colours,
 * derived from the user's wallpaper. On older devices, or if dynamic colours are
 * unavailable, the widgets fall back to the app's HYDRO_THEME palette.
 */
object HydroWidgetColors {

    /**
     * The HYDRO_THEME palette expressed as Glance [ColorProviders].
     * Built directly from the app's own light/dark colour schemes so the widgets and the
     * app always stay in sync.
     */
    val hydroColors: ColorProviders = ColorProviders(
        light = HydroLightColorScheme,
        dark = HydroDarkColorScheme,
    )

    /**
     * Success colour used for the "goal reached" state, taken from the extended colour
     * scheme's success family (harmonized with the widget's primary, light and dark).
     */
    fun success(context: Context, useDynamicColors: Boolean = true): ColorProvider {
        val (light, dark) = widgetExtendedColors(context, useDynamicColors)
        return ColorProvider(day = light.success, night = dark.success)
    }
}

/**
 * Top-level theme wrapper for every HydroTracker widget.
 *
 * Passing no colours lets [GlanceTheme] resolve its default, which is the dynamic system
 * colour theme on API 31+. Below API 31 we explicitly supply the HYDRO_THEME palette.
 * [forceHydroColors] forces the HYDRO_THEME palette on all versions (the user's
 * "dynamic colours off" widget setting).
 */
@Composable
fun HydroWidgetTheme(
    forceHydroColors: Boolean = false,
    content: @GlanceComposable @Composable () -> Unit,
) {
    GlanceTheme(
        colors = if (!forceHydroColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Default LocalColors value: DynamicThemeColorProviders (Material You).
            GlanceTheme.colors
        } else {
            HydroWidgetColors.hydroColors
        },
    ) {
        content()
    }
}
