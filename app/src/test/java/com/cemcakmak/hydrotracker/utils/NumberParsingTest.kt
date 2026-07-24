package com.cemcakmak.hydrotracker.utils

import org.junit.Assert.assertEquals
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
        assertEquals(1250.0, parseLocaleNumber("1,250", Locale.US))
    }

    @Test fun `US plain integer parses correctly`() {
        assertEquals(1250.0, parseLocaleNumber("1250", Locale.US))
    }

    @Test fun `US dot decimal parses correctly - no regression`() {
        assertEquals(1.5, parseLocaleNumber("1.5", Locale.US))
    }

    @Test fun `Turkish comma decimal parses correctly`() {
        assertEquals(1.5, parseLocaleNumber("1,5", Locale("tr", "TR")))
    }

    @Test fun `German dot grouping and comma decimal parse correctly`() {
        assertEquals(1250.5, parseLocaleNumber("1.250,5", Locale.GERMANY))
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
        assertEquals(1250.5, parseLocaleNumber(input, Locale.FRANCE))
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
}
