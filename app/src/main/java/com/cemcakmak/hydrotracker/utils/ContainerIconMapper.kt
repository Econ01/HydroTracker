package com.cemcakmak.hydrotracker.utils

import com.cemcakmak.hydrotracker.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.DrawableRes

/**
 * Icon types for container presets
 */
enum class IconType {
    VECTOR,    // Material Icons (ImageVector)
    DRAWABLE   // Custom drawable resources
}

/**
 * Container icon configuration
 */
data class ContainerIcon(
    val type: IconType,
    val name: String,
    val vectorIcon: ImageVector? = null,
    @DrawableRes val drawableRes: Int? = null
)

/**
 * Utility for mapping container volumes to appropriate icons
 */
object ContainerIconMapper {

    /**
     * All available icons in order of volume (smallest to largest)
     */
    private val iconsByVolume = listOf(
        ContainerIcon(IconType.VECTOR, "LocalCafe", vectorIcon = Icons.Default.LocalCafe),
        ContainerIcon(IconType.DRAWABLE, "glass_cup", drawableRes = R.drawable.glass_cup_filled),
        ContainerIcon(IconType.DRAWABLE, "water_loss", drawableRes = R.drawable.water_loss_filled),
        ContainerIcon(IconType.DRAWABLE, "water_medium", drawableRes = R.drawable.water_medium_filled),
        ContainerIcon(IconType.DRAWABLE, "water_full", drawableRes = R.drawable.water_full_filled),
        ContainerIcon(IconType.DRAWABLE, "water_bottle", drawableRes = R.drawable.water_bottle_filled),
        ContainerIcon(IconType.DRAWABLE, "water_bottle_large", drawableRes = R.drawable.water_bottle_large_filled)
    )

    /**
     * Volume thresholds for icon selection (upper bounds)
     * Maps volume ranges to icons based on the plan:
     * - ≤125ml → LocalCafe
     * - 126-162ml → glass_cup
     * - 163-187ml → water_loss
     * - 188-250ml → water_medium
     * - 251-400ml → water_full
     * - 401-750ml → water_bottle
     * - >750ml → water_bottle_large
     */
    private val volumeThresholds = listOf(
        125.0 to 0,   // LocalCafe
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

    /**
     * Get the drawable resource ID for a given icon name (for DRAWABLE type)
     */
    fun getDrawableResId(iconName: String): Int? {
        return when (iconName) {
            "glass_cup" -> R.drawable.glass_cup_filled
            "water_loss" -> R.drawable.water_loss_filled
            "water_medium" -> R.drawable.water_medium_filled
            "water_full" -> R.drawable.water_full_filled
            "water_bottle" -> R.drawable.water_bottle_filled
            "water_bottle_large" -> R.drawable.water_bottle_large_filled
            else -> null
        }
    }

    /**
     * Get the ImageVector for a given icon name (for VECTOR type)
     */
    fun getVectorIcon(iconName: String): ImageVector? {
        return when (iconName) {
            "LocalCafe" -> Icons.Default.LocalCafe
            else -> null
        }
    }
}
