package com.cemcakmak.hydrotracker.widgets

import androidx.compose.ui.unit.DpSize

/** Content variants for the adaptive hydration widget. */
internal enum class HydrationWidgetLayout {
    RING_ONLY,
    LINEAR_ONLY,
    LINEAR_WITH_ACTION,
    RING_WITH_TWO_ACTIONS,
    RING_WITH_THREE_ACTIONS,
}

/** Number of actions exposed by the adaptive quick-actions widget. */
internal enum class QuickAddWidgetLayout(val actionCount: Int) {
    SINGLE(1),
    TWO(2),
    THREE(3),
}

// Area thresholds approximate launcher cell counts while remaining orientation-independent. The
// reference Pixel sizes in Android's widget guidance are roughly 6k dp² per cell in both orientations.
private const val SINGLE_CELL_MAX_AREA = 10_000f
private const val COMPACT_MULTICELL_MAX_AREA = 40_000f
private const val SQUARE_COMPACT_MAX_AREA = 45_000f
private const val SHORT_HEIGHT = 140f

/**
 * Selects hydration content from actual host-supplied bounds.
 *
 * Launcher cells have no universal dp size and Glance does not expose cell spans, so these buckets
 * deliberately describe useful layouts rather than promising an exact grid classification. The
 * very small and short cases come first; roomy layouts retain the existing ring and three cards.
 */
internal fun resolveHydrationWidgetLayout(size: DpSize): HydrationWidgetLayout {
    val width = size.width.value
    val height = size.height.value
    if (width <= 0f || height <= 0f) return HydrationWidgetLayout.RING_ONLY

    val area = width * height
    val aspectRatio = width / height

    // Older launchers can report a one-row widget as only 40dp high. Width thresholds keep this
    // bucket monotonic and leave enough room for progress before adding an action.
    if (height < 80f && aspectRatio > 2.6f) {
        return if (width < 270f) {
            HydrationWidgetLayout.LINEAR_ONLY
        } else {
            HydrationWidgetLayout.LINEAR_WITH_ACTION
        }
    }

    if (area < SINGLE_CELL_MAX_AREA) return HydrationWidgetLayout.RING_ONLY

    // Short non-square bounds form one continuous bucket; do not let a near-square aspect ratio
    // cause an action-count cliff when width changes by a fraction of a dp.
    if (height < SHORT_HEIGHT && width != height) {
        val actionWidthThreshold = if (height < 80f) 270f else 200f
        return if (width < actionWidthThreshold) {
            HydrationWidgetLayout.LINEAR_ONLY
        } else {
            HydrationWidgetLayout.LINEAR_WITH_ACTION
        }
    }

    // Exact-square metadata/design sizes (notably 110x110) still use the square ring layout.
    if (aspectRatio in 0.8f..1.25f && minOf(width, height) >= 80f) {
        return if (area < SQUARE_COMPACT_MAX_AREA) {
            HydrationWidgetLayout.RING_WITH_TWO_ACTIONS
        } else {
            HydrationWidgetLayout.RING_WITH_THREE_ACTIONS
        }
    }

    // A tall one-column widget cannot accommodate useful side-by-side actions.
    if (width < 100f) return HydrationWidgetLayout.RING_ONLY

    return if (area < COMPACT_MULTICELL_MAX_AREA) {
        HydrationWidgetLayout.RING_WITH_TWO_ACTIONS
    } else {
        HydrationWidgetLayout.RING_WITH_THREE_ACTIONS
    }
}

/** Selects one to three shared quick-add slots from the available quick-action widget bounds. */
internal fun resolveQuickAddWidgetLayout(size: DpSize): QuickAddWidgetLayout {
    val width = size.width.value
    val height = size.height.value
    if (width <= 0f || height <= 0f) return QuickAddWidgetLayout.SINGLE

    val area = width * height
    val aspectRatio = width / height

    if (height < 80f && aspectRatio > 2.6f) {
        return if (width < 270f) QuickAddWidgetLayout.TWO else QuickAddWidgetLayout.THREE
    }

    if (area < SINGLE_CELL_MAX_AREA) return QuickAddWidgetLayout.SINGLE
    if (height < SHORT_HEIGHT && width != height) {
        return if (width < 270f) QuickAddWidgetLayout.TWO else QuickAddWidgetLayout.THREE
    }
    if (aspectRatio in 0.8f..1.25f && minOf(width, height) >= 80f) {
        return QuickAddWidgetLayout.THREE
    }
    if (width < 100f) return QuickAddWidgetLayout.TWO
    return QuickAddWidgetLayout.THREE
}
