package com.cemcakmak.hydrotracker.data.models

import kotlinx.serialization.Serializable

/**
 * Identifies where a water intake entry originated.
 *
 * This is the source of truth for sync behaviour:
 * - [LOCAL] entries are created in HydroTracker and are mirrored to Health Connect.
 * - [HEALTH_CONNECT_EXTERNAL] entries are imported from other health apps and are read-only.
 * - [HEALTH_CONNECT_RESTORED] entries are HydroTracker's own records restored from Health Connect.
 */
@Serializable
enum class EntrySource {
    LOCAL,
    HEALTH_CONNECT_EXTERNAL,
    HEALTH_CONNECT_RESTORED
}
