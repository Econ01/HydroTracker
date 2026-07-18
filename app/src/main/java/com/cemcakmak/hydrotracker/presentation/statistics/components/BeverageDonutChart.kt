/*
 *
 *  * HydroTracker - A modern and private water intake tracking application
 *  * Copyright (c) 2026 Ali Cem Çakmak
 *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.cemcakmak.hydrotracker.presentation.statistics.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.BeveragePreferences
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.presentation.common.BlurMorph
import com.cemcakmak.hydrotracker.presentation.common.AnimatedNumber
import com.cemcakmak.hydrotracker.presentation.common.shapes.PillShape
import com.cemcakmak.hydrotracker.presentation.statistics.BeverageBreakdownItem
import com.cemcakmak.hydrotracker.ui.theme.ExtendedColorScheme
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.ui.theme.extendedColorScheme
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * An interactive doughnut chart that visualizes the share of effective hydration by beverage type.
 *
 * The chart is wrapped in a rounded card and uses dynamic colours from the active Material You
 * [ColorScheme]. Tapping a segment (or its legend chip) highlights that slice, pops it outward,
 * and updates the centre label with a blur-morph transition.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BeverageDonutChart(
    items: List<BeverageBreakdownItem>,
    volumeUnit: VolumeUnit,
    strokeWidth: Dp = 40.dp,
    selectedStrokeWidthBoost: Dp = 8.dp,
    segmentGap: Dp = 50.dp,
    explodeDistance: Dp = 20.dp,
    entryDelayMillis: Int = 0
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    var selectedIndex by remember { mutableIntStateOf(-1) }

    val colorRoles = rememberBeverageColorRoles(items)

    val selectionProgress = List(items.size) { index ->
        animateFloatAsState(
            targetValue = if (selectedIndex == index) 1f else 0f,
            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
            label = "segmentSelection$index"
        )
    }

    val dimProgress = List(items.size) { index ->
        animateFloatAsState(
            targetValue = if (selectedIndex == -1 || selectedIndex == index) 1f else 0.35f,
            animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
            label = "segmentDimming$index"
        )
    }

    val iconPainters = remember(items.map { it.iconRes }.toSet()) {
        mutableMapOf<Int, Painter>()
    }
    items.forEach { item ->
        iconPainters[item.iconRes] = painterResource(item.iconRes)
    }

    val centreIndex = if (selectedIndex != -1) selectedIndex else items.indexOfFirst { it.percentage > 0 }
    val centreItem = items.getOrNull(centreIndex)
    val centreColor = colorRoles.getOrNull(centreIndex)?.color ?: MaterialTheme.colorScheme.primary
    val centrePainter = centreItem?.iconRes?.let { iconPainters[it] }
    val centreLabelState = remember(centreItem?.key, centreColor, centreItem?.iconRes) {
        if (centreItem != null && centrePainter != null) {
            CentreLabelState(centreItem, centreColor, centrePainter)
        } else null
    }

    val sectionTitle = stringResource(R.string.statistics_section_beverages)
    val cardDescription = remember(items, sectionTitle) {
        items.joinToString(prefix = "$sectionTitle: ", separator = ", ") {
            "${it.displayName} ${it.percentage.roundToInt()}%"
        }
    }

    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val selectedStrokeWidthBoostPx = with(density) { selectedStrokeWidthBoost.toPx() }
    val gapPx = with(density) { segmentGap.toPx() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(items) {
                    detectTapGestures { offset ->
                        val selected = segmentIndexAtTap(
                            offset = offset,
                            width = size.width.toFloat(),
                            height = size.height.toFloat(),
                            items = items,
                            strokeWidthPx = strokeWidthPx,
                            gapPx = gapPx
                        )
                        if (selected != -1) {
                            selectedIndex = if (selectedIndex == selected) -1 else selected
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                    }
                }
                .semantics { contentDescription = cardDescription },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (items.isEmpty()) return@Canvas

                val strokeWidthPx = strokeWidth.toPx()
                val gapPx = segmentGap.toPx()
                val explodePx = explodeDistance.toPx()
                val diameter = min(size.width, size.height) - strokeWidthPx
                val topLeft = Offset(
                    x = (size.width - diameter) / 2,
                    y = (size.height - diameter) / 2
                )
                val arcSize = Size(diameter, diameter)

                val geometries = computeSegmentGeometry(
                    items = items,
                    width = size.width,
                    height = size.height,
                    strokeWidthPx = strokeWidthPx,
                    gapPx = gapPx
                )

                items.forEachIndexed { index, _ ->
                    val geometry = geometries[index]
                    val arcSweep = geometry.sweepAngle

                    val selection = selectionProgress[index].value
                    val alpha = dimProgress[index].value
                    val explode = selection * explodePx
                    val segmentStrokeWidthPx = strokeWidthPx + selection * selectedStrokeWidthBoostPx

                    val midAngle = geometry.startAngle + arcSweep / 2
                    val midRad = Math.toRadians(midAngle.toDouble())
                    val offsetX = (explode * cos(midRad)).toFloat()
                    val offsetY = (explode * sin(midRad)).toFloat()

                    val colour = colorRoles[index].color

                    drawArc(
                        color = colour.copy(alpha = alpha),
                        startAngle = geometry.startAngle,
                        sweepAngle = arcSweep,
                        useCenter = false,
                        topLeft = Offset(topLeft.x + offsetX, topLeft.y + offsetY),
                        size = arcSize,
                        style = Stroke(width = segmentStrokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }

            centreLabelState?.let { labelState ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BlurMorph(targetState = labelState) { state, blurModifier ->
                        Box(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                modifier = blurModifier.size(50.dp),
                                painter = state.painter,
                                contentDescription = null,
                                tint = state.color
                            )
                        }
                    }

                    AnimatedNumber(
                        targetValue = labelState.item.effectiveAmount,
                        formatValue = { value ->
                            stringResource(
                                R.string.statistics_beverage_effective_short,
                                VolumeUnitConverter.format(context, value.toDouble(), volumeUnit)
                            )
                        },
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        color = labelState.color,
                        hapticsEnabled = false,
                        entryDelayMillis = entryDelayMillis
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                BeverageChip(
                    item = item,
                    roles = colorRoles[index],
                    isSelected = isSelected,
                    onClick = {
                        selectedIndex = if (isSelected) -1 else index
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    }
                )
            }
        }
    }
}

/**
 * Snapshot of the centre label content, used as the [BlurMorph] target state so that the beverage
 * details, theme colour, and resolved icon all cross-fade together.
 */
