package com.cemcakmak.hydrotracker.widgets.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.cemcakmak.hydrotracker.MainActivity
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import com.cemcakmak.hydrotracker.widgets.HydrationWidgetLayout
import com.cemcakmak.hydrotracker.widgets.HydroWidgetColors
import com.cemcakmak.hydrotracker.widgets.HydroWidgetState
import com.cemcakmak.hydrotracker.widgets.resolveHydrationWidgetLayout

/** Adaptive content for the original hydration provider. */
@Composable
internal fun HydrationWidgetContent(state: HydroWidgetState) {
    when (resolveHydrationWidgetLayout(LocalSize.current)) {
        HydrationWidgetLayout.RING_ONLY -> RingOnlyContent(state)
        HydrationWidgetLayout.LINEAR_ONLY -> LinearOnlyContent(state)
        HydrationWidgetLayout.LINEAR_WITH_ACTION -> LinearWithActionContent(state)
        HydrationWidgetLayout.RING_WITH_TWO_ACTIONS -> RingWithTwoActionsContent(state)
        HydrationWidgetLayout.RING_WITH_THREE_ACTIONS -> RingWithThreeActionsContent(state)
    }
}

@Composable
private fun hydrationSurfaceModifier(state: HydroWidgetState): GlanceModifier = GlanceModifier
    .widgetSurface(
        transparent = state.useTransparentBackground,
        pureBlack = state.usePureBlack,
        pureWhite = state.usePureWhite,
    )
    .clickable(actionStartActivity<MainActivity>())

/** 1x1: the ring itself is the first quick-add action. */
@Composable
private fun RingOnlyContent(state: HydroWidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val card = quickAddCardModels(state, count = 1).first()
    val padding = (minOf(size.width, size.height) * 0.06f).coerceIn(2.dp, 8.dp)
    val ringSize = (minOf(size.width, size.height) - (padding * 2)).coerceAtLeast(24.dp)
    val actionDescription = "${hydrationProgressDescription(context, state)}. " +
        quickAddContentDescription(context, state, card)

    Box(
        modifier = GlanceModifier
            .widgetSurface(
                transparent = state.useTransparentBackground,
                pureBlack = state.usePureBlack,
                pureWhite = state.usePureWhite,
            )
            .clickable(quickAddAction(card))
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        HydrationProgressRing(
            state = state,
            size = ringSize,
            showArcTexts = false,
            contentDescription = actionDescription,
        )
    }
}

/** Short two-cell layout: branded header with end-aligned intake/goal, then bar and percentage. */
@Composable
private fun LinearOnlyContent(state: HydroWidgetState) {
    val size = LocalSize.current
    val horizontalPadding = (size.width * 0.04f).coerceIn(6.dp, 14.dp)
    val verticalPadding = linearVerticalPadding(size.height)
    val compactHeader = size.height < 80.dp
    val showHeader = !state.useTransparentBackground
    val contentWidth = size.width - (horizontalPadding * 2)

    Column(
        modifier = hydrationSurfaceModifier(state).padding(
            horizontal = horizontalPadding,
            vertical = verticalPadding,
        ),
    ) {
        if (showHeader) {
            LinearHeader(
                state = state,
                compact = compactHeader,
                availableWidth = contentWidth,
            )
            Spacer(GlanceModifier.height(if (compactHeader) 4.dp else 8.dp))
        }
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentAlignment = Alignment.Center,
        ) {
            LinearProgressWithPercentage(state, contentWidth)
        }
    }
}

/** Short roomy layout: the same information hierarchy plus the first quick-add action. */
@Composable
private fun LinearWithActionContent(state: HydroWidgetState) {
    val size = LocalSize.current
    val card = quickAddCardModels(state, count = 1).first()
    val horizontalPadding = (size.width * 0.035f).coerceIn(6.dp, 14.dp)
    val verticalPadding = linearVerticalPadding(size.height)
    val compactHeader = size.height < 80.dp
    val showHeader = !state.useTransparentBackground
    val headerBlock = linearHeaderBlock(showHeader, compactHeader)
    val bodyHeight = (size.height - (verticalPadding * 2) - headerBlock).coerceAtLeast(16.dp)
    val actionHeight = bodyHeight.coerceAtMost(64.dp)
    val actionWidth = (size.width * 0.28f).coerceIn(72.dp, 120.dp)
    val contentWidth = size.width - (horizontalPadding * 2)
    val actionGap = 8.dp
    val progressWidth = contentWidth - actionWidth - actionGap

    Column(
        modifier = hydrationSurfaceModifier(state).padding(
            horizontal = horizontalPadding,
            vertical = verticalPadding,
        ),
    ) {
        if (showHeader) {
            LinearHeader(
                state = state,
                compact = compactHeader,
                availableWidth = contentWidth,
            )
            Spacer(GlanceModifier.height(if (compactHeader) 4.dp else 8.dp))
        }
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                LinearProgressWithPercentage(state, progressWidth)
            }
            Spacer(GlanceModifier.width(actionGap))
            Box(
                modifier = GlanceModifier.width(actionWidth).height(actionHeight),
                contentAlignment = Alignment.Center,
            ) {
                CompactQuickAddCard(
                    card = card,
                    state = state,
                    height = actionHeight,
                    vertical = actionHeight >= 56.dp && actionWidth < 100.dp,
                    availableWidth = actionWidth,
                )
            }
        }
    }
}

