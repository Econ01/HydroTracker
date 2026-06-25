package com.cemcakmak.hydrotracker.utils

import com.cemcakmak.hydrotracker.R
import androidx.annotation.DrawableRes

/**
 * Icon types for container presets
 */
enum class IconType {
    DRAWABLE   // Custom drawable resources
}

/**
 * Container icon configuration. Both checked (filled) and unchecked (outlined) drawable
 * resources are provided so the icon can be used in selectable UI such as toggle buttons.
 */
data class ContainerIcon(
    val type: IconType,
    val name: String,
    @DrawableRes val uncheckedRes: Int,
    @DrawableRes val checkedRes: Int
)

/**
 * Utility for mapping container volumes to appropriate icons
 */
object ContainerIconMapper {

    /**
     * All available icons in order of volume (smallest to largest)
     */
    private val iconsByVolume = listOf(
        ContainerIcon(
            IconType.DRAWABLE,
            "local_cafe",
            uncheckedRes = R.drawable.local_cafe,
            checkedRes = R.drawable.local_cafe_filled
        ),
        ContainerIcon(
            IconType.DRAWABLE,
            "glass_cup",
            uncheckedRes = R.drawable.glass_cup,
            checkedRes = R.drawable.glass_cup_filled
        ),
        ContainerIcon(
            IconType.DRAWABLE,
            "water_loss",
            uncheckedRes = R.drawable.water_loss,
            checkedRes = R.drawable.water_loss_filled
        ),
        ContainerIcon(
            IconType.DRAWABLE,
            "water_medium",
            uncheckedRes = R.drawable.water_medium,
            checkedRes = R.drawable.water_medium_filled
        ),
        ContainerIcon(
            IconType.DRAWABLE,
            "water_full",
            uncheckedRes = R.drawable.water_full,
            checkedRes = R.drawable.water_full_filled
        ),
        ContainerIcon(
            IconType.DRAWABLE,
            "water_bottle",
            uncheckedRes = R.drawable.water_bottle,
            checkedRes = R.drawable.water_bottle_filled
        ),
        ContainerIcon(
            IconType.DRAWABLE,
            "water_bottle_large",
            uncheckedRes = R.drawable.water_bottle_large,
            checkedRes = R.drawable.water_bottle_large_filled
        )
    )

    /**
     * Volume thresholds for icon selection (upper bounds)
     * Maps volume ranges to icons based on the plan:
     * - ≤125ml → local_café
     * - 126-162ml → glass_cup
     * - 163-187ml → water_loss
     * - 188-250ml → water_medium
     * - 251-400ml → water_full
     * - 401-750ml → water_bottle
     * - >750ml → water_bottle_large
     */
    private val volumeThresholds = listOf(
        125.0 to 0,   // local_cafe
        162.0 to 1,   // glass_cup
        187.0 to 2,   // water_loss
        250.0 to 3,   // water_medium
        400.0 to 4,   // water_full
        750.0 to 5,   // water_bottle
        Double.MAX_VALUE to 6  // water_bottle_large
    )

    /**
     * Get the appropriate icon for a given volume
     */
    fun getIconForVolume(volume: Double): ContainerIcon {
        for ((threshold, iconIndex) in volumeThresholds) {
            if (volume <= threshold) {
                return iconsByVolume[iconIndex]
            }
        }
        return iconsByVolume.last()
    }

    /**
     * Get icon by type and name
     */
    fun getIconByName(iconType: String, iconName: String): ContainerIcon? {
        return iconsByVolume.find {
            it.type.name == iconType && it.name == iconName
        }
    }

    /**
     * Get all available icons (useful for icon picker)
     */
    fun getAllIcons(): List<ContainerIcon> = iconsByVolume
}
