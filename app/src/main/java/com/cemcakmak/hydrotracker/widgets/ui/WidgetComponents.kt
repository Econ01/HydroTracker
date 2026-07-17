package com.cemcakmak.hydrotracker.widgets.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.widgets.HydroWidgetColors
import com.cemcakmak.hydrotracker.widgets.HydroWidgetState
import com.cemcakmak.hydrotracker.widgets.HydroWidgetTheme

/**
 * Applies the platform's widget corner radius on API 31+ (matching the launcher),
 * or a fixed 16dp radius on older versions.
 */
fun GlanceModifier.systemWidgetCornerRadius(): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        cornerRadius(16.dp)
    }

/**
 * Standard HydroTracker widget surface: themed widget background, host-provided background
 * drawable and system corner radius. Apply to the root element of every widget.
 *
 * Appearance overrides from the widget settings: [transparent] uses a transparent background
 * drawable resource — MIUI/HyperOS launchers ignore colour-int backgrounds with alpha and
 * composite their own card behind the widget, but they honour a transparent resource;
 * [pureBlack] forces the surface to black in dark mode and [pureWhite] to white in light mode.
 * Non-overridden modes keep Glance's exact widgetBackground, resolved per mode via a
 * configuration-adjusted context.
 */
@Composable
fun GlanceModifier.widgetSurface(
    transparent: Boolean = false,
    pureBlack: Boolean = false,
    pureWhite: Boolean = false,
): GlanceModifier {
    if (transparent) {
        return this
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_transparent_background))
            .systemWidgetCornerRadius()
    }
    val context = LocalContext.current
    val dayBackground = GlanceTheme.colors.widgetBackground.getColor(context.withNightMode(false))
    val nightBackground = GlanceTheme.colors.widgetBackground.getColor(context.withNightMode(true))
    return this
        .fillMaxSize()
        .appWidgetBackground()
        .background(
            ColorProvider(
                day = if (pureWhite) Color.White else dayBackground,
                night = if (pureBlack) Color.Black else nightBackground,
            ),
        )
        .systemWidgetCornerRadius()
}

/** A copy of this context whose configuration forces the given night mode. */
private fun Context.withNightMode(night: Boolean): Context {
    val config = Configuration(resources.configuration)
    config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
        if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    return createConfigurationContext(config)
}

/**
 * Renders the circular progress ring used by the widgets.
 *
 * Glance's [androidx.glance.appwidget.CircularProgressIndicator] is indeterminate-only, so a
 * determinate ring is drawn into a [Bitmap] with [android.graphics.Canvas] instead. This gives
 * us rounded stroke caps, a tinted track, an optional curved title ([Canvas.drawTextOnPath]) and an
 * optional icon badge pinned to the arc's origin — all following the active theme.
 *
 * The arc starts at the 6 o'clock position and sweeps clockwise.
 */
object WidgetRingRenderer {

    /** Canvas angle (degrees) of the 6 o'clock position; 0° is 3 o'clock, positive clockwise. */
    private const val ARC_START_ANGLE = 90f

    /**
     * @param sizeDp outer diameter of the ring bitmap
     * @param strokeWidthDp ring stroke width
     * @param progress 0f..1f
     * @param progressColour resolved colour of the progress arc
     * @param trackColour resolved colour of the full-circle track
     * @param arcTitle optional curved title drawn along the inside top of the ring
     * @param arcTitleColour resolved colour of [arcTitle]
     * @param arcSubtitle optional curved subtitle drawn along the inside bottom of the ring,
     * centred at 6 o'clock
     * @param arcSubtitleColour resolved colour of [arcSubtitle]
     */
    fun createRingBitmap(
        context: Context,
        sizeDp: Dp,
        strokeWidthDp: Dp,
        progress: Float,
        progressColour: Color,
        trackColour: Color,
        arcTitle: String? = null,
        arcTitleColour: Color = Color.Unspecified,
        arcSubtitle: String? = null,
        arcSubtitleColour: Color = Color.Unspecified,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp.value * density).toInt().coerceAtLeast(1)
        val strokePx = (strokeWidthDp.value * density).coerceAtLeast(1f)

        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        val inset = strokePx / 2f
        val bounds = RectF(inset, inset, sizePx - inset, sizePx - inset)

