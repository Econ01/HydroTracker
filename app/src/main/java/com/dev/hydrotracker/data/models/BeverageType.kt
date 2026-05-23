package com.dev.hydrotracker.data.models

import androidx.annotation.DrawableRes
import com.dev.hydrotracker.R

/**
 * Beverage types with hydration effectiveness multipliers based on peer-reviewed research.
 *
 * Categories and multipliers are based on "Hydration Effects of Common Beverages in Healthy Adults"
 * research study and Beverage Hydration Index (BHI) data.
 */
enum class BeverageType(
    val displayName: String,
    val hydrationMultiplier: Double,
    @param:DrawableRes val iconRes: Int,
    @param:DrawableRes val iconResFilled: Int,
    val description: String
) {
    // Water (baseline)
    WATER(
        displayName = "Water",
        hydrationMultiplier = 1.0,
        iconRes = R.drawable.water,
        iconResFilled = R.drawable.water_filled,
        description = "Pure water - baseline hydration"
    ),

    // Coffee (1.0x)
    COFFEE(
        displayName = "Coffee",
        hydrationMultiplier = 1.0,
        iconRes = R.drawable.coffee,
        iconResFilled = R.drawable.coffee_filled,
        description = "Coffee - equivalent to water"
    ),

    // Tea (1.0x)
    TEA(
        displayName = "Tea",
        hydrationMultiplier = 1.0,
        iconRes = R.drawable.tea,
        iconResFilled = R.drawable.tea_filled,
        description = "Tea - equivalent to water"
    ),

    // Soft Drink (1.0x)
    SOFT_DRINK(
        displayName = "Soft Drink",
        hydrationMultiplier = 1.0,
        iconRes = R.drawable.soft_drink,
        iconResFilled = R.drawable.soft_drink_filled,
        description = "Soft drink - equivalent to water"
    ),

    // Energy Drink (1.0x)
    ENERGY_DRINK(
        displayName = "Energy Drink",
        hydrationMultiplier = 1.0,
        iconRes = R.drawable.energy_drink,
        iconResFilled = R.drawable.energy_drink_filled,
        description = "Energy drink - equivalent to water"
    ),

    // Sports Drink (1.1x)
    SPORTS_DRINK(
        displayName = "Sports Drink",
        hydrationMultiplier = 1.1,
        iconRes = R.drawable.sports_drink,
        iconResFilled = R.drawable.sports_drink_filled,
        description = "Sports drink - slightly enhanced hydration"
    ),

    // Oral Rehydration Solution (1.5x)
    ORAL_REHYDRATION_SOLUTION(
        displayName = "ORS",
        hydrationMultiplier = 1.5,
        iconRes = R.drawable.oral_rehydration_solution,
        iconResFilled = R.drawable.oral_rehydration_solution_filled,
        description = "Oral rehydration solution - superior hydration"
    ),

    // Milk (1.5x)
    MILK(
        displayName = "Milk",
        hydrationMultiplier = 1.5,
        iconRes = R.drawable.milk,
        iconResFilled = R.drawable.milk_filled,
        description = "Milk - superior hydration"
    ),

    // Fruit Juice (1.3x)
    FRUIT_JUICE(
        displayName = "Fruit Juice",
        hydrationMultiplier = 1.3,
        iconRes = R.drawable.fruit_juice,
        iconResFilled = R.drawable.fruit_juice_filled,
        description = "Fruit juice - enhanced hydration"
    );

    companion object {
        /**
         * Get the default beverage type (Water)
         */
        fun getDefault(): BeverageType = WATER

        /**
         * Get all beverage types with Water first, then sorted by name
         */
        fun getAllSorted(): List<BeverageType> {
            val water = listOf(WATER)
            val others = entries.filter { it != WATER }.sortedBy { it.displayName }
            return water + others
        }

        /**
         * Find beverage type by display name (case-insensitive)
         */
        fun fromDisplayName(name: String): BeverageType? =
            entries.find { it.displayName.equals(name, ignoreCase = true) }

        /**
         * Get beverage type from string, defaulting to WATER if not found
         * First tries to match by enum name, then by display name
         */
        fun fromStringOrDefault(name: String?): BeverageType {
            if (name == null) return WATER

            // First try to find by enum name (for database compatibility)
            entries.find { it.name == name }?.let { return it }

            // Then try by display name (for UI compatibility)
            fromDisplayName(name)?.let { return it }

            // Handle legacy enum names for backwards compatibility
            return when (name) {
                "MILK_WHOLE", "MILK_SKIM" -> MILK
                "COFFEE_REGULAR", "COFFEE_DECAF" -> COFFEE
                "TEA_BLACK", "TEA_GREEN", "TEA_OOLONG", "TEA_HERBAL" -> TEA
                "SODA_COLA", "SODA_REGULAR", "SODA_CAFFEINE_FREE", "SODA_DIET_COLA", "SODA_DIET", "SODA_ZERO" -> SOFT_DRINK
                "JUICE_ORANGE", "JUICE_APPLE", "JUICE_FRUIT" -> FRUIT_JUICE
                "ENERGY_DRINK", "COCONUT_WATER", "FLAVORED_WATER", "SPARKLING_WATER" -> WATER // These are removed, default to water
                else -> WATER
            }
        }
    }
}