private fun linearVerticalPadding(height: Dp): Dp = if (height < 64.dp) {
    2.dp
} else {
    (height * 0.04f).coerceIn(3.dp, 8.dp)
}

private fun linearHeaderBlock(showHeader: Boolean, compact: Boolean): Dp = when {
    !showHeader -> 0.dp
    compact -> WIDGET_COMPACT_HEADER_BLOCK
    else -> WIDGET_HEADER_BLOCK
}

/** Branded header with intake/goal aligned to the opposite end when both labels fit. */
@Composable
private fun LinearHeader(
    state: HydroWidgetState,
    compact: Boolean,
    availableWidth: Dp,
) {
    val context = LocalContext.current
    val compactTypography = compact || availableWidth < 200.dp
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(modifier = GlanceModifier.defaultWeight()) {
            WidgetHeader(compact = compactTypography)
        }
        if (availableWidth >= 170.dp) {
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = hydrationGoalSubtitle(context, state),
                style = TextStyle(
                    color = if (state.isGoalAchieved) {
                        HydroWidgetColors.success(context, state.useDynamicColors)
                    } else {
                        GlanceTheme.colors.onSurfaceVariant
                    },
                    fontSize = if (compactTypography) 10.sp else 11.sp,
                    fontWeight = if (state.isGoalAchieved) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.End,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LinearProgressWithPercentage(
    state: HydroWidgetState,
    availableWidth: Dp,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val percent = (state.progress * 100).toInt().coerceIn(0, 100)
    val percentLabel = context.getString(R.string.percent_format, "$percent%")
    val gap = when {
        availableWidth < 80.dp -> 2.dp
        availableWidth < 120.dp -> 4.dp
        else -> 8.dp
    }
    val fontSize = (size.height.value * 0.2f).coerceIn(12f, 20f).let { baseSize ->
        if (availableWidth < 80.dp) minOf(baseSize, 14f) else baseSize
    }.sp
    // The launcher renders the TextView, so app-side glyph measurement is only a baseline. Include
    // a fixed safety inset and a digit-count floor to avoid truncation under OEM font/DPI changes.
    val displayMetrics = context.resources.displayMetrics
    val textSizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        fontSize.value,
        displayMetrics,
    )
    val percentageWidth = remember(percentLabel, textSizePx, displayMetrics.density) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val measuredWidth = (paint.measureText(percentLabel) / displayMetrics.density).dp
        val minimumWidth = (fontSize.value * when {
            percent >= 100 -> 3.2f
            percent >= 10 -> 2.5f
            else -> 2.0f
        }).dp
        maxOf(measuredWidth + 8.dp, minimumWidth)
    }
    val barWidth = (availableWidth - percentageWidth - gap).coerceAtLeast(16.dp)
    Row(
        modifier = GlanceModifier.width(availableWidth),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        LinearHydrationProgress(
            state = state,
            width = barWidth,
            height = (size.height * 0.18f).coerceIn(8.dp, 16.dp),
        )
        Spacer(GlanceModifier.width(gap))
        Box(
            modifier = GlanceModifier.width(percentageWidth),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = percentLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}

/** Compact multi-row layout: a ring with two quick-add actions. */
@Composable
private fun RingWithTwoActionsContent(state: HydroWidgetState) {
    val size = LocalSize.current
    val horizontalPadding = (size.width * 0.04f).coerceIn(5.dp, 12.dp)
    val verticalPadding = (size.height * 0.04f).coerceIn(4.dp, 10.dp)
    val showHeader = !state.useTransparentBackground && size.height >= 150.dp
    val bodyHeight = (
        size.height - (verticalPadding * 2) - if (showHeader) WIDGET_HEADER_BLOCK else 0.dp
        ).coerceAtLeast(80.dp)
    val contentWidth = (size.width - (horizontalPadding * 2)).coerceAtLeast(48.dp)
    val cards = quickAddCardModels(state, count = 2)

    Column(
        modifier = hydrationSurfaceModifier(state).padding(
            horizontal = horizontalPadding,
            vertical = verticalPadding,
        ),
    ) {
        if (showHeader) {
            WidgetHeader()
            Spacer(GlanceModifier.height(8.dp))
        }
        if (contentWidth <= bodyHeight * 1.4f) {
            val gap = 4.dp
            val actionsHeight = (bodyHeight * 0.32f).coerceIn(48.dp, 64.dp)
            val ringAreaHeight = bodyHeight - actionsHeight - gap
            val ringSize = minOf(ringAreaHeight, contentWidth).coerceAtLeast(40.dp)
            Column(modifier = GlanceModifier.fillMaxWidth().height(bodyHeight)) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().height(ringAreaHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    HydrationProgressRing(
                        state = state,
                        size = ringSize,
                        showArcTexts = false,
                    )
                }
                Spacer(GlanceModifier.height(gap))
                Row(modifier = GlanceModifier.fillMaxWidth().height(actionsHeight)) {
                    cards.forEachIndexed { index, card ->
                        if (index > 0) Spacer(GlanceModifier.width(gap))
                        Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                            CompactQuickAddCard(
                                card = card,
                                state = state,
                                height = actionsHeight,
                                vertical = true,
                                showLabel = actionsHeight >= 60.dp && contentWidth >= 160.dp,
                                availableWidth = (contentWidth - gap) / 2,
                            )
                        }
                    }
                }
            }
        } else {
            SideBySideRingAndCards(
                state = state,
                cards = cards,
                contentWidth = contentWidth,
                bodyHeight = bodyHeight,
                showArcTexts = false,
            )
        }
    }
}

/** Roomy layout: Three cards, arranged around the ring to suit the host bounds. */
@Composable
private fun RingWithThreeActionsContent(state: HydroWidgetState) {
    val size = LocalSize.current
    val horizontalPadding = (size.width * 0.03f).coerceIn(8.dp, 16.dp)
    val verticalPadding = (size.height * 0.05f).coerceIn(5.dp, 10.dp)
    val showHeader = !state.useTransparentBackground && size.height >= 140.dp
    val bodyHeight = (
        size.height - (verticalPadding * 2) - if (showHeader) WIDGET_HEADER_BLOCK else 0.dp
        ).coerceAtLeast(96.dp)
    val contentWidth = (size.width - (horizontalPadding * 2)).coerceAtLeast(72.dp)
    val cards = quickAddCardModels(state, count = 3)

    Column(
        modifier = hydrationSurfaceModifier(state).padding(
            horizontal = horizontalPadding,
            vertical = verticalPadding,
        ),
    ) {
        if (showHeader) {
            WidgetHeader()
            Spacer(GlanceModifier.height(8.dp))
        }
        if (contentWidth >= bodyHeight * 1.15f) {
            SideBySideRingAndCards(
                state = state,
                cards = cards,
                contentWidth = contentWidth,
                bodyHeight = bodyHeight,
                showArcTexts = true,
            )
        } else {
            StackedRingAndCards(
                state = state,
                cards = cards,
                contentWidth = contentWidth,
                bodyHeight = bodyHeight,
            )
        }
    }
}

@Composable
private fun SideBySideRingAndCards(
    state: HydroWidgetState,
    cards: List<QuickAddCardModel>,
    contentWidth: Dp,
    bodyHeight: Dp,
    showArcTexts: Boolean,
) {
    val gap = 4.dp
    val ringAreaWidth = ((contentWidth - gap) / 2).coerceAtLeast(40.dp)
    val ringSize = minOf(bodyHeight, ringAreaWidth).coerceAtLeast(40.dp)
    val initialCardHeight = (bodyHeight - (gap * (cards.size - 1))) / cards.size
    val visibleCards = if (initialCardHeight < 48.dp && cards.size > 2) cards.take(2) else cards
    val cardHeight = (
        (bodyHeight - (gap * (visibleCards.size - 1))) / visibleCards.size
        ).coerceAtLeast(24.dp)

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(bodyHeight),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            HydrationProgressRing(
                state = state,
                size = ringSize,
                showArcTexts = showArcTexts && ringSize >= 90.dp,
            )
        }
        Spacer(GlanceModifier.width(gap))
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            visibleCards.forEachIndexed { index, card ->
                if (index > 0) Spacer(GlanceModifier.height(gap))
                if (cardHeight >= 36.dp) {
                    FullQuickAddCard(card = card, state = state, height = cardHeight)
                } else {
                    CompactQuickAddCard(
                        card = card,
                        state = state,
                        height = cardHeight,
                        availableWidth = ringAreaWidth,
                    )
                }
            }
        }
    }
}

