package com.cemcakmak.hydrotracker.widgets.ui

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import com.cemcakmak.hydrotracker.widgets.HydroWidgetState
import com.cemcakmak.hydrotracker.widgets.QUICK_ADD_SLOT_COUNT
import com.cemcakmak.hydrotracker.widgets.WidgetCardColours
import com.cemcakmak.hydrotracker.widgets.WidgetQuickAddSlot
import com.cemcakmak.hydrotracker.widgets.actions.QuickAddAction
import com.cemcakmak.hydrotracker.widgets.beverageCardColours

/** A hardcoded fallback used when its quick-add slot has no history or pinned selection. */
internal data class QuickAddPreset(
    val amount: Double,
    val container: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
)

internal val QUICK_ADD_PRESETS = listOf(
    QuickAddPreset(250.0, "Glass", R.string.widget_card_glass, R.drawable.glass_cup_filled),
    QuickAddPreset(500.0, "Bottle", R.string.widget_card_bottle, R.drawable.water_bottle_filled),
    QuickAddPreset(1000.0, "Large Bottle", R.string.widget_card_large_bottle, R.drawable.water_bottle_large_filled),
)

/** Everything a quick-add card needs to render and to fire its action. */
internal data class QuickAddCardModel(
    val amount: Double,
    val containerName: String,
    val beverageName: String,
    val label: String,
    @DrawableRes val iconRes: Int,
    val colours: WidgetCardColours,
)

