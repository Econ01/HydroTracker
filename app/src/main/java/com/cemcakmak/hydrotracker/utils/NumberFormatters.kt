/*
 *
 *  * HydroTracker - A modern and private water intake tracking application
 *  * Copyright (c) 2026 Ali Cem Çakmak
 *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.cemcakmak.hydrotracker.utils

import android.icu.text.CompactDecimalFormat
import android.icu.text.CompactDecimalFormat.CompactStyle
import java.util.Locale

/**
 * Locale-aware number formatters shared across the app.
 */
private val COMPACT_NUMBER_REGEX = Regex("^([\\d\\s.,]+)(.*)$")

object NumberFormatters {

    /**
     * Formats a count for hero-style display.
     *
     * Values below 1,000 are shown as plain integers. Values at or above 1,000 are shown in
     * compact notation with one decimal place (e.g. 8,888 -> "8.9K").
     */
    fun formatCompactCount(value: Double, maxFractionDigits: Int = 1): String {
        if (value < 1000.0) {
            return value.toInt().toString()
        }
        return CompactDecimalFormat.getInstance(Locale.getDefault(), CompactStyle.SHORT)
            .apply { maximumSignificantDigits = maxFractionDigits + 1 }
            .format(value)
    }

    /**
     * Splits a compact-formatted count into its numeric and suffix components.
     *
     * This is useful when the suffix (e.g. "K", "M") needs to be styled separately from the
     * number itself.
     */
    fun formatCompactCountParts(value: Double, maxFractionDigits: Int = 1): CompactNumberParts {
        val formatted = formatCompactCount(value, maxFractionDigits)
        val match = COMPACT_NUMBER_REGEX.find(formatted.trim())
        return if (match != null) {
            CompactNumberParts(
                numericPart = match.groupValues[1].trim(),
                suffix = match.groupValues[2].trim()
            )
        } else {
            CompactNumberParts(numericPart = formatted, suffix = "")
        }
    }
}

/**
 * Numeric and suffix components of a compact-formatted count.
 */
data class CompactNumberParts(
    val numericPart: String,
    val suffix: String
)
