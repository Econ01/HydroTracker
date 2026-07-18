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

package com.cemcakmak.hydrotracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.cemcakmak.hydrotracker.data.models.BeverageType

data class ExtendedColorScheme(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val water: Color,
    val onWater: Color,
    val waterContainer: Color,
    val onWaterContainer: Color,
    val coffee: Color,
    val onCoffee: Color,
    val coffeeContainer: Color,
    val onCoffeeContainer: Color,
    val energyDrink: Color,
    val onEnergyDrink: Color,
    val energyDrinkContainer: Color,
    val onEnergyDrinkContainer: Color,
    val fruitJuice: Color,
    val onFruitJuice: Color,
    val fruitJuiceContainer: Color,
    val onFruitJuiceContainer: Color,
    val milk: Color,
    val onMilk: Color,
    val milkContainer: Color,
    val onMilkContainer: Color,
    val oralRehydrationSolution: Color,
    val onOralRehydrationSolution: Color,
    val oralRehydrationSolutionContainer: Color,
    val onOralRehydrationSolutionContainer: Color,
    val softDrink: Color,
    val onSoftDrink: Color,
    val softDrinkContainer: Color,
    val onSoftDrinkContainer: Color,
    val sportsDrink: Color,
    val onSportsDrink: Color,
    val sportsDrinkContainer: Color,
    val onSportsDrinkContainer: Color,
    val tea: Color,
    val onTea: Color,
    val teaContainer: Color,
    val onTeaContainer: Color
)

val LocalExtendedColorScheme = staticCompositionLocalOf {
    // Default fallback
    ExtendedColorScheme(
        success = Color.Unspecified,
        onSuccess = Color.Unspecified,
        successContainer = Color.Unspecified,
        onSuccessContainer = Color.Unspecified,
        warning = Color.Unspecified,
        onWarning = Color.Unspecified,
        warningContainer = Color.Unspecified,
        onWarningContainer = Color.Unspecified,
        water = Color.Unspecified,
        onWater = Color.Unspecified,
        waterContainer = Color.Unspecified,
        onWaterContainer = Color.Unspecified,
        coffee = Color.Unspecified,
        onCoffee = Color.Unspecified,
        coffeeContainer = Color.Unspecified,
        onCoffeeContainer = Color.Unspecified,
        energyDrink = Color.Unspecified,
        onEnergyDrink = Color.Unspecified,
        energyDrinkContainer = Color.Unspecified,
        onEnergyDrinkContainer = Color.Unspecified,
        fruitJuice = Color.Unspecified,
        onFruitJuice = Color.Unspecified,
        fruitJuiceContainer = Color.Unspecified,
        onFruitJuiceContainer = Color.Unspecified,
        milk = Color.Unspecified,
        onMilk = Color.Unspecified,
        milkContainer = Color.Unspecified,
        onMilkContainer = Color.Unspecified,
        oralRehydrationSolution = Color.Unspecified,
        onOralRehydrationSolution = Color.Unspecified,
        oralRehydrationSolutionContainer = Color.Unspecified,
        onOralRehydrationSolutionContainer = Color.Unspecified,
        softDrink = Color.Unspecified,
        onSoftDrink = Color.Unspecified,
        softDrinkContainer = Color.Unspecified,
        onSoftDrinkContainer = Color.Unspecified,
        sportsDrink = Color.Unspecified,
        onSportsDrink = Color.Unspecified,
        sportsDrinkContainer = Color.Unspecified,
        onSportsDrinkContainer = Color.Unspecified,
        tea = Color.Unspecified,
        onTea = Color.Unspecified,
        teaContainer = Color.Unspecified,
        onTeaContainer = Color.Unspecified
    )
}

@Suppress("UnusedReceiverParameter")    // The pattern is intentional to keep the syntax consistent.
val MaterialTheme.extendedColorScheme: ExtendedColorScheme
    @Composable
    get() = LocalExtendedColorScheme.current

/**
 * The four colour roles for a single beverage: main accent, content on that accent,
 * container surface, and content on the container.
 */
data class BeverageColorRoles(
    val color: Color,
    val onColor: Color,
    val containerColor: Color,
    val onContainerColor: Color
)