        // Track
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            strokeCap = Paint.Cap.ROUND
            color = trackColour.toArgb()
        }
        canvas.drawArc(bounds, 0f, 360f, false, trackPaint)

        // Progress arc: 6 o'clock origin, clockwise
        if (progress > 0f) {
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokePx
                strokeCap = Paint.Cap.ROUND
                color = progressColour.toArgb()
            }
            canvas.drawArc(
                bounds,
                ARC_START_ANGLE,
                360f * progress.coerceIn(0f, 1f),
                false,
                progressPaint,
            )
        }

        // Curved title along the inside top of the ring
        if (!arcTitle.isNullOrBlank()) {
            drawArcTitle(canvas, bounds, strokePx, arcTitle, arcTitleColour, density)
        }

        // Curved subtitle along the inside bottom of the ring (centred at 6 o'clock)
        if (!arcSubtitle.isNullOrBlank()) {
            drawArcSubtitle(canvas, bounds, strokePx, arcSubtitle, arcSubtitleColour, density)
        }

        return bitmap
    }

    /**
     * Draws [title] curved along the top of the ring, inside the stroke. The text size is
     * reduced until the text fits the arc (translations may be longer than English).
     */
    private fun drawArcTitle(
        canvas: Canvas,
        bounds: RectF,
        strokePx: Float,
        title: String,
        titleColour: Color,
        density: Float,
    ) {
        var textSizePx = bounds.width() * 0.09f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = titleColour.toArgb()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val titleRadius = (bounds.width() / 2f) - strokePx - (textSizePx * 0.6f)
        if (titleRadius <= 0f) return
        val titleBounds = RectF(
            bounds.centerX() - titleRadius,
            bounds.centerY() - titleRadius,
            bounds.centerX() + titleRadius,
            bounds.centerY() + titleRadius,
        )
        // Top semicircle: starts at 9 o'clock, sweeps clockwise over 12 o'clock to 3 o'clock
        val path = Path().apply { addArc(titleBounds, 180f, 180f) }
        val pathLength = PathMeasure(path, false).length

        var textWidth = Float.MAX_VALUE
        while (textWidth > pathLength * 0.85f && textSizePx > 7f * density) {
            paint.textSize = textSizePx
            textWidth = paint.measureText(title)
            if (textWidth <= pathLength * 0.85f) break
            textSizePx *= 0.9f
        }
        paint.textSize = textSizePx

        val hOffset = ((pathLength - textWidth) / 2f).coerceAtLeast(0f)
        canvas.drawTextOnPath(title, path, hOffset, 0f, paint)
    }

    /**
     * Draws [subtitle] curved along the bottom of the ring, inside the stroke, centred at
     * 6 o'clock. The path is swept counter-clockwise so the glyphs read upright and fan inward
     */
    private fun drawArcSubtitle(
        canvas: Canvas,
        bounds: RectF,
        strokePx: Float,
        subtitle: String,
        subtitleColour: Color,
        density: Float,
    ) {
        var textSizePx = bounds.width() * 0.09f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subtitleColour.toArgb()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Sit just inside the stroke: low enough to clear the centre percentage.
        val subtitleRadius = (bounds.width() / 2f) - strokePx
        if (subtitleRadius <= 0f) return
        val subtitleBounds = RectF(
            bounds.centerX() - subtitleRadius,
            bounds.centerY() - subtitleRadius,
            bounds.centerX() + subtitleRadius,
            bounds.centerY() + subtitleRadius,
        )
        // Restricted bottom arc (from ~8:40 to ~3:20 through 6 o'clock), swept
        // counter-clockwise so the text reads upright with glyph tops facing the centre.
        val path = Path().apply { addArc(subtitleBounds, 160f, -140f) }
        val pathLength = PathMeasure(path, false).length

        var textWidth = Float.MAX_VALUE
        while (textWidth > pathLength && textSizePx > 6f * density) {
            paint.textSize = textSizePx
            textWidth = paint.measureText(subtitle)
            if (textWidth <= pathLength) break
            textSizePx *= 0.9f
        }
        paint.textSize = textSizePx

        val hOffset = (pathLength - textWidth) / 2f
        canvas.drawTextOnPath(subtitle, path, hOffset, 0f, paint)
    }
}

