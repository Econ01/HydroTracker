package com.cemcakmak.hydrotracker.utils

import android.content.Context
import androidx.annotation.StringRes
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import java.text.NumberFormat
import java.util.Locale

/**
 * Converts hydration amounts between millilitres (the internal unit) and the user's preferred
 * display unit. All calculations and database storage continue to use millilitres.
 */
object VolumeUnitConverter {

    /**
     * A family-aware scale preference named after metric units.
     *
     * Each value maps to the closest equivalent unit in the user's selected [VolumeUnit] family.
     * For example, [LITRE] maps to litres for metric users and gallons for US/Imperial users.
     */
    enum class MetricEquivalent {
        MILLILITRE,
        LITRE,
        KILOLITRE,
        MEGALITRE,
        GIGALITRE
    }

    /**
     * A concrete unit used only for rendering.
     *
     * [VolumeUnit] represents the user's preference and input unit; [DisplayUnit] represents any
     * unit the formatter may auto-select or a caller may force (e.g. litres, gallons).
     */
    enum class DisplayUnit(
        @StringRes val shortLabelResId: Int,
        val toMillilitresFactor: Double,
        val maximumFractionDigits: Int
    ) {
        MILLILITRE(R.string.unit_ml_short, 1.0, 0),
        LITRE(R.string.unit_litre_short, 1_000.0, 1),
        KILOLITRE(R.string.unit_kilolitre_short, 1_000_000.0, 1),
        MEGALITRE(R.string.unit_megalitre_short, 1_000_000_000.0, 1),
        GIGALITRE(R.string.unit_gigalitre_short, 1_000_000_000_000.0, 1),

        US_FLUID_OUNCE(R.string.unit_us_fl_oz_short, 29.5735, 1),
        US_PINT(R.string.unit_us_pint_short, 473.176, 1),
        US_QUART(R.string.unit_us_quart_short, 946.353, 1),
        US_GALLON(R.string.unit_us_gallon_short, 3_785.41, 1),

        IMPERIAL_FLUID_OUNCE(R.string.unit_imperial_fl_oz_short, 28.4131, 1),
        IMPERIAL_PINT(R.string.unit_imperial_pint_short, 568.261, 1),
        IMPERIAL_QUART(R.string.unit_imperial_quart_short, 1_136.52, 1),
        IMPERIAL_GALLON(R.string.unit_imperial_gallon_short, 4_546.09, 1)
    }

    /** Converts a value in the given [unit] to millilitres. */
    fun toMillilitres(value: Double, unit: VolumeUnit): Double {
        return value * unit.toMillilitresFactor
    }

    /**
     * Maps a [MetricEquivalent] scale preference to a concrete [DisplayUnit] for the given
     * [unit] family.
     */
    fun MetricEquivalent.toDisplayUnit(unit: VolumeUnit): DisplayUnit {
        return when (unit) {
            VolumeUnit.MILLILITRES -> when (this) {
                MetricEquivalent.MILLILITRE -> DisplayUnit.MILLILITRE
                MetricEquivalent.LITRE -> DisplayUnit.LITRE
                MetricEquivalent.KILOLITRE -> DisplayUnit.KILOLITRE
                MetricEquivalent.MEGALITRE -> DisplayUnit.MEGALITRE
                MetricEquivalent.GIGALITRE -> DisplayUnit.GIGALITRE
            }
            VolumeUnit.US_FLUID_OUNCE -> when (this) {
                MetricEquivalent.MILLILITRE -> DisplayUnit.US_FLUID_OUNCE
                else -> DisplayUnit.US_GALLON
            }
            VolumeUnit.IMPERIAL_FLUID_OUNCE -> when (this) {
                MetricEquivalent.MILLILITRE -> DisplayUnit.IMPERIAL_FLUID_OUNCE
                else -> DisplayUnit.IMPERIAL_GALLON
            }
        }
    }

    /**
     * Selects the most readable [DisplayUnit] for [millilitres] within the family of [unit].
     *
     * The selected unit is the largest one whose converted value is at least one. This keeps very
     * large totals readable while preserving whole-number precision for small everyday amounts.
     */
    fun selectDisplayUnit(millilitres: Double, unit: VolumeUnit): DisplayUnit {
        return when (unit) {
            VolumeUnit.MILLILITRES -> selectMetricUnit(millilitres)
            VolumeUnit.US_FLUID_OUNCE -> selectUsUnit(millilitres)
            VolumeUnit.IMPERIAL_FLUID_OUNCE -> selectImperialUnit(millilitres)
        }
    }