@Composable
private fun StackedRingAndCards(
    state: HydroWidgetState,
    cards: List<QuickAddCardModel>,
    contentWidth: Dp,
    bodyHeight: Dp,
) {
    val gap = 6.dp
    val initialRingAreaHeight = (bodyHeight * 0.46f).coerceIn(64.dp, 190.dp)
    val initialActionsHeight = bodyHeight - initialRingAreaHeight - gap
    val initialCardHeight = (initialActionsHeight - (gap * (cards.size - 1))) / cards.size
    val visibleCards = if (initialCardHeight < 48.dp && cards.size > 2) cards.take(2) else cards
    val requiredActionsHeight = (48.dp * visibleCards.size) + (gap * (visibleCards.size - 1))
    val ringAreaHeight = if (visibleCards.size < cards.size) {
        minOf(initialRingAreaHeight, bodyHeight - requiredActionsHeight - gap).coerceAtLeast(24.dp)
    } else {
        initialRingAreaHeight
    }
    val actionsHeight = bodyHeight - ringAreaHeight - gap
    val cardHeight = (
        (actionsHeight - (gap * (visibleCards.size - 1))) / visibleCards.size
        ).coerceAtLeast(24.dp)
    val ringSize = minOf(ringAreaHeight, contentWidth * 0.7f).coerceAtLeast(24.dp)

    Column(modifier = GlanceModifier.fillMaxWidth().height(bodyHeight)) {
        Box(
            modifier = GlanceModifier.fillMaxWidth().height(ringAreaHeight),
            contentAlignment = Alignment.Center,
        ) {
            HydrationProgressRing(
                state = state,
                size = ringSize,
                showArcTexts = ringSize >= 90.dp,
            )
        }
        Spacer(GlanceModifier.height(gap))
        visibleCards.forEachIndexed { index, card ->
            if (index > 0) Spacer(GlanceModifier.height(gap))
            if (cardHeight >= 38.dp && contentWidth >= 140.dp) {
                FullQuickAddCard(card = card, state = state, height = cardHeight)
            } else {
                CompactQuickAddCard(
                    card = card,
                    state = state,
                    height = cardHeight,
                    availableWidth = contentWidth,
                )
            }
        }
    }
}