/**
 * Determinate circular progress ring with themed colours and centred content.
 *
 * The arc starts at 6 o'clock and sweeps clockwise, using the theme's primary colour and
 * switching to the success colour once the daily goal has been reached. [centreContent] is
 * stacked in the middle of the ring (typically the percentage text or a goal-achieved check
 * mark). Optionally shows a curved [arcTitle] inside the top of the ring and a curved
 * [arcSubtitle] inside the bottom, centred at 6 o'clock.
 */
@Composable
fun ProgressRing(
    state: HydroWidgetState,
    size: Dp,
    strokeWidth: Dp,
    arcTitle: String? = null,
    arcSubtitle: String? = null,
    centreContent: @GlanceComposable @Composable () -> Unit,
) {
    val context = LocalContext.current
    val ringColour = if (state.isGoalAchieved) {
        HydroWidgetColors.success(context, state.useDynamicColors).getColor(context)
    } else {
        GlanceTheme.colors.primary.getColor(context)
    }
    val trackColour = GlanceTheme.colors.primaryContainer.getColor(context)
    val arcTitleColour = GlanceTheme.colors.onSurfaceVariant.getColor(context)
    val arcSubtitleColour = if (state.isGoalAchieved) {
        HydroWidgetColors.success(context, state.useDynamicColors).getColor(context)
    } else {
        GlanceTheme.colors.primary.getColor(context)
    }

    val bitmap = remember(size, strokeWidth, state.progress, state.isGoalAchieved, arcTitle, arcSubtitle) {
        WidgetRingRenderer.createRingBitmap(
            context = context,
            sizeDp = size,
            strokeWidthDp = strokeWidth,
            progress = state.progress,
            progressColour = ringColour,
            trackColour = trackColour,
            arcTitle = arcTitle,
            arcTitleColour = arcTitleColour,
            arcSubtitle = arcSubtitle,
            arcSubtitleColour = arcSubtitleColour,
        )
    }

    Box(
        modifier = GlanceModifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            modifier = GlanceModifier.size(size),
        )
        centreContent()
    }
}

private fun ringPreviewState(
    currentIntake: Double = 1300.0,
    dailyGoal: Double = 3000.0,
) = HydroWidgetState(
    currentIntake = currentIntake,
    dailyGoal = dailyGoal,
    progress = (currentIntake / dailyGoal).toFloat().coerceIn(0f, 1f),
    isGoalAchieved = currentIntake >= dailyGoal,
    remainingAmount = maxOf(0.0, dailyGoal - currentIntake),
    volumeUnit = VolumeUnit.MILLILITRES,
)

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 180, heightDp = 180)
@Composable
private fun ProgressRingPreview() {
    HydroWidgetTheme {
        ProgressRing(
            state = ringPreviewState(),
            size = 138.dp,
            strokeWidth = 18.dp,
            arcTitle = "Daily Hydration",
            arcSubtitle = "1.3 of 3 L",
        ) {
            Text(
                text = "43%",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 180, heightDp = 180)
@Composable
private fun ProgressRingGoalPreview() {
    HydroWidgetTheme {
        ProgressRing(
            state = ringPreviewState(currentIntake = 3000.0),
            size = 138.dp,
            strokeWidth = 18.dp,
            arcTitle = "Daily Hydration",
            arcSubtitle = "Goal reached!",
        ) {
            Image(
                provider = ImageProvider(R.drawable.award_star_filled),
                contentDescription = null,
                colorFilter = ColorFilter.tint(HydroWidgetColors.success(LocalContext.current)),
                modifier = GlanceModifier.size(30.dp),
            )
        }
    }
}