private data class CentreLabelState(
    val item: BeverageBreakdownItem,
    val color: Color,
    val painter: Painter
)

/**
 * A compact, tappable legend chip for a single beverage slice.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BeverageChip(
    item: BeverageBreakdownItem,
    roles: BeverageColorRoles,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedContainerColor by animateColorAsState(
        targetValue = if (isSelected) {
            roles.color
        } else {
            roles.containerColor
        },
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "chipContainer"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) roles.onColor else roles.onContainerColor,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "chipContent"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "chipScale"
    )

    Surface(
        onClick = onClick,
        shape = PillShape,
        color = animatedContainerColor,
        interactionSource = interactionSource,
        modifier = Modifier
            .height(32.dp)
            .scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.displayName,
                style = if (isSelected) MaterialTheme.typography.labelMediumEmphasized else MaterialTheme.typography.labelMedium,
                color = animatedContentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.percentage.roundToInt()}%",
                style = if (isSelected) MaterialTheme.typography.labelMediumEmphasized else MaterialTheme.typography.labelMedium,
                color = animatedContentColor,
            )
        }
    }
}

/**
 * Derives the complete colour roles for each beverage slice.
 *
 * Preset beverages map to dedicated colours in [ExtendedColorScheme]. Water uses the new
 * harmonized water colour rather than [ColorScheme.primary]. Custom beverages rotate through
 * secondary / tertiary colours and their containers, avoiding adjacent repeats where possible.
 */
@Composable
private fun rememberBeverageColorRoles(
    items: List<BeverageBreakdownItem>
): List<BeverageColorRoles> {
    val colorScheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColorScheme

    return remember(items, colorScheme, extended) {
        val count = items.size
        if (count == 0) return@remember emptyList()

        val customRolesPalette = listOf(
            BeverageColorRoles(
                color = colorScheme.secondary,
                onColor = colorScheme.onSecondary,
                containerColor = colorScheme.secondaryContainer,
                onContainerColor = colorScheme.onSecondaryContainer
            ),
            BeverageColorRoles(
                color = colorScheme.tertiary,
                onColor = colorScheme.onTertiary,
                containerColor = colorScheme.tertiaryContainer,
                onContainerColor = colorScheme.onTertiaryContainer
            )
        )

        var customPaletteIndex = 0
        val result = MutableList<BeverageColorRoles?>(count) { null }

        for (index in items.indices) {
            val key = items[index].key

            result[index] = if (BeveragePreferences.isCustomToken(key)) {
                val previous = result.getOrNull((index - 1).mod(count))
                val next = result.getOrNull((index + 1) % count)

                var candidate: BeverageColorRoles
                var attempts = 0
                do {
                    candidate = customRolesPalette[customPaletteIndex % customRolesPalette.size]
                    customPaletteIndex++
                    attempts++
                } while ((candidate.color == previous?.color || candidate.color == next?.color) &&
                    attempts < customRolesPalette.size * 2
                )

                candidate
            } else {
                beverageColorRolesFor(key, colorScheme, extended)
            }
        }

        result.map { it!! }
    }
}