/** Resolves the requested number of shared automatic/pinned slots into themed cards. */
@Composable
internal fun quickAddCardModels(
    state: HydroWidgetState,
    count: Int = QUICK_ADD_SLOT_COUNT,
): List<QuickAddCardModel> {
    val context = LocalContext.current
    val fallbackColours = listOf(
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
    return (0 until count.coerceIn(0, QUICK_ADD_SLOT_COUNT)).map { index ->
        state.quickAddSlots.getOrNull(index)?.let {
            slotCardModel(context, it, state.useDynamicColors)
        } ?: presetCardModel(context, QUICK_ADD_PRESETS[index], fallbackColours[index])
    }
}

/** The common callback used by every full, compact, and ring-only quick-add target. */
internal fun quickAddAction(card: QuickAddCardModel): Action = actionRunCallback<QuickAddAction>(
    actionParametersOf(
        QuickAddAction.KEY_AMOUNT to card.amount,
        QuickAddAction.KEY_CONTAINER to card.containerName,
        QuickAddAction.KEY_BEVERAGE to card.beverageName,
    ),
)

/** Announces both identity and amount, including when a compact card hides its visible label. */
internal fun quickAddContentDescription(
    context: Context,
    state: HydroWidgetState,
    card: QuickAddCardModel,
): String {
    val amountLabel = VolumeUnitConverter.format(context, card.amount, state.volumeUnit)
    return context.getString(R.string.widget_quick_add_cd, "${card.label}, $amountLabel")
}

/**
 * Builds a card from a usage-driven slot. Water combos show the container's icon and name; other
 * beverages show the beverage's icon and localized name. Colours use the beverage's palette family.
 */
internal fun slotCardModel(
    context: Context,
    slot: WidgetQuickAddSlot,
    useDynamicColors: Boolean,
): QuickAddCardModel {
    val matchingBeverage = BeverageType.entries.firstOrNull {
        it.name == slot.beverageName || it.displayName.equals(slot.beverageName, ignoreCase = true)
    }
    val beverage = matchingBeverage ?: BeverageType.fromStringOrDefault(slot.beverageName)
    val isCustom = matchingBeverage == null && beverage == BeverageType.WATER &&
        slot.beverageName !in LEGACY_WATER_BEVERAGE_NAMES
    val (label, iconRes, beverageKey) = when {
        isCustom -> Triple(slot.beverageName, R.drawable.water_drop_filled, slot.beverageName)
        beverage != BeverageType.WATER -> Triple(
            context.getString(beverage.labelResId),
            beverage.iconResFilled,
            beverage.name,
        )
        else -> {
            val preset = ContainerPreset.getDefaultPresets().firstOrNull {
                it.name == slot.containerName
            }
            val presetLabel = preset?.let {
                if (it.labelResId != 0) context.getString(it.labelResId) else it.name
            }
            Triple(
                presetLabel ?: slot.containerName,
                preset?.iconRes ?: R.drawable.glass_cup_filled,
                beverage.name,
            )
        }
    }
    return QuickAddCardModel(
        amount = slot.volume,
        containerName = slot.containerName,
        beverageName = beverageKey,
        label = label,
        iconRes = iconRes,
        colours = beverageCardColours(context, slot.beverageName, useDynamicColors),
    )
}

// Removed enum names that must continue to resolve as water rather than as custom beverages.
private val LEGACY_WATER_BEVERAGE_NAMES = setOf(
    "ENERGY_DRINK",
    "COCONUT_WATER",
    "FLAVORED_WATER",
    "SPARKLING_WATER",
)

/** Builds a card from a hardcoded fallback preset. */
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

/** The existing roomy quick-add card: icon pill, container/beverage label, and formatted amount. */
@Composable
internal fun FullQuickAddCard(
    card: QuickAddCardModel,
    state: HydroWidgetState,
    height: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val amountLabel = VolumeUnitConverter.format(context, card.amount, state.volumeUnit)
    val pillHeight = height * 0.72f
    val pillWidth = height * 0.5f
    val labelFontSize = (height.value * 0.18f).coerceIn(7f, 16f).sp
    val amountFontSize = (height.value * 0.38f).coerceIn(8f, 20f).sp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(card.colours.container)
            .cornerRadius((height * 0.38f).coerceAtMost(28.dp))
            .semantics {
                contentDescription = quickAddContentDescription(context, state, card)
            }
            .clickable(quickAddAction(card))
            .padding(horizontal = (height * 0.13f).coerceIn(4.dp, 12.dp)),
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
        Spacer(GlanceModifier.width((height * 0.18f).coerceAtMost(12.dp)))
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

/**
 * A space-constrained quick-add target. Vertical mode stacks the icon above its text and is useful
 * for buttons arranged in a row; horizontal mode suits short, full-width cards.
 */
@Composable
internal fun CompactQuickAddCard(
    card: QuickAddCardModel,
    state: HydroWidgetState,
    height: Dp,
    modifier: GlanceModifier = GlanceModifier,
    vertical: Boolean = false,
    showLabel: Boolean = false,
    availableWidth: Dp? = null,
) {
    val context = LocalContext.current
    val amountLabel = VolumeUnitConverter.format(context, card.amount, state.volumeUnit)
    val effectiveShowLabel = showLabel && (availableWidth?.let { it >= 80.dp } != false)
    val cardModifier = modifier
        .fillMaxWidth()
        .height(height)
        .background(card.colours.container)
        .cornerRadius((height * 0.3f).coerceAtMost(24.dp))
        .semantics {
            contentDescription = quickAddContentDescription(context, state, card)
        }
        .clickable(quickAddAction(card))

    if (vertical) {
        val pillSize = (height * if (effectiveShowLabel) 0.3f else 0.4f).coerceIn(16.dp, 44.dp)
        Column(
            modifier = cardModifier.padding(2.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            QuickAddIconPill(card, pillSize)
            Spacer(GlanceModifier.height(2.dp))
            if (effectiveShowLabel) {
                Text(
                    text = card.label,
                    style = TextStyle(
                        color = card.colours.content,
                        fontSize = compactTextSize(height, availableWidth, 0.13f, 0.13f, 9f, 14f).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
            Text(
                text = amountLabel,
                style = TextStyle(
                    color = card.colours.content,
                    fontSize = compactTextSize(height, availableWidth, 0.19f, 0.2f, 10f, 18f).sp,
                    fontWeight = if (effectiveShowLabel) FontWeight.Normal else FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    } else {
        val basePillSize = (height * 0.58f).coerceIn(16.dp, 36.dp)
        val pillSize = availableWidth?.let {
            minOf(basePillSize, it * 0.24f).coerceAtLeast(14.dp)
        } ?: basePillSize
        val showIcon = availableWidth?.let { amountLabel.length <= 6 || it >= 96.dp } != false
        val horizontalPadding = availableWidth?.let {
            (minOf(height, it) * 0.08f).coerceIn(3.dp, 10.dp)
        } ?: (height * 0.12f).coerceIn(4.dp, 10.dp)
        val iconGap = (height * 0.12f).coerceIn(4.dp, 8.dp)
        Row(
            modifier = cardModifier.padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            if (showIcon) {
                QuickAddIconPill(card, pillSize)
                Spacer(GlanceModifier.width(iconGap))
            }
            if (effectiveShowLabel) {
                Column {
                    Text(
                        text = card.label,
                        style = TextStyle(
                            color = card.colours.content,
                            fontSize = compactTextSize(height, availableWidth, 0.17f, 0.14f, 8f, 13f).sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = amountLabel,
                        style = TextStyle(
                            color = card.colours.content,
                            fontSize = compactTextSize(height, availableWidth, 0.24f, 0.16f, 9f, 16f).sp,
                        ),
                        maxLines = 1,
                    )
                }
            } else {
                Text(
                    text = amountLabel,
                    style = TextStyle(
                        color = card.colours.content,
                        fontSize = compactTextSize(height, availableWidth, 0.28f, 0.16f, 10f, 18f).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Caps compact typography by both card height and the actual horizontal space available. */
private fun compactTextSize(
    height: Dp,
    availableWidth: Dp?,
    heightFactor: Float,
    widthFactor: Float,
    minimum: Float,
    maximum: Float,
): Float {
    val byHeight = height.value * heightFactor
    val constrained = availableWidth?.let { minOf(byHeight, it.value * widthFactor) } ?: byHeight
    return constrained.coerceIn(minimum, maximum)
}

@Composable
private fun QuickAddIconPill(card: QuickAddCardModel, size: Dp) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .background(card.colours.pill)
            .cornerRadius(size * 0.35f),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(card.iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(card.colours.pillContent),
            modifier = GlanceModifier.size(size * 0.55f),
        )
    }
}
