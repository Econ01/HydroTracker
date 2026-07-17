package com.cemcakmak.hydrotracker.widgets

import android.content.Context
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.cemcakmak.hydrotracker.MainActivity
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import com.cemcakmak.hydrotracker.widgets.actions.QuickAddAction
import com.cemcakmak.hydrotracker.widgets.ui.ProgressRing
import com.cemcakmak.hydrotracker.widgets.ui.widgetSurface


class HydroLargeWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HydroLargeGlanceWidget()
}

class HydroLargeGlanceWidget : GlanceAppWidget() {

    companion object {
        private const val TAG = "HydroLargeWidget"

        private val SIZES = setOf(
            DpSize(250.dp, 180.dp),
            DpSize(330.dp, 180.dp),
            DpSize(330.dp, 260.dp),
        )
    }

    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // State-driven UI: HydroWidgetUpdater mirrors the latest snapshot into Glance
        // preferences before every update, and a live session re-reads them here on each
        // recomposition. Loading directly in provideGlance would only run once per session
        // and republish stale data for every update within the session's lifetime.
        provideContent {
            HydroWidgetTheme {
                LargeContent(currentState<Preferences>().toHydroWidgetState())
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val state = HydroWidgetStateLoader.load(context)
        provideContent {
            HydroWidgetTheme {
                LargeContent(state)
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        Log.e(TAG, "❌ Large widget composition failed", throwable)
        super.onCompositionError(context, glanceId, appWidgetId, throwable)
    }
}

/** A quick-add action card on the Large widget. */
private data class QuickAddPreset(
    val amount: Double,
    val container: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
)

private val QUICK_ADD_PRESETS = listOf(
    QuickAddPreset(250.0, "Glass", R.string.widget_card_glass, R.drawable.glass_cup_filled),
    QuickAddPreset(500.0, "Bottle", R.string.widget_card_bottle, R.drawable.water_bottle_filled),
    QuickAddPreset(1000.0, "Large Bottle", R.string.widget_card_large_bottle, R.drawable.water_bottle_large_filled),
)

/** Everything a quick-add card needs to render and to fire its action. */
private data class QuickAddCardModel(
    val amount: Double,
    val containerName: String,
    val beverageName: String,
    val label: String,
    @DrawableRes val iconRes: Int,
    val colours: WidgetCardColours,
)

/**
 * Builds a card from a usage-driven slot. Water combos show the container's icon and name
 * (custom/unknown containers fall back to a generic glass); other beverages show the
 * beverage's icon and localized name. Colours come from the beverage's extended-palette family.
 */
private fun slotCardModel(context: Context, slot: WidgetQuickAddSlot): QuickAddCardModel {
    val beverage = BeverageType.fromStringOrDefault(slot.beverageName)
    val (label, iconRes) = if (beverage != BeverageType.WATER) {
        context.getString(beverage.labelResId) to beverage.iconResFilled
    } else {
        val preset = ContainerPreset.getDefaultPresets().firstOrNull { it.name == slot.containerName }
        val presetLabel = preset?.let { if (it.labelResId != 0) context.getString(it.labelResId) else it.name }
        (presetLabel ?: slot.containerName) to (preset?.iconRes ?: R.drawable.glass_cup_filled)
    }
    return QuickAddCardModel(
        amount = slot.volume,
        containerName = slot.containerName,
        beverageName = beverage.name,
        label = label,
        iconRes = iconRes,
        colours = beverageCardColours(context, slot.beverageName),
    )
}

/** Builds a card from one of the hardcoded default presets (used when a slot has no history). */
private fun presetCardModel(
    context: Context,
    preset: QuickAddPreset,
    colours: WidgetCardColours,
) = QuickAddCardModel(
    amount = preset.amount,
    containerName = preset.container,
    beverageName = BeverageType.WATER.name,
    label = context.getString(preset.labelRes),
    iconRes = preset.iconRes,
    colours = colours,
)

/** Header height (icon + text) plus the spacer below it, in dp. */
private val HEADER_BLOCK = 24.dp

@Composable
private fun LargeContent(state: HydroWidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current

    val horizontalPadding = (size.width * 0.03f).coerceIn(10.dp, 16.dp)
    val verticalPadding = (size.height * 0.055f).coerceIn(6.dp, 10.dp)
    val sectionSpacing = 4.dp

    // Body height available to the ring + cards below the header.
    val bodyHeight = size.height - (verticalPadding * 2) - HEADER_BLOCK

    // Ring and quick-add column share the content width 1:1; the ring fills the body height.
    val maxRingByWidth = (size.width - (horizontalPadding * 2) - sectionSpacing) / 2
    val ringSize = minOf(bodyHeight, maxRingByWidth).coerceAtLeast(40.dp)
    // Stroke is ~13% of the ring diameter.
    val strokeWidth = ringSize * 0.15f
    // Curved arc texts only stay legible on a reasonably large ring.
    val showArcTexts = ringSize >= 90.dp
    val percent = (state.progress * 100).toInt()

    // Quick-add card geometry: exactly three cards plus two gaps must fit the body height.
    val cardGap = 4.dp
    val cardHeight = ((bodyHeight - (cardGap * 4)) / 3).coerceAtLeast(16.dp)

    val cardColours = listOf(
        WidgetCardColours(
            GlanceTheme.colors.primaryContainer,
            GlanceTheme.colors.primary,
            GlanceTheme.colors.onPrimary,
            GlanceTheme.colors.onPrimaryContainer,
        ),
        WidgetCardColours(
            GlanceTheme.colors.secondaryContainer,
            GlanceTheme.colors.secondary,
            GlanceTheme.colors.onSecondary,
            GlanceTheme.colors.onSecondaryContainer,
        ),
        WidgetCardColours(
            GlanceTheme.colors.tertiaryContainer,
            GlanceTheme.colors.tertiary,
            GlanceTheme.colors.onTertiary,
            GlanceTheme.colors.onTertiaryContainer,
        ),
    )

    // Quick-add cards: usage-driven slots when history exists, themed defaults otherwise.
    val quickAddCards = (0 until QUICK_ADD_SLOT_COUNT).map { index ->
        state.quickAddSlots.getOrNull(index)?.let { slotCardModel(context, it) }
            ?: presetCardModel(context, QUICK_ADD_PRESETS[index], cardColours[index])
    }

    Column(
        modifier = GlanceModifier
            .widgetSurface()
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Image(
                provider = ImageProvider(R.drawable.water_drop_filled),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.secondary),
                modifier = GlanceModifier.size(16.dp),
            )
            Spacer(GlanceModifier.width(5.dp))
            Text(
                text = context.getString(R.string.app_name),
                style = TextStyle(
                    color = GlanceTheme.colors.secondary,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(8.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                ProgressRing(
                    state = state,
                    size = ringSize,
                    strokeWidth = strokeWidth,
                    arcTitle = if (showArcTexts) {
                        context.getString(R.string.widget_daily_hydration)
                    } else {
                        null
                    },
                    arcSubtitle = if (showArcTexts) {
                        if (state.isGoalAchieved) {
                            context.getString(R.string.widget_goal_reached)
                        } else {
                            // "1.2 of 2 L" — current intake in the goal's display unit
                            val goalDisplayUnit = VolumeUnitConverter.selectDisplayUnit(
                                state.dailyGoal,
                                state.volumeUnit,
                            )
                            context.getString(
                                R.string.widget_of_goal_format,
                                VolumeUnitConverter.formatValue(
                                    state.currentIntake,
                                    goalDisplayUnit,
                                ),
                                VolumeUnitConverter.format(
                                    context,
                                    state.dailyGoal,
                                    state.volumeUnit,
                                ),
                            )
                        }
                    } else {
                        null
                    }
                ) {
                    if (state.isGoalAchieved) {
                        Image(
                            provider = ImageProvider(R.drawable.award_star_filled),
                            contentDescription = context.getString(R.string.widget_goal_reached),
                            colorFilter = ColorFilter.tint(HydroWidgetColors.success(context)),
                            modifier = GlanceModifier.size(
                                (ringSize * 0.25f).coerceAtLeast(16.dp),
                            ),
                        )
                    } else {
                        Text(
                            text = context.getString(R.string.percent_format, percent),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = (ringSize.value * 0.17f).sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(GlanceModifier.width(sectionSpacing))

            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                quickAddCards.forEachIndexed { index, card ->
                    if (index > 0) {
                        Spacer(GlanceModifier.height(cardGap))
                    }
                    QuickAddCard(
                        card = card,
                        height = cardHeight,
                        state = state,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddCard(
    card: QuickAddCardModel,
    height: Dp,
    state: HydroWidgetState,
) {
    val context = LocalContext.current
    val amountLabel = VolumeUnitConverter.format(context, card.amount, state.volumeUnit)
    val pillHeight = height * 0.72f
    val pillWidth = height * 0.5f
    val labelFontSize = (height.value * 0.18f).coerceAtLeast(7f).sp
    val amountFontSize = (height.value * 0.38f).coerceAtLeast(8f).sp
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(height)
            .background(card.colours.container)
            .cornerRadius(height * 0.38f)
            .semantics {
                contentDescription = context.getString(R.string.widget_quick_add_cd, amountLabel)
            }
            .clickable(
                actionRunCallback<QuickAddAction>(
                    actionParametersOf(
                        QuickAddAction.KEY_AMOUNT to card.amount,
                        QuickAddAction.KEY_CONTAINER to card.containerName,
                        QuickAddAction.KEY_BEVERAGE to card.beverageName,
                    ),
                ),
            )
            .padding(horizontal = height * 0.13f),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(pillWidth, pillHeight)
                .background(card.colours.pill)
                .cornerRadius(pillHeight * 0.35f),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(card.iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(card.colours.pillContent),
                modifier = GlanceModifier.size(pillHeight * 0.45f),
            )
        }
        Spacer(GlanceModifier.width(height * 0.18f))
        Column {
            Text(
                text = card.label,
                style = TextStyle(
                    color = card.colours.content,
                    fontSize = labelFontSize,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Text(
                text = amountLabel,
                style = TextStyle(
                    color = card.colours.content,
                    fontSize = amountFontSize,
                ),
                maxLines = 1,
            )
        }
    }
}

private fun previewState(
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

@Suppress("unused") // Rendered by the Android Studio Design pane, not called from code
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 330, heightDp = 190)
@Composable
private fun LargeWidgetTallPreview() {
    HydroWidgetTheme {
        LargeContent(previewState())
    }
}

@Suppress("unused") // Design-pane entry point
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 330, heightDp = 190)
@Composable
private fun LargeWidgetGoalPreview() {
    HydroWidgetTheme {
        LargeContent(previewState(currentIntake = 3000.0))
    }
}

/** Forces the HYDRO_THEME palette so the branded (non-dynamic) colours can be reviewed. */
@Suppress("unused") // Design-pane entry point
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 330, heightDp = 180)
@Composable
private fun LargeWidgetHydroThemePreview() {
    GlanceTheme(colors = HydroWidgetColors.hydroColors) {
        LargeContent(previewState())
    }
}