/**
 * The complete set of theme colours for a single beverage: main accent, text on that accent,
 * container background, and text on the container.
 */
private data class BeverageColorRoles(
    val color: Color,
    val onColor: Color,
    val containerColor: Color,
    val onContainerColor: Color
)

/**
 * Maps a preset beverage key to its dedicated [ExtendedColorScheme] colour roles.
 */
private fun beverageColorRolesFor(
    key: String,
    colorScheme: ColorScheme,
    extended: ExtendedColorScheme
): BeverageColorRoles = when (key) {
    BeverageType.WATER.name -> BeverageColorRoles(
        color = extended.water,
        onColor = extended.onWater,
        containerColor = extended.waterContainer,
        onContainerColor = extended.onWaterContainer
    )
    BeverageType.COFFEE.name -> BeverageColorRoles(
        color = extended.coffee,
        onColor = extended.onCoffee,
        containerColor = extended.coffeeContainer,
        onContainerColor = extended.onCoffeeContainer
    )
    BeverageType.TEA.name -> BeverageColorRoles(
        color = extended.tea,
        onColor = extended.onTea,
        containerColor = extended.teaContainer,
        onContainerColor = extended.onTeaContainer
    )
    BeverageType.SOFT_DRINK.name -> BeverageColorRoles(
        color = extended.softDrink,
        onColor = extended.onSoftDrink,
        containerColor = extended.softDrinkContainer,
        onContainerColor = extended.onSoftDrinkContainer
    )
    BeverageType.ENERGY_DRINK.name -> BeverageColorRoles(
        color = extended.energyDrink,
        onColor = extended.onEnergyDrink,
        containerColor = extended.energyDrinkContainer,
        onContainerColor = extended.onEnergyDrinkContainer
    )
    BeverageType.SPORTS_DRINK.name -> BeverageColorRoles(
        color = extended.sportsDrink,
        onColor = extended.onSportsDrink,
        containerColor = extended.sportsDrinkContainer,
        onContainerColor = extended.onSportsDrinkContainer
    )
    BeverageType.ORAL_REHYDRATION_SOLUTION.name -> BeverageColorRoles(
        color = extended.oralRehydrationSolution,
        onColor = extended.onOralRehydrationSolution,
        containerColor = extended.oralRehydrationSolutionContainer,
        onContainerColor = extended.onOralRehydrationSolutionContainer
    )
    BeverageType.MILK.name -> BeverageColorRoles(
        color = extended.milk,
        onColor = extended.onMilk,
        containerColor = extended.milkContainer,
        onContainerColor = extended.onMilkContainer
    )
    BeverageType.FRUIT_JUICE.name -> BeverageColorRoles(
        color = extended.fruitJuice,
        onColor = extended.onFruitJuice,
        containerColor = extended.fruitJuiceContainer,
        onContainerColor = extended.onFruitJuiceContainer
    )
    else -> BeverageColorRoles(
        color = colorScheme.tertiary,
        onColor = colorScheme.onTertiary,
        containerColor = colorScheme.tertiaryContainer,
        onContainerColor = colorScheme.onTertiaryContainer
    )
}

/**
 * Redistributes [values] so that every entry is at least [minValue], taking the deficit from the
 * largest entries proportionally. The returned list always sums to 100.0.
 */
