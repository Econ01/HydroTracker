package com.cemcakmak.hydrotracker.widgets

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
     * Success colour used for the "goal reached" state.
     * Matches the in-app success seed colour used by the extended colour scheme.
     */
    val success: ColorProvider = ColorProvider(
        day = Color(0xFF1E8E3E),
        night = Color(0xFF7BD88F),
    )
}

/**
 * Top-level theme wrapper for every HydroTracker widget.
 *
 * Passing no colours lets [GlanceTheme] resolve its default, which is the dynamic system
 * colour theme on API 31+. Below API 31 we explicitly supply the HYDRO_THEME palette.
 */
@Composable
fun HydroWidgetTheme(content: @GlanceComposable @Composable () -> Unit) {
    GlanceTheme(
        colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Default LocalColors value: DynamicThemeColorProviders (Material You).
            GlanceTheme.colors
        } else {
            HydroWidgetColors.hydroColors
        },
    ) {
        content()
    }
}
