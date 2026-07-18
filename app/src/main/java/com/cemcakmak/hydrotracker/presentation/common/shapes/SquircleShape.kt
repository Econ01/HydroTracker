package com.cemcakmak.hydrotracker.presentation.common.shapes

import android.graphics.Path as AndroidPath
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import kotlin.math.min

/**
 * A continuous-corner shape (squircle).
 *
 * Each corner can have an independent radius, specified with layout-direction-aware
 * names (`topStart`/`topEnd`/`bottomEnd`/`bottomStart`) that automatically flip in RTL.
 * [smoothing] controls how much the circular arc is extended into the edges: `0` gives a
 * regular rounded rectangle, `1` gives the maximum continuous-curve effect (the default).
 *
 * Size-adaptive smoothing is applied automatically: when the corner radius is very small
 * relative to the shape's shortest dimension, the effective smoothing is reduced because
 * the visual difference would be imperceptible.
 *
 * @param topStart    Corner radius for the top-start corner (top-left in LTR, top-right in RTL).
 * @param topEnd      Corner radius for the top-end corner (top-right in LTR, top-left in RTL).
 * @param bottomEnd   Corner radius for the bottom-end corner (bottom-right in LTR, bottom-left in RTL).
 * @param bottomStart Corner radius for the bottom-start corner (bottom-left in LTR, bottom-right in RTL).
 * @param smoothing   Curvature-smoothing factor in `[0, 1]`. Defaults to [DEFAULT_SMOOTHING].
 */
data class SquircleShape(
    val topStart: CornerSize,
    val topEnd: CornerSize,
    val bottomEnd: CornerSize,
    val bottomStart: CornerSize,
    val smoothing: Float = DEFAULT_SMOOTHING
) : Shape {

    /**
     * Convenience constructor that applies the same [cornerSize] to all four corners.
     */
    constructor(
        cornerSize: CornerSize = DEFAULT_CORNER_SIZE,
        smoothing: Float = DEFAULT_SMOOTHING
    ) : this(
        topStart = cornerSize,
        topEnd = cornerSize,
        bottomEnd = cornerSize,
        bottomStart = cornerSize,
        smoothing = smoothing
    )

    init {
        require(smoothing in 0f..1f) { "smoothing must be in the range [0, 1]" }
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val maxRadius = min(size.width, size.height) / 2f
        val isLtr = layoutDirection == LayoutDirection.Ltr

        // Resolve layout-direction-aware corners to absolute positions and clamp
        // each radius to min(width, height) / 2 to prevent rendering artefacts.
        val topLeftPx = (if (isLtr) topStart else topEnd)
            .toPx(size, density).coerceIn(0f, maxRadius)
        val topRightPx = (if (isLtr) topEnd else topStart)
            .toPx(size, density).coerceIn(0f, maxRadius)
        val bottomRightPx = (if (isLtr) bottomEnd else bottomStart)
            .toPx(size, density).coerceIn(0f, maxRadius)
        val bottomLeftPx = (if (isLtr) bottomStart else bottomEnd)
            .toPx(size, density).coerceIn(0f, maxRadius)

        // Reduce effective smoothing for shapes where the corner radius is very
        // small relative to the shape dimensions.
        val effectiveSmoothing = adaptSmoothing(
            smoothing = smoothing,
            maxRadius = maxOf(topLeftPx, topRightPx, bottomRightPx, bottomLeftPx),
            minDimension = min(size.width, size.height)
        )

        val cacheKey = CacheKey(
            size, topLeftPx, topRightPx, bottomRightPx, bottomLeftPx, effectiveSmoothing
        )
        cache[cacheKey]?.let { return it }

        // Build the smoothed rounded rectangle via graphics-shapes.
        // Vertex order for RoundedPolygon.rectangle(): 0 = top-left, 1 = top-right, 2 = bottom-right, 3 = bottom-left
        val polygon = RoundedPolygon.rectangle(
            width = size.width,
            height = size.height,
            perVertexRounding = listOf(
                CornerRounding(radius = bottomRightPx, smoothing = effectiveSmoothing),
                CornerRounding(radius = bottomLeftPx, smoothing = effectiveSmoothing),
                CornerRounding(radius = topLeftPx, smoothing = effectiveSmoothing),
                CornerRounding(radius = topRightPx, smoothing = effectiveSmoothing)
            ),
            centerX = size.width / 2f,
            centerY = size.height / 2f
        )

        val path = AndroidPath()
        polygon.toPath(path)
        val outline = Outline.Generic(path.asComposePath())

        cache[cacheKey] = outline
        return outline
    }

    private data class CacheKey(
        val size: Size,
        val topLeft: Float,
        val topRight: Float,
        val bottomRight: Float,
        val bottomLeft: Float,
        val smoothing: Float
    )

    companion object {
        /** Default smoothing value — maximum continuous-curve effect. */
        const val DEFAULT_SMOOTHING = 1f

        val DEFAULT_CORNER_SIZE = CornerSize(40.dp)

        private const val MAX_CACHE_SIZE = 8

        private val cache = object : LinkedHashMap<CacheKey, Outline>(
            MAX_CACHE_SIZE,
            0.75f,
            /* accessOrder = */ true
        ) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<CacheKey, Outline>?
            ): Boolean = size > MAX_CACHE_SIZE
        }

        /**
         * Reduces effective [smoothing] when the largest corner radius is very small
         * relative to [minDimension], where the visual difference is imperceptible.
         *
         * The smoothing scales linearly when the largest corner radius is below
         * [ADAPTIVE_THRESHOLD_FRACTION] of the shortest dimension. Above that
         * threshold, the full [smoothing] value is returned.
         */
        internal fun adaptSmoothing(
            smoothing: Float,
            maxRadius: Float,
            minDimension: Float
        ): Float {
            if (minDimension <= 0f || maxRadius <= 0f) return 0f
            val ratio = (maxRadius / (minDimension * ADAPTIVE_THRESHOLD_FRACTION))
                .coerceIn(0f, 1f)
            return smoothing * ratio
        }

        /**
         * Corner radii below this fraction of the shortest dimension are considered
         * too small for smoothing to be visually noticeable. Set to 5 % so that a
         * 100 dp-wide shape only reduces smoothing for radii below 5 dp.
         */
        private const val ADAPTIVE_THRESHOLD_FRACTION = 0.05f
    }
}