private fun redistributeWithMinimum(
    values: List<Double>,
    minValue: Double
): List<Double> {
    if (values.isEmpty()) return emptyList()

    val effectiveMin = min(minValue, 100.0 / values.size)
    val result = values.map { maxOf(it, effectiveMin) }.toMutableList()
    var excess = result.sum() - 100.0

    while (excess > 0.001) {
        val aboveMinIndices = result.mapIndexedNotNull { index, value ->
            if (value > effectiveMin) index else null
        }
        if (aboveMinIndices.isEmpty()) break

        val totalAbove = aboveMinIndices.sumOf { result[it] }
        val reductions = aboveMinIndices.map { index ->
            val proposed = excess * result[index] / totalAbove
            val headroom = result[index] - effectiveMin
            index to min(proposed, headroom)
        }
        val totalReduction = reductions.sumOf { it.second }

        reductions.forEach { (index, reduction) ->
            result[index] -= reduction
        }
        excess -= totalReduction
    }

    // If the input values already summed to less than 100 and no segment needed boosting,
    // normalize so the ring always closes.
    val finalSum = result.sum()
    return if (finalSum > 0.0) result.map { it * 100.0 / finalSum } else result
}

/**
 * Pre-computed angular bounds for a single doughnut segment.
 *
 * Angles follow the Compose `drawArc` convention: 0° is at 3 o'clock, positive values sweep
 * clockwise, and the chart starts at -90° (12 o'clock).
 */
private data class SegmentGeometry(
    val startAngle: Float,
    val sweepAngle: Float
)

/**
 * Computes the start angle and sweep angle for every segment using the same math as the
 * drawing code. Sharing this prevents the tap detector and the renderer from drifting apart.
 */
private fun computeSegmentGeometry(
    items: List<BeverageBreakdownItem>,
    width: Float,
    height: Float,
    strokeWidthPx: Float,
    gapPx: Float
): List<SegmentGeometry> {
    if (items.isEmpty()) return emptyList()

    val diameter = min(width, height) - strokeWidthPx
    val outerRadius = diameter / 2 + strokeWidthPx / 2
    val centrelineRadius = diameter / 2
    val gapAngle = if (items.size > 1) {
        (gapPx / outerRadius) * (180f / PI.toFloat())
    } else 0f
    val totalArcAngle = (360f - items.size * gapAngle).coerceAtLeast(1f)
    val minSweepAngle = (strokeWidthPx / centrelineRadius) * (180f / PI.toFloat())
    val minArcPercentage = (minSweepAngle / totalArcAngle).toDouble()
    val adjustedPercentages = redistributeWithMinimum(
        items.map { it.percentage },
        minArcPercentage
    )

    var currentAngle = 270f
    return List(items.size) { index ->
        val arcSweep = (adjustedPercentages[index] / 100.0 * totalArcAngle)
            .toFloat()
            .coerceAtLeast(if (items.size > 1) 0.5f else totalArcAngle)
        val startAngle = currentAngle.normalizedAngle()
        currentAngle += arcSweep + gapAngle
        SegmentGeometry(startAngle = startAngle, sweepAngle = arcSweep)
    }
}

/**
 * Normalizes an angle to the range [0, 360).
 */
private fun Float.normalizedAngle(): Float = ((this % 360f) + 360f) % 360f

/**
 * Returns true when [this] angle lies on the clockwise arc from [start] to [end], inclusive.
 * Handles wrapping across 360°.
 */
private fun Float.isBetweenAngles(start: Float, end: Float): Boolean {
    val normalizedThis = this.normalizedAngle()
    val normalizedStart = start.normalizedAngle()
    val normalizedEnd = end.normalizedAngle()
    return if (normalizedEnd >= normalizedStart) {
        normalizedThis in normalizedStart..normalizedEnd
    } else {
        normalizedThis >= normalizedStart || normalizedThis <= normalizedEnd
    }
}

/**
 * Converts a tap [offset] into the index of the doughnut segment that contains it.
 * Returns -1 if the tap is outside the chart ring.
 */
