package com.cemcakmak.hydrotracker.presentation.common

import androidx.annotation.DrawableRes
import com.cemcakmak.hydrotracker.data.database.entities.CustomBeverageEntity
import com.cemcakmak.hydrotracker.data.models.BeveragePreferences
import com.cemcakmak.hydrotracker.data.models.BeverageType

/**
 * Unified beverage shown in the Home pickers — either a preset ([BeverageType]) or a
 * user-defined custom beverage. Lets the pickers list both behind one type.
 */
data class BeverageOption(
    val storageKey: String,   // BeverageType.name for presets; the custom's name for customs
    val displayName: String,
    @param:DrawableRes val iconRes: Int,
    @param:DrawableRes val iconResFilled: Int,
    val hydrationMultiplier: Double,
    val description: String?,
    val isCustom: Boolean
) {
    val isWater: Boolean get() = storageKey == BeverageType.WATER.name

    /** Multiplier persisted on a logged entry: customs store theirs; presets stay enum-driven (null). */
    val storedMultiplier: Double? get() = if (isCustom) hydrationMultiplier else null
}

fun BeverageType.toOption(): BeverageOption = BeverageOption(
    storageKey = name,
    displayName = displayName,
    iconRes = iconRes,
    iconResFilled = iconResFilled,
    hydrationMultiplier = hydrationMultiplier,
    description = description,
    isCustom = false
)

fun CustomBeverageEntity.toOption(): BeverageOption {
    val icon = BeverageIcons.resFor(iconKey)
    return BeverageOption(
        storageKey = name,
        displayName = name,
        iconRes = icon,
        iconResFilled = icon,
        hydrationMultiplier = hydrationMultiplier,
        description = null,
        isCustom = true
    )
}

/**
 * The beverages offered in quick add: WATER first, then the visible presets + custom beverages
 * in [BeveragePreferences.orderedVisible] order.
 */
fun buildActiveBeverages(
    prefs: BeveragePreferences,
    customs: List<CustomBeverageEntity>
): List<BeverageOption> {
    val customById = customs.associateBy { it.id }
    val used = mutableSetOf<Long>()
    val result = mutableListOf<BeverageOption>()
    result.add(BeverageType.WATER.toOption())
    prefs.orderedVisible.forEach { token ->
        val customId = BeveragePreferences.customIdOrNull(token)
        if (customId != null) {
            customById[customId]?.let { result.add(it.toOption()); used.add(customId) }
        } else {
            BeverageType.entries.find { it.name == token }
                ?.takeIf { it != BeverageType.WATER }
                ?.let { result.add(it.toOption()) }
        }
    }
    customs.filter { it.id !in used }.forEach { result.add(it.toOption()) }
    return result
}