    /**
     * Formats an amount in millilitres without a unit label.
     *
     * The value is converted to [displayUnit] and formatted with that unit's usual precision
     * (whole numbers for millilitres, one decimal place otherwise).
     */
    fun formatValue(millilitres: Double, displayUnit: DisplayUnit): String {
        val converted = millilitres / displayUnit.toMillilitresFactor
        return NumberFormat.getNumberInstance(Locale.getDefault())
            .apply { maximumFractionDigits = displayUnit.maximumFractionDigits }
            .format(converted)
    }

    /** Formats an amount in millilitres without a unit label, using the user's base [unit]. */
    fun formatValue(millilitres: Double, unit: VolumeUnit): String {
        return formatValue(millilitres, displayUnitForVolumeUnit(unit))
    }

    /**
     * Formats an amount in millilitres without a unit label, using the [metricEquivalent] scale
     * mapped to the user's [unit] family.
     */
    fun formatValue(
        millilitres: Double,
        metricEquivalent: MetricEquivalent,
        unit: VolumeUnit
    ): String {
        return formatValue(millilitres, metricEquivalent.toDisplayUnit(unit))
    }

    /**
     * Formats an amount in millilitres for display.
     *
     * If [metricEquivalent] is provided, the formatter uses the equivalent unit within the user's
     * [unit] family. Otherwise it selects the most readable unit automatically.
     */
    fun format(
        context: Context,
        millilitres: Double,
        unit: VolumeUnit,
        metricEquivalent: MetricEquivalent? = null
    ): String {
        val displayUnit = metricEquivalent?.toDisplayUnit(unit) ?: selectDisplayUnit(millilitres, unit)
        val value = formatValue(millilitres, displayUnit)
        val unitLabel = context.getString(displayUnit.shortLabelResId)
        return "$value $unitLabel"
    }

    /**
     * Infers a sensible default display unit from a locale.
     *
     * The United States defaults to US fluid ounces; the United Kingdom defaults to Imperial fluid
     * ounces; all other locales default to millilitres.
     */
    fun defaultUnitForLocale(locale: Locale = Locale.getDefault()): VolumeUnit {
        return when (locale.country.uppercase()) {
            "US" -> VolumeUnit.US_FLUID_OUNCE
            "GB", "UK" -> VolumeUnit.IMPERIAL_FLUID_OUNCE
            else -> VolumeUnit.MILLILITRES
        }
    }

    private fun displayUnitForVolumeUnit(unit: VolumeUnit): DisplayUnit {
        return when (unit) {
            VolumeUnit.MILLILITRES -> DisplayUnit.MILLILITRE
            VolumeUnit.US_FLUID_OUNCE -> DisplayUnit.US_FLUID_OUNCE
            VolumeUnit.IMPERIAL_FLUID_OUNCE -> DisplayUnit.IMPERIAL_FLUID_OUNCE
        }
    }

    private fun selectMetricUnit(millilitres: Double): DisplayUnit {
        return when {
            millilitres < 1_000.0 -> DisplayUnit.MILLILITRE
            millilitres < 1_000_000.0 -> DisplayUnit.LITRE
            millilitres < 1_000_000_000.0 -> DisplayUnit.KILOLITRE
            millilitres < 1_000_000_000_000.0 -> DisplayUnit.MEGALITRE
            else -> DisplayUnit.GIGALITRE
        }
    }

    private fun selectUsUnit(millilitres: Double): DisplayUnit {
        return when {
            millilitres < 473.176 -> DisplayUnit.US_FLUID_OUNCE
            millilitres < 946.353 -> DisplayUnit.US_PINT
            millilitres < 3_785.41 -> DisplayUnit.US_QUART
            else -> DisplayUnit.US_GALLON
        }
    }

    private fun selectImperialUnit(millilitres: Double): DisplayUnit {
        return when {
            millilitres < 568.261 -> DisplayUnit.IMPERIAL_FLUID_OUNCE
            millilitres < 1_136.52 -> DisplayUnit.IMPERIAL_PINT
            millilitres < 4_546.09 -> DisplayUnit.IMPERIAL_QUART
            else -> DisplayUnit.IMPERIAL_GALLON
        }
    }
}
