package com.cemcakmak.hydrotracker.data.models

import kotlinx.serialization.Serializable

@Serializable
data class BeveragePreferences(
    // Display-order tokens, WATER excluded. Each token is either a BeverageType.name (preset)
    // or a "custom:<id>" token referencing a CustomBeverageEntity.
    val orderedVisible: List<String> = emptyList(),
    val hidden: Set<String> = emptySet()   // BeverageType.name values only (customs are deleted, never hidden)
) {
    /** Preset beverages shown on Home. Custom tokens are intentionally ignored here (not yet loggable). */
    fun toDisplayList(): List<BeverageType> {
        val visible = orderedVisible.mapNotNull { name ->
            BeverageType.entries.find { it.name == name }
        }
        return listOf(BeverageType.WATER) + visible
    }

    companion object {
        private const val CUSTOM_PREFIX = "custom:"

        /** Token used inside [orderedVisible] to reference a custom beverage by id. */
        fun customToken(id: Long): String = "$CUSTOM_PREFIX$id"

        fun isCustomToken(token: String): Boolean = token.startsWith(CUSTOM_PREFIX)

        /** The custom beverage id for a token, or null if it is a preset token. */
        fun customIdOrNull(token: String): Long? =
            if (isCustomToken(token)) token.removePrefix(CUSTOM_PREFIX).toLongOrNull() else null

        fun default(): BeveragePreferences {
            val defaults = BeverageType.getAllSorted()
                .filter { it != BeverageType.WATER }
                .map { it.name }
            return BeveragePreferences(orderedVisible = defaults, hidden = emptySet())
        }
    }
}
