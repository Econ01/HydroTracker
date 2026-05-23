package com.dev.hydrotracker.data.models

import com.dev.hydrotracker.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.DrawableRes

/**
 * Predefined container sizes for quick water logging
 * Updated with new volumes and Material Icons
 */
data class ContainerPreset(
    val id: Long = 0,
    val name: String,
    val volume: Double,
    val isDefault: Boolean = false,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null,
    val isCustom: Boolean = false
) {
    fun getFormattedVolume(): String {
        return when {
            volume >= 1000 -> "${(volume / 1000).format(1)} L"
            else -> "${volume.toInt()} ml"
        }
    }

    companion object {
        fun getDefaultPresets(): List<ContainerPreset> {
            return listOf(
                ContainerPreset(
                    id = 1,
                    name = "Coffee Cup",
                    volume = 100.0,
                    isDefault = true,
                    icon = Icons.Default.LocalCafe
                ),
                ContainerPreset(
                    id = 2,
                    name = "Tea Cup",
                    volume = 150.0,
                    isDefault = true,
                    iconRes = R.drawable.glass_cup
                ),
                ContainerPreset(
                    id = 3,
                    name = "Small Cup",
                    volume = 175.0,
                    isDefault = true,
                    iconRes = R.drawable.water_loss
                ),
                ContainerPreset(
                    id = 4,
                    name = "Medium Glass",
                    volume = 200.0,
                    isDefault = true,
                    iconRes = R.drawable.water_medium
                ),
                ContainerPreset(
                    id = 5,
                    name = "Large Glass",
                    volume = 300.0,
                    isDefault = true,
                    iconRes = R.drawable.water_full
                ),
                ContainerPreset(
                    id = 6,
                    name = "Water Bottle",
                    volume = 500.0,
                    isDefault = true,
                    iconRes = R.drawable.water_bottle
                ),
                ContainerPreset(
                    id = 7,
                    name = "Large Bottle",
                    volume = 1000.0,
                    isDefault = true,
                    iconRes = R.drawable.water_bottle_large
                )
            )
        }
    }
}

// Extension function to format Double with specified decimal places
private fun Double.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}