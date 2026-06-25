package com.cemcakmak.hydrotracker.data.models

import com.cemcakmak.hydrotracker.R
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter

/**
 * Predefined container sizes for quick water logging
 * Updated with new volumes and Material Icons
 *
 * [name] is the stable English identifier used for storage and matching.
 * [labelResId] is the user-facing localized name; use it in the UI when non-zero.
 */
data class ContainerPreset(
    val id: Long = 0,
    val name: String,
    val volume: Double,
    val isDefault: Boolean = false,
    @param:DrawableRes val iconRes: Int? = null,
    val iconType: String = "",
    val iconName: String = "",
    val isCustom: Boolean = false,
    @param:StringRes val labelResId: Int = 0
) {
    /**
     * Returns the preset volume formatted in the user's preferred [volumeUnit].
     * Internal storage continues to use millilitres.
     */
    fun getFormattedVolume(context: Context, volumeUnit: VolumeUnit): String {
        return VolumeUnitConverter.format(context, volume, volumeUnit)
    }

    companion object {
        fun getDefaultPresets(): List<ContainerPreset> {
            return listOf(
                ContainerPreset(
                    id = 1,
                    name = "Coffee Cup",
                    volume = 100.0,
                    isDefault = true,
                    iconRes = R.drawable.local_cafe_filled,
                    iconType = "DRAWABLE",
                    iconName = "local_cafe",
                    labelResId = R.string.container_coffee_cup
                ),
                ContainerPreset(
                    id = 2,
                    name = "Tea Cup",
                    volume = 150.0,
                    isDefault = true,
                    iconRes = R.drawable.glass_cup_filled,
                    iconType = "DRAWABLE",
                    iconName = "glass_cup",
                    labelResId = R.string.container_tea_cup
                ),
                ContainerPreset(
                    id = 3,
                    name = "Small Cup",
                    volume = 175.0,
                    isDefault = true,
                    iconRes = R.drawable.water_loss_filled,
                    iconType = "DRAWABLE",
                    iconName = "water_loss",
                    labelResId = R.string.container_small_cup
                ),
                ContainerPreset(
                    id = 4,
                    name = "Medium Glass",
                    volume = 200.0,
                    isDefault = true,
                    iconRes = R.drawable.water_medium_filled,
                    iconType = "DRAWABLE",
                    iconName = "water_medium",
                    labelResId = R.string.container_medium_glass
                ),
                ContainerPreset(
                    id = 5,
                    name = "Large Glass",
                    volume = 300.0,
                    isDefault = true,
                    iconRes = R.drawable.water_full_filled,
                    iconType = "DRAWABLE",
                    iconName = "water_full",
                    labelResId = R.string.container_large_glass
                ),
                ContainerPreset(
                    id = 6,
                    name = "Water Bottle",
                    volume = 500.0,
                    isDefault = true,
                    iconRes = R.drawable.water_bottle_filled,
                    iconType = "DRAWABLE",
                    iconName = "water_bottle",
                    labelResId = R.string.container_water_bottle
                ),
                ContainerPreset(
                    id = 7,
                    name = "Large Bottle",
                    volume = 1000.0,
                    isDefault = true,
                    iconRes = R.drawable.water_bottle_large_filled,
                    iconType = "DRAWABLE",
                    iconName = "water_bottle_large",
                    labelResId = R.string.container_large_bottle
                )
            )
        }
    }
}