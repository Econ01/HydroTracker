package com.cemcakmak.hydrotracker.utils

import android.os.Build

/**
 * Detects devices whose system font renderer is known to ignore or break
 * custom font styling (weights, roundness axes, etc.) for third-party apps.
 *
 * Currently, covers Xiaomi Group brands (Xiaomi, Redmi, POCO) running HyperOS
 * and similar skins, all of which share the same text-rendering pipeline.
 */
object OemFontWarning {

    private val AFFECTED_OEMS = setOf(
        "xiaomi",
        "redmi",
        "poco",
        "mi"
    )

    /** True when the current device is from an OEM known to break custom font styling. */
    val isAffectedDevice: Boolean
        get() = Build.MANUFACTURER.lowercase() in AFFECTED_OEMS ||
            Build.BRAND.lowercase() in AFFECTED_OEMS
}
