package com.cemcakmak.hydrotracker.data.models

import kotlinx.serialization.Serializable

/**
 * Home-screen widget preferences, configured from Appearance ▸ Widget.
 *
 * Persisted inside the app-preferences JSON document ([com.cemcakmak.hydrotracker.data.preferences.AppPreferences])
 * and mirrored into each widget's Glance state by `HydroWidgetUpdater`, so the widget itself
 * never reads this store directly.
 *
 * Surface overrides resolve per mode: a transparent background wins outright (the pure-black /
 * pure-white rows are hidden while it is on); pure black only affects dark mode, pure white
 * only light mode, so the two can be enabled independently.
 */
@Serializable
data class WidgetPreferences(
    val useDynamicColors: Boolean = true, // Material You colours; off = HYDRO_THEME palette
    val useTransparentBackground: Boolean = false, // Fully transparent surface, header hidden
    val usePureBlack: Boolean = false, // Dark-mode surface -> Color.Black
    val usePureWhite: Boolean = false, // Light-mode surface -> Color.White
    val pinnedQuickAddSlots: List<PinnedQuickAddSlot> = emptyList(), // Empty = all slots auto
)

/**
 * A quick-add card pinned to a fixed choice by the user (instead of the most-common combo
 * that would fill [slot] automatically). [beverageName] is a `BeverageType` enum name or a
 * custom beverage's name.
 */
@Serializable
data class PinnedQuickAddSlot(
    val slot: Int,
    val containerName: String,
    val volume: Double,
    val beverageName: String,
)
