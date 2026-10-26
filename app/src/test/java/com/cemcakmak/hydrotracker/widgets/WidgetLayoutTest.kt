package com.cemcakmak.hydrotracker.widgets

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies layout buckets against Android's reference sizes and legacy small-widget envelopes. */
class WidgetLayoutTest {

    @Test
    fun `hydration uses ring only for one-cell and tall narrow bounds`() {
        assertHydration(57, 102, HydrationWidgetLayout.RING_ONLY)
        assertHydration(127, 51, HydrationWidgetLayout.RING_ONLY)
        assertHydration(40, 40, HydrationWidgetLayout.RING_ONLY)
        assertHydration(57, 220, HydrationWidgetLayout.RING_ONLY)
    }

    @Test
    fun `hydration uses linear progress only for short two-cell bounds`() {
        assertHydration(130, 102, HydrationWidgetLayout.LINEAR_ONLY)
        assertHydration(269, 51, HydrationWidgetLayout.LINEAR_ONLY)
        assertHydration(110, 40, HydrationWidgetLayout.LINEAR_ONLY)
        assertHydration(180, 40, HydrationWidgetLayout.LINEAR_ONLY)
        assertHydration(195, 107, HydrationWidgetLayout.LINEAR_ONLY)
    }

    @Test
    fun `hydration adds one action to wider short bounds`() {
        assertHydration(203, 102, HydrationWidgetLayout.LINEAR_WITH_ACTION)
        assertHydration(412, 51, HydrationWidgetLayout.LINEAR_WITH_ACTION)
        assertHydration(276, 102, HydrationWidgetLayout.LINEAR_WITH_ACTION)
        assertHydration(292, 107, HydrationWidgetLayout.LINEAR_WITH_ACTION)
        assertHydration(270, 60, HydrationWidgetLayout.LINEAR_WITH_ACTION)
        assertHydration(300, 60, HydrationWidgetLayout.LINEAR_WITH_ACTION)
        assertHydration(425, 40, HydrationWidgetLayout.LINEAR_WITH_ACTION)
    }

    @Test
    fun `hydration uses two actions for compact square and multirow bounds`() {
        assertHydration(100, 100, HydrationWidgetLayout.RING_WITH_TWO_ACTIONS)
        assertHydration(110, 110, HydrationWidgetLayout.RING_WITH_TWO_ACTIONS)
        assertHydration(180, 180, HydrationWidgetLayout.RING_WITH_TWO_ACTIONS)
        assertHydration(130, 220, HydrationWidgetLayout.RING_WITH_TWO_ACTIONS)
    }

    @Test
    fun `hydration retains three actions for roomy bounds`() {
        assertHydration(250, 180, HydrationWidgetLayout.RING_WITH_THREE_ACTIONS)
        assertHydration(330, 180, HydrationWidgetLayout.RING_WITH_THREE_ACTIONS)
        assertHydration(276, 455, HydrationWidgetLayout.RING_WITH_THREE_ACTIONS)
        assertHydration(330, 330, HydrationWidgetLayout.RING_WITH_THREE_ACTIONS)
    }

    @Test
    fun `quick add shows one action only in one-cell bounds`() {
        assertQuickAdd(57, 102, QuickAddWidgetLayout.SINGLE)
        assertQuickAdd(127, 51, QuickAddWidgetLayout.SINGLE)
        assertQuickAdd(40, 40, QuickAddWidgetLayout.SINGLE)
    }

    @Test
    fun `quick add shows two actions in short two-cell and tall narrow bounds`() {
        assertQuickAdd(130, 102, QuickAddWidgetLayout.TWO)
        assertQuickAdd(269, 51, QuickAddWidgetLayout.TWO)
        assertQuickAdd(110, 40, QuickAddWidgetLayout.TWO)
        assertQuickAdd(180, 40, QuickAddWidgetLayout.TWO)
        assertQuickAdd(203, 102, QuickAddWidgetLayout.TWO)
        assertQuickAdd(195, 107, QuickAddWidgetLayout.TWO)
        assertQuickAdd(57, 220, QuickAddWidgetLayout.TWO)
    }

    @Test
    fun `quick add shows three actions when space permits`() {
        assertQuickAdd(276, 102, QuickAddWidgetLayout.THREE)
        assertQuickAdd(292, 107, QuickAddWidgetLayout.THREE)
        assertQuickAdd(412, 51, QuickAddWidgetLayout.THREE)
        assertQuickAdd(425, 40, QuickAddWidgetLayout.THREE)
        assertQuickAdd(100, 100, QuickAddWidgetLayout.THREE)
        assertQuickAdd(110, 110, QuickAddWidgetLayout.THREE)
        assertQuickAdd(180, 180, QuickAddWidgetLayout.THREE)
        assertQuickAdd(330, 180, QuickAddWidgetLayout.THREE)
    }

    @Test
    fun `short wide layouts never lose actions as width increases`() {
        assertEquals(
            resolveHydrationWidgetLayout(size(249, 40)),
            resolveHydrationWidgetLayout(size(250, 40)),
        )
        assertEquals(
            resolveQuickAddWidgetLayout(size(249, 40)),
            resolveQuickAddWidgetLayout(size(250, 40)),
        )
    }

    @Test
    fun `near square short bounds do not change action counts at the aspect cliff`() {
        assertHydration(127, 102, HydrationWidgetLayout.LINEAR_ONLY)
        assertHydration(128, 102, HydrationWidgetLayout.LINEAR_ONLY)
        assertQuickAdd(127, 102, QuickAddWidgetLayout.TWO)
        assertQuickAdd(128, 102, QuickAddWidgetLayout.TWO)
        assertHydration(80, 125, HydrationWidgetLayout.LINEAR_ONLY)
    }

    @Test
    fun `invalid bounds use safe minimal layouts`() {
        assertHydration(0, 0, HydrationWidgetLayout.RING_ONLY)
        assertQuickAdd(0, 0, QuickAddWidgetLayout.SINGLE)
    }

    private fun assertHydration(width: Int, height: Int, expected: HydrationWidgetLayout) {
        assertEquals("${width}x$height", expected, resolveHydrationWidgetLayout(size(width, height)))
    }

    private fun assertQuickAdd(width: Int, height: Int, expected: QuickAddWidgetLayout) {
        assertEquals("${width}x$height", expected, resolveQuickAddWidgetLayout(size(width, height)))
    }

    private fun size(width: Int, height: Int) = DpSize(width.dp, height.dp)
}
