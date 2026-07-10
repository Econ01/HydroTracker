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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
