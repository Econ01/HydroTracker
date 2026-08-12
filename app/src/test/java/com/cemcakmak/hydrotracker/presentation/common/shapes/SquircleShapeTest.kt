package com.cemcakmak.hydrotracker.presentation.common.shapes

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [SquircleShape] outline generation with degenerate sizes.
 *
 * Consumers such as carousel `maskClip` can momentarily supply zero, negative or non-finite
 * sizes while an item scrolls out of view; the shape must emit an empty outline instead of
 * throwing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SquircleShapeTest {

    private val density = Density(1f)

    private fun outlineFor(size: Size) = SquircleShape().createOutline(
        size = size,
        layoutDirection = LayoutDirection.Ltr,
        density = density
    )

    @Test fun `zero size returns an outline without throwing`() {
        assertNotNull(outlineFor(Size.Zero))
    }

    @Test fun `zero width returns an outline without throwing`() {
        assertNotNull(outlineFor(Size(0f, 150f)))
    }

    @Test fun `zero height returns an outline without throwing`() {
        assertNotNull(outlineFor(Size(150f, 0f)))
    }

    @Test fun `negative width returns an outline without throwing`() {
        assertNotNull(outlineFor(Size(-12f, 150f)))
    }

    @Test fun `negative height returns an outline without throwing`() {
        assertNotNull(outlineFor(Size(150f, -12f)))
    }

    @Test fun `NaN size returns an outline without throwing`() {
        assertNotNull(outlineFor(Size(Float.NaN, Float.NaN)))
    }

    @Test fun `infinite size returns an outline without throwing`() {
        assertNotNull(outlineFor(Size(Float.POSITIVE_INFINITY, 150f)))
    }

    @Test fun `normal size returns an outline without throwing`() {
        assertNotNull(outlineFor(Size(150f, 150f)))
    }
}