private fun segmentIndexAtTap(
    offset: Offset,
    width: Float,
    height: Float,
    items: List<BeverageBreakdownItem>,
    strokeWidthPx: Float,
    gapPx: Float
): Int {
    val centreX = width / 2f
    val centreY = height / 2f
    val dx = offset.x - centreX
    val dy = offset.y - centreY
    val distance = hypot(dx, dy)

    val outerRadius = min(width, height) / 2f
    val innerRadius = outerRadius - strokeWidthPx

    val hitTolerance = gapPx / 2
    if (distance !in (innerRadius - hitTolerance)..(outerRadius + hitTolerance)) return -1

    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat().normalizedAngle()

    val geometries = computeSegmentGeometry(
        items = items,
        width = width,
        height = height,
        strokeWidthPx = strokeWidthPx,
        gapPx = gapPx
    )

    val diameter = min(width, height) - strokeWidthPx
    val centrelineRadius = diameter / 2
    val capHalfAngle = (strokeWidthPx / 2 / centrelineRadius) * (180f / PI.toFloat())

    // Iterate in reverse drawing order so caps drawn on top win ties in overlap regions.
    for (index in items.indices.reversed()) {
        val geometry = geometries[index]
        val expandedStart = geometry.startAngle - capHalfAngle
        val expandedEnd = geometry.startAngle + geometry.sweepAngle + capHalfAngle
        if (angle.isBetweenAngles(expandedStart, expandedEnd)) {
            return index
        }
    }
    return -1
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, name = "Beverage Donut Chart")
@Composable
private fun BeverageDonutChartPreview() {
    val items = listOf(
        BeverageBreakdownItem(
            key = BeverageType.WATER.name,
            displayName = "Water",
            iconRes = BeverageType.WATER.iconResFilled,
            color = Color.Unspecified,
            count = 120,
            effectiveAmount = 42000.0,
            percentage = 28.0
        ),
        BeverageBreakdownItem(
            key = BeverageType.COFFEE.name,
            displayName = "Coffee",
            iconRes = BeverageType.COFFEE.iconResFilled,
            color = Color.Unspecified,
            count = 45,
            effectiveAmount = 9500.0,
            percentage = 14.0
        ),
        BeverageBreakdownItem(
            key = BeverageType.TEA.name,
            displayName = "Tea",
            iconRes = BeverageType.TEA.iconResFilled,
            color = Color.Unspecified,
            count = 30,
            effectiveAmount = 7200.0,
            percentage = 12.0
        ),
        BeverageBreakdownItem(
            key = BeverageType.SOFT_DRINK.name,
            displayName = "Soft Drink",
            iconRes = BeverageType.SOFT_DRINK.iconResFilled,
            color = Color.Unspecified,
            count = 20,
            effectiveAmount = 4800.0,
            percentage = 8.0
        ),
        BeverageBreakdownItem(
            key = BeverageType.ENERGY_DRINK.name,
            displayName = "Energy Drink",
            iconRes = BeverageType.ENERGY_DRINK.iconResFilled,
            color = Color.Unspecified,
            count = 18,
            effectiveAmount = 4200.0,
            percentage = 7.0
        ),
        BeverageBreakdownItem(
            key = BeverageType.SPORTS_DRINK.name,
            displayName = "Sports Drink",
            iconRes = BeverageType.SPORTS_DRINK.iconResFilled,
            color = Color.Unspecified,
            count = 20,
            effectiveAmount = 4800.0,
            percentage = 8.0
        ),
        BeverageBreakdownItem(
            key = BeverageType.ORAL_REHYDRATION_SOLUTION.name,
            displayName = "ORS",
            iconRes = BeverageType.ORAL_REHYDRATION_SOLUTION.iconResFilled,
            color = Color.Unspecified,
            count = 10,
            effectiveAmount = 3600.0,
            percentage = 6.0
        ),
        BeverageBreakdownItem(
            key = BeverageType.MILK.name,
            displayName = "Milk",
            iconRes = BeverageType.MILK.iconResFilled,
            color = Color.Unspecified,
            count = 10,
            effectiveAmount = 3600.0,
            percentage = 6.0
        ),
        BeverageBreakdownItem(
            key = BeverageType.FRUIT_JUICE.name,
            displayName = "Fruit Juice",
            iconRes = BeverageType.FRUIT_JUICE.iconResFilled,
            color = Color.Unspecified,
            count = 8,
            effectiveAmount = 3000.0,
            percentage = 5.0
        ),
        BeverageBreakdownItem(
            key = BeveragePreferences.customToken(1L),
            displayName = "Custom Drink",
            iconRes = BeverageType.WATER.iconResFilled,
            color = Color.Unspecified,
            count = 5,
            effectiveAmount = 2400.0,
            percentage = 4.0
        ),
        BeverageBreakdownItem(
            key = BeveragePreferences.customToken(2L),
            displayName = "Another Custom",
            iconRes = BeverageType.WATER.iconResFilled,
            color = Color.Unspecified,
            count = 3,
            effectiveAmount = 1200.0,
            percentage = 2.0
        )
    )

    HydroTrackerTheme {
        BeverageDonutChart(
            items = items,
            volumeUnit = VolumeUnit.MILLILITRES
        )
    }
}