/**
 * A smooth pill shape with curvature-continuous corners.
 *
 * The corner radius is automatically set to 50 % of the shape's shortest dimension,
 * producing a stadium-like shape with smoothly rounded ends. Smoothing is fixed at
 * [SquircleShape.DEFAULT_SMOOTHING].
 */
object PillShape : Shape by SquircleShape(
    cornerSize = CornerSize(50),
    smoothing = SquircleShape.DEFAULT_SMOOTHING
)

/**
 * Returns a new [CornerSize] whose radius is reduced by [padding], suitable for an
 * element nested inside a container with this corner size.
 *
 * This maintains **visual concentricity** — inner and outer corners appear to share
 * the same centre, avoiding the "disproportionately round inner corner" artefact that
 * occurs when both layers use the same radius.
 *
 * The resulting radius is never negative.
 *
 * Usage:
 * ```
 * val outerCorner = CornerSize(24.dp)
 * val innerCorner = outerCorner.inset(padding = 12.dp)
 * // innerCorner resolves to 12 dp at layout time
 * ```
 */
fun CornerSize.inset(padding: Dp): CornerSize = InsetCornerSize(outer = this, padding = padding)

/**
 * A [CornerSize] that subtracts a fixed [padding] from an [outer] corner size at
 * layout time. Implemented as a data class for correct equality semantics.
 */
private data class InsetCornerSize(
    private val outer: CornerSize,
    private val padding: Dp
) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float {
        val outerPx = outer.toPx(shapeSize, density)
        val paddingPx = with(density) { padding.toPx() }
        return (outerPx - paddingPx).coerceAtLeast(0f)
    }
}


@Preview(showBackground = true, name = "Squircle and Pill Shapes")
@Composable
private fun SquircleShapePreview() {
    HydroTrackerTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PreviewCard(
                label = "No smoothing (standard rounded rect)",
                shape = SquircleShape(
                    cornerSize = CornerSize(28.dp),
                    smoothing = 0f
                )
            )
            PreviewCard(
                label = "Half smoothing (0.5)",
                shape = SquircleShape(
                    cornerSize = CornerSize(28.dp),
                    smoothing = 0.5f
                )
            )
            PreviewCard(
                label = "Full smoothing (default, 1.0)",
                shape = SquircleShape(cornerSize = CornerSize(28.dp))
            )

            // Per-corner radii — top only
            PreviewCard(
                label = "Top corners only (28 dp)",
                shape = SquircleShape(
                    topStart = CornerSize(28.dp),
                    topEnd = CornerSize(28.dp),
                    bottomEnd = CornerSize(0.dp),
                    bottomStart = CornerSize(0.dp)
                )
            )

            // Pill
            PreviewCard(
                label = "Pill shape",
                shape = PillShape
            )
        }
    }
}

@Preview(showBackground = true, name = "Concentric Corner Radius")
@Composable
private fun ConcentricCornerPreview() {
    HydroTrackerTheme {
        val outerCorner = CornerSize(32.dp)
        val padding = 12.dp

        Box(
            modifier = Modifier
                .padding(24.dp)
                .size(width = 280.dp, height = 160.dp)
                .clip(SquircleShape(cornerSize = outerCorner))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(SquircleShape(cornerSize = outerCorner.inset(padding)))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Concentric corners",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(
    label: String,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
