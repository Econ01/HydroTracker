package com.cemcakmak.hydrotracker.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Unit tests for [parseLocaleNumber].
 */
class NumberParsingTest {
    @Test fun `US grouping comma parses correctly`() {
        val result = parseLocaleNumber("1,250", Locale.US)
        assertNotNull(result)
        assertEquals(1250.0, result!!, 0.0001)
    }

    @Test fun `US plain integer parses correctly`() {
        val result = parseLocaleNumber("1250", Locale.US)
        assertNotNull(result)
        assertEquals(1250.0, result!!, 0.0001)
    }

    @Test fun `US dot decimal parses correctly - no regression`() {
        val result = parseLocaleNumber("1.5", Locale.US)
        assertNotNull(result)
        assertEquals(1.5, result!!, 0.0001)
    }

    @Test fun `Turkish comma decimal parses correctly`() {
        val result = parseLocaleNumber("1,5", Locale.forLanguageTag("tr-TR"))
        assertNotNull(result)
        assertEquals(1.5, result!!, 0.0001)
    }

    @Test fun `German dot grouping and comma decimal parse correctly`() {
        val result = parseLocaleNumber("1.250,5", Locale.GERMANY)
        assertNotNull(result)
        assertEquals(1250.5, result!!, 0.0001)
    }

    @Test fun `non-numeric text returns null`() {
        assertNull(parseLocaleNumber("abc", Locale.US))
    }

    @Test fun `empty string returns null`() {
        assertNull(parseLocaleNumber("", Locale.US))
    }

    @Test fun `blank whitespace-only string returns null`() {
        assertNull(parseLocaleNumber("   ", Locale.US))
    }

    @Test fun `French locale with correct grouping separator and padding trims and parses`() {
        val groupingSep = (NumberFormat.getNumberInstance(Locale.FRANCE) as DecimalFormat)
            .decimalFormatSymbols.groupingSeparator
        val input = "  1${groupingSep}250,5  "
        val result = parseLocaleNumber(input, Locale.FRANCE)
        assertNotNull(result)
        assertEquals(1250.5, result!!, 0.0001)
    }

    @Test fun `partial parse with wrong separator returns null instead of truncating`() {
        assertNull(parseLocaleNumber("1 250,5", Locale.FRANCE))
    }

    @Test fun `trailing garbage after valid number returns null`() {
        assertNull(parseLocaleNumber("1,250abc", Locale.US))
    }

    @Test fun `grouping separator alone with no digits returns null`() {
        assertNull(parseLocaleNumber(",", Locale.US))
    }

    @Test fun `multiple grouping separators parse correctly`() {
        val result = parseLocaleNumber("1,234,567", Locale.US)
        assertNotNull(result)
        assertEquals(1_234_567.0, result!!, 0.0001)
    }

    @Test fun `decimal without leading zero parses correctly`() {
        val result = parseLocaleNumber(".5", Locale.US)
        assertNotNull(result)
        assertEquals(0.5, result!!, 0.0001)
    }

    @Test fun `zero parses correctly`() {
        val result = parseLocaleNumber("0", Locale.US)
        assertNotNull(result)
        assertEquals(0.0, result!!, 0.0001)
    }

    @Test fun `German decimal without leading zero parses correctly`() {
        val result = parseLocaleNumber(",5", Locale.GERMANY)
        assertNotNull(result)
        assertEquals(0.5, result!!, 0.0001)
    }
}