/**
 * Returns the colour roles for a beverage key.
 *
 * - When [useBeverageColors] is true, preset beverages map to their [ExtendedColorScheme] family.
 * - Water uses the [ColorScheme.primary] family unless [useBeverageColorsForWater] is true.
 * - When [useBeverageColors] is false, non-water beverages use the [ColorScheme.tertiary] family,
 *   matching the legacy look.
 * - Custom or unknown beverages fall back to the [ColorScheme.tertiary] family.
 */
fun ExtendedColorScheme.beverageColorRoles(
    beverageKey: String,
    colorScheme: ColorScheme,
    useBeverageColors: Boolean = true,
    useBeverageColorsForWater: Boolean = false
): BeverageColorRoles {
    val beverage = BeverageType.fromStringOrDefault(beverageKey)

    if (!useBeverageColors && beverage != BeverageType.WATER) {
        return BeverageColorRoles(
            color = colorScheme.tertiary,
            onColor = colorScheme.onTertiary,
            containerColor = colorScheme.tertiaryContainer,
            onContainerColor = colorScheme.onTertiaryContainer
        )
    }

    return when {
        beverage == BeverageType.WATER && !useBeverageColorsForWater -> BeverageColorRoles(
            color = colorScheme.primary,
            onColor = colorScheme.onPrimary,
            containerColor = colorScheme.primaryContainer,
            onContainerColor = colorScheme.onPrimaryContainer
        )
        beverage == BeverageType.WATER -> BeverageColorRoles(
            color = water,
            onColor = onWater,
            containerColor = waterContainer,
            onContainerColor = onWaterContainer
        )
        beverage == BeverageType.COFFEE -> BeverageColorRoles(
            color = coffee,
            onColor = onCoffee,
            containerColor = coffeeContainer,
            onContainerColor = onCoffeeContainer
        )
        beverage == BeverageType.TEA -> BeverageColorRoles(
            color = tea,
            onColor = onTea,
            containerColor = teaContainer,
            onContainerColor = onTeaContainer
        )
        beverage == BeverageType.SOFT_DRINK -> BeverageColorRoles(
            color = softDrink,
            onColor = onSoftDrink,
            containerColor = softDrinkContainer,
            onContainerColor = onSoftDrinkContainer
        )
        beverage == BeverageType.ENERGY_DRINK -> BeverageColorRoles(
            color = energyDrink,
            onColor = onEnergyDrink,
            containerColor = energyDrinkContainer,
            onContainerColor = onEnergyDrinkContainer
        )
        beverage == BeverageType.SPORTS_DRINK -> BeverageColorRoles(
            color = sportsDrink,
            onColor = onSportsDrink,
            containerColor = sportsDrinkContainer,
            onContainerColor = onSportsDrinkContainer
        )
        beverage == BeverageType.ORAL_REHYDRATION_SOLUTION -> BeverageColorRoles(
            color = oralRehydrationSolution,
            onColor = onOralRehydrationSolution,
            containerColor = oralRehydrationSolutionContainer,
            onContainerColor = onOralRehydrationSolutionContainer
        )
        beverage == BeverageType.MILK -> BeverageColorRoles(
            color = milk,
            onColor = onMilk,
            containerColor = milkContainer,
            onContainerColor = onMilkContainer
        )
        beverage == BeverageType.FRUIT_JUICE -> BeverageColorRoles(
            color = fruitJuice,
            onColor = onFruitJuice,
            containerColor = fruitJuiceContainer,
            onContainerColor = onFruitJuiceContainer
        )
        else -> BeverageColorRoles(
            color = colorScheme.tertiary,
            onColor = colorScheme.onTertiary,
            containerColor = colorScheme.tertiaryContainer,
            onContainerColor = colorScheme.onTertiaryContainer
        )
    }
}

/**
 * Remembered variant of [beverageColorRoles] for use inside Composables.
 */
@Composable
fun rememberBeverageColorRoles(
    beverageKey: String,
    useBeverageColors: Boolean = true,
    useBeverageColorsForWater: Boolean = false
): BeverageColorRoles {
    val colorScheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColorScheme
    return remember(beverageKey, colorScheme, extended, useBeverageColors, useBeverageColorsForWater) {
        extended.beverageColorRoles(beverageKey, colorScheme, useBeverageColors, useBeverageColorsForWater)
    }
}