/** Shared ring rendering, including optional curved labels and centre goal/percentage content. */
@Composable
private fun HydrationProgressRing(
    state: HydroWidgetState,
    size: Dp,
    showArcTexts: Boolean,
    modifier: GlanceModifier = GlanceModifier,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val percent = (state.progress * 100).toInt().coerceIn(0, 100)
    val percentLabel = context.getString(R.string.percent_format, "$percent%")
    val arcSubtitle = if (showArcTexts) hydrationGoalSubtitle(context, state) else null

    ProgressRing(
        state = state,
        size = size,
        strokeWidth = size * 0.15f,
        modifier = modifier.semantics {
            this.contentDescription = contentDescription ?: hydrationProgressDescription(context, state)
        },
        arcTitle = if (showArcTexts) context.getString(R.string.widget_daily_hydration) else null,
        arcSubtitle = arcSubtitle,
    ) {
        if (state.isGoalAchieved) {
            Image(
                provider = ImageProvider(R.drawable.award_star_filled),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    HydroWidgetColors.success(context, state.useDynamicColors),
                ),
                modifier = GlanceModifier.size((size * 0.25f).coerceAtLeast(16.dp)),
            )
        } else {
            Text(
                text = percentLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = (size.value * 0.19f).coerceIn(9f, 34f).sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}

/** Current intake and daily goal, or the completion label used by every progress presentation. */
private fun hydrationGoalSubtitle(context: Context, state: HydroWidgetState): String {
    if (state.isGoalAchieved) return context.getString(R.string.widget_goal_reached)
    val goalDisplayUnit = VolumeUnitConverter.selectDisplayUnit(
        state.dailyGoal,
        state.volumeUnit,
    )
    return context.getString(
        R.string.widget_of_goal_format,
        VolumeUnitConverter.formatValue(state.currentIntake, goalDisplayUnit),
        VolumeUnitConverter.format(context, state.dailyGoal, state.volumeUnit),
    )
}

private fun hydrationProgressDescription(context: Context, state: HydroWidgetState): String {
    val percent = (state.progress * 100).toInt().coerceIn(0, 100)
    val description = context.getString(R.string.widget_progress_cd, "$percent%")
    return if (state.isGoalAchieved) {
        "$description. ${context.getString(R.string.widget_goal_reached)}"
    } else {
        description
    }
}
