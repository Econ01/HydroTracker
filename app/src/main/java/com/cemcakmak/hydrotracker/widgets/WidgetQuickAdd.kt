package com.cemcakmak.hydrotracker.widgets

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.ui.theme.ExtendedColorScheme
import com.cemcakmak.hydrotracker.ui.theme.HydroDarkColorScheme
import com.cemcakmak.hydrotracker.ui.theme.HydroLightColorScheme
import com.cemcakmak.hydrotracker.ui.theme.extendedColorSchemeFor

/**
 * One quick-add card slot driven by the user's logging history: the most frequently logged
 * (container, volume, beverage) combinations, mirrored into Glance preferences by
 * [HydroWidgetUpdater]. Colours are deliberately not stored — they are derived from the
 * extended palette at composition time via [beverageCardColours].
 */
data class WidgetQuickAddSlot(
    val volume: Double,
    val beverageName: String,
    val containerName: String,
)

/** Themed colour set for one quick-add card (container, icon pill, pill content, text). */
internal data class WidgetCardColours(
    val container: ColorProvider,
    val pill: ColorProvider,
    val pillContent: ColorProvider,
    val content: ColorProvider,
)

private data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
)

/** The four colour roles of [beverage] within this scheme. Unknown/custom beverages use water. */
private fun ExtendedColorScheme.familyOf(beverage: BeverageType): ColorFamily = when (beverage) {
    BeverageType.WATER -> ColorFamily(water, onWater, waterContainer, onWaterContainer)
    BeverageType.COFFEE -> ColorFamily(coffee, onCoffee, coffeeContainer, onCoffeeContainer)
    BeverageType.TEA -> ColorFamily(tea, onTea, teaContainer, onTeaContainer)
    BeverageType.SOFT_DRINK -> ColorFamily(softDrink, onSoftDrink, softDrinkContainer, onSoftDrinkContainer)
    BeverageType.ENERGY_DRINK -> ColorFamily(energyDrink, onEnergyDrink, energyDrinkContainer, onEnergyDrinkContainer)
    BeverageType.SPORTS_DRINK -> ColorFamily(sportsDrink, onSportsDrink, sportsDrinkContainer, onSportsDrinkContainer)
    BeverageType.ORAL_REHYDRATION_SOLUTION -> ColorFamily(
        oralRehydrationSolution,
        onOralRehydrationSolution,
        oralRehydrationSolutionContainer,
        onOralRehydrationSolutionContainer
    )
    BeverageType.MILK -> ColorFamily(milk, onMilk, milkContainer, onMilkContainer)
    BeverageType.FRUIT_JUICE -> ColorFamily(fruitJuice, onFruitJuice, fruitJuiceContainer, onFruitJuiceContainer)
}

/**
 * The extended colour scheme in light and dark variants, harmonized with the same primary the
 * widget chrome uses: Material You dynamic colours on API 31+ (unless [useDynamicColors] is
 * off), the HYDRO_THEME palette otherwise. Falls back to the HYDRO primary when dynamic
 * colours are unavailable (e.g. Design previews).
 */
internal fun widgetExtendedColors(
    context: Context,
    useDynamicColors: Boolean = true,
): Pair<ExtendedColorScheme, ExtendedColorScheme> {
    val (lightPrimary, darkPrimary) = if (useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching {
            dynamicLightColorScheme(context).primary to dynamicDarkColorScheme(context).primary
        }.getOrElse {
            HydroLightColorScheme.primary to HydroDarkColorScheme.primary
        }
    } else {
        HydroLightColorScheme.primary to HydroDarkColorScheme.primary
    }
    return extendedColorSchemeFor(lightPrimary, isDark = false) to
        extendedColorSchemeFor(darkPrimary, isDark = true)
}

/**
 * Day/night card colours for [beverageName] from the extended palette, role-mapped like the
 * default theme cards (container = `*Container`, pill = colour, pill content = `on*`,
 * text = `on*Container`). Unknown or custom beverage names fall back to the water family.
 */
internal fun beverageCardColours(
    context: Context,
    beverageName: String,
    useDynamicColors: Boolean = true,
): WidgetCardColours {
    val beverage = BeverageType.fromStringOrDefault(beverageName)
    val (light, dark) = widgetExtendedColors(context, useDynamicColors)
    val day = light.familyOf(beverage)
    val night = dark.familyOf(beverage)
    return WidgetCardColours(
        container = ColorProvider(day = day.container, night = night.container),
        pill = ColorProvider(day = day.color, night = night.color),
        pillContent = ColorProvider(day = day.onColor, night = night.onColor),
        content = ColorProvider(day = day.onContainer, night = night.onContainer),
    )
}
