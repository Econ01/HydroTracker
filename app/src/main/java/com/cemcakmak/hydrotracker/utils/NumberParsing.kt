package com.cemcakmak.hydrotracker.utils

import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale

/**
 * Parses a numeric string formatted for [locale] into a [Double].
 * Returns null if the text is blank, contains only a grouping separator,
 * or cannot be fully parsed as a number in the given locale.
 *
 * Unlike NumberFormat.parse() alone, this rejects partial parses:
 * a string like "1,250abc" or "1 250,5" parsed with the wrong locale
 * would otherwise silently return a truncated value (e.g. 1.0) instead
 * of failing.
 */
fun parseLocaleNumber(
    text: String,
    locale: Locale = Locale.getDefault()
): Double? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null

    val format = NumberFormat.getNumberInstance(locale)
    val pos = ParsePosition(0)
    val result = format.parse(trimmed, pos)

    // Reject if parsing failed, or didn't consume the whole string
    // (e.g. wrong separator, trailing junk, "1,250abc").
    if (result == null || pos.index != trimmed.length) return null

    return result.toDouble()
}
