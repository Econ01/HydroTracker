package com.cemcakmak.hydrotracker.widgets.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.width
import com.cemcakmak.hydrotracker.MainActivity
import com.cemcakmak.hydrotracker.widgets.HydroWidgetState
import com.cemcakmak.hydrotracker.widgets.QuickAddWidgetLayout
import com.cemcakmak.hydrotracker.widgets.resolveQuickAddWidgetLayout

/** Adaptive quick-actions-only content shared by every instance of the second provider. */
@Composable
internal fun QuickAddWidgetContent(state: HydroWidgetState) {
    val size = LocalSize.current
    val layout = resolveQuickAddWidgetLayout(size)
    val cards = quickAddCardModels(state, count = layout.actionCount)
    val horizontalPadding = (size.width * 0.04f).coerceIn(4.dp, 12.dp)
    val verticalPadding = if (size.height < 120.dp) {
        1.dp
    } else {
        (size.height * 0.05f).coerceIn(3.dp, 10.dp)
    }
    val showHeader = !state.useTransparentBackground &&
        layout == QuickAddWidgetLayout.THREE && size.height >= 150.dp
    val bodyHeight = (
        size.height - (verticalPadding * 2) - if (showHeader) WIDGET_HEADER_BLOCK else 0.dp
        ).coerceAtLeast(32.dp)
    val contentWidth = (size.width - (horizontalPadding * 2)).coerceAtLeast(32.dp)

    Column(
        modifier = GlanceModifier
            .widgetSurface(
                transparent = state.useTransparentBackground,
                pureBlack = state.usePureBlack,
                pureWhite = state.usePureWhite,
            )
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        if (showHeader) {
            WidgetHeader()
            Spacer(GlanceModifier.height(8.dp))
        }
        Box(modifier = GlanceModifier.fillMaxWidth().height(bodyHeight)) {
            when (layout) {
                QuickAddWidgetLayout.SINGLE -> SingleActionContent(
                    state = state,
                    card = cards.first(),
                    bodyHeight = bodyHeight,
                    contentWidth = contentWidth,
                )

                QuickAddWidgetLayout.TWO -> TwoActionsContent(
                    state = state,
                    cards = cards,
                    bodyHeight = bodyHeight,
                    contentWidth = contentWidth,
                )

                QuickAddWidgetLayout.THREE -> ThreeActionsContent(
                    state = state,
                    cards = cards,
                    bodyHeight = bodyHeight,
                    contentWidth = contentWidth,
                )
            }
        }
    }
}

@Composable
private fun SingleActionContent(
    state: HydroWidgetState,
    card: QuickAddCardModel,
    bodyHeight: Dp,
    contentWidth: Dp,
) {
    Box(
        modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        CompactQuickAddCard(
            card = card,
            state = state,
            height = bodyHeight,
            vertical = true,
            showLabel = bodyHeight >= 100.dp && contentWidth >= 120.dp,
            availableWidth = contentWidth,
        )
    }
}

@Composable
private fun TwoActionsContent(
    state: HydroWidgetState,
    cards: List<QuickAddCardModel>,
    bodyHeight: Dp,
    contentWidth: Dp,
) {
    val gap = 4.dp
    if (contentWidth >= bodyHeight) {
        val buttonWidth = (contentWidth - gap) / 2
        val verticalButtons = bodyHeight >= 56.dp
        Row(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
            cards.forEachIndexed { index, card ->
                if (index > 0) Spacer(GlanceModifier.width(gap))
                Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                    CompactQuickAddCard(
                        card = card,
                        state = state,
                        height = bodyHeight,
                        vertical = verticalButtons,
                        showLabel = verticalButtons && bodyHeight >= 86.dp && buttonWidth >= 100.dp,
                        availableWidth = buttonWidth,
                    )
                }
            }
        }
    } else {
        val cardHeight = ((bodyHeight - gap) / 2).coerceAtLeast(24.dp)
        Column(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
            cards.forEachIndexed { index, card ->
                if (index > 0) Spacer(GlanceModifier.height(gap))
                CompactQuickAddCard(
                    card = card,
                    state = state,
                    height = cardHeight,
                    vertical = contentWidth < 80.dp,
                    showLabel = cardHeight >= 64.dp && contentWidth >= 110.dp,
                    availableWidth = contentWidth,
                )
            }
        }
    }
}

@Composable
private fun ThreeActionsContent(
    state: HydroWidgetState,
    cards: List<QuickAddCardModel>,
    bodyHeight: Dp,
    contentWidth: Dp,
) {
    val gap = 4.dp
    val horizontal = contentWidth >= bodyHeight * 1.45f && bodyHeight <= 180.dp
    if (horizontal) {
        val buttonWidth = (contentWidth - (gap * 2)) / 3
        val verticalButtons = bodyHeight >= 56.dp
        Row(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
            cards.forEachIndexed { index, card ->
                if (index > 0) Spacer(GlanceModifier.width(gap))
                Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                    CompactQuickAddCard(
                        card = card,
                        state = state,
                        height = bodyHeight,
                        vertical = verticalButtons,
                        showLabel = verticalButtons && bodyHeight >= 80.dp && buttonWidth >= 80.dp,
                        availableWidth = buttonWidth,
                    )
                }
            }
        }
        return
    }

    val cardHeight = ((bodyHeight - (gap * 2)) / 3).coerceAtLeast(24.dp)
    if (cardHeight < 48.dp) {
        ThreeActionGrid(
            state = state,
            cards = cards,
            bodyHeight = bodyHeight,
            contentWidth = contentWidth,
        )
        return
    }

    Column(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
        cards.forEachIndexed { index, card ->
            if (index > 0) Spacer(GlanceModifier.height(gap))
            if (cardHeight >= 44.dp && contentWidth >= 140.dp) {
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

/** Two buttons above one full-width button keep all three targets tall in a compact square. */
@Composable
private fun ThreeActionGrid(
    state: HydroWidgetState,
    cards: List<QuickAddCardModel>,
    bodyHeight: Dp,
    contentWidth: Dp,
) {
    val gap = 2.dp
    val rowHeight = (bodyHeight - gap) / 2
    val topButtonWidth = (contentWidth - gap) / 2
    Column(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
        Row(modifier = GlanceModifier.fillMaxWidth().height(rowHeight)) {
            cards.take(2).forEachIndexed { index, card ->
                if (index > 0) Spacer(GlanceModifier.width(gap))
                Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                    CompactQuickAddCard(
                        card = card,
                        state = state,
                        height = rowHeight,
                        vertical = true,
                        showLabel = rowHeight >= 64.dp && topButtonWidth >= 80.dp,
                        availableWidth = topButtonWidth,
                    )
                }
            }
        }
        Spacer(GlanceModifier.height(gap))
        CompactQuickAddCard(
            card = cards[2],
            state = state,
            height = rowHeight,
            showLabel = rowHeight >= 60.dp && contentWidth >= 140.dp,
            availableWidth = contentWidth,
        )
    }
}
