package com.cemcakmak.hydrotracker.data.models

data class BeveragePreferences(
    val orderedVisible: List<String>,  // BeverageType.name values, WATER excluded
    val hidden: Set<String>            // BeverageType.name values, WATER never here
) {
    fun toDisplayList(): List<BeverageType> {
        val visible = orderedVisible.mapNotNull { name ->
            BeverageType.entries.find { it.name == name }
        }
        return listOf(BeverageType.WATER) + visible
    }

    companion object {
        fun default(): BeveragePreferences {
            val defaults = BeverageType.getAllSorted()
                .filter { it != BeverageType.WATER }
                .map { it.name }
            return BeveragePreferences(orderedVisible = defaults, hidden = emptySet())
        }
    }
}
