package com.cemcakmak.hydrotracker.presentation.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Ease
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

@Composable
fun <T> BlurMorph(
    modifier: Modifier = Modifier,
    targetState: T,
    content: @Composable (state: T, blurModifier: Modifier) -> Unit
) {
    val fadeSpec = tween<Float>(durationMillis = 600, delayMillis = 100, easing = EaseOut)
    val blurSpec = tween<Dp>(durationMillis = 800, easing = EaseOut)
    AnimatedContent(
        modifier = modifier.graphicsLayer(clip = false),
        targetState = targetState,
        transitionSpec = {
            (fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec)) using SizeTransform(clip = false)
        },
        label = "standardInfo"
    ) { state ->
        val blur by transition.animateDp(
            transitionSpec = { blurSpec },
            label = "standardInfoBlur"
        ) { enterExit ->
            if (enterExit == EnterExitState.Visible) 0.dp else 12.dp
        }
        content(state, Modifier.blur(blur, BlurredEdgeTreatment.Unbounded))
    }
}

private const val DEFAULT_ENTRY_STEP_SPACING_MS = 150

/** Default delay applied to entry animations across the app. */
object EntryAnimationDefaults {
    const val DELAY_MS = 200
}

/** Extracts the first formatted numeric portion from [text], including grouping separators. */
private fun extractNumericPortion(text: String): String? {
    val match = Regex("""[0-9][\d\s.,\u00A0]*""").find(text)
    return match?.value?.trim()
}

/** Parses a numeric string formatted for [locale] into a [Double]. */
private fun parseFormattedNumber(text: String, locale: Locale): Double? {
    return try {
        NumberFormat.getNumberInstance(locale).parse(text)?.toDouble()
    } catch (_: Exception) {
        null
    }
}

/** Counts the number of decimal places shown in [numericText] for [locale]. */
private fun countDecimalPlaces(numericText: String, locale: Locale): Int {
    val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    val index = numericText.indexOfLast { it == decimalSeparator }
    return if (index == -1) 0 else numericText.length - index - 1
}

/**
 * Pads the first numeric portion of [text] so it always shows [decimalPlaces] decimal places,
 * using [locale]'s decimal separator. This keeps the number width stable while rolling through
 * whole-number intermediate values (e.g. "52" becomes "52.0" when the final value is "51.5").
 */
private fun padNumericDecimalPlaces(text: String, decimalPlaces: Int, locale: Locale): String {
    if (decimalPlaces <= 0) return text
    val match = Regex("""[0-9][\d\s.,\u00A0]*""").find(text) ?: return text
    val numeric = match.value
    val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    val separatorIndex = numeric.indexOfLast { it == decimalSeparator }
    val paddedNumeric = if (separatorIndex != -1) {
        val existingDecimals = numeric.length - separatorIndex - 1
        if (existingDecimals >= decimalPlaces) {
            numeric
        } else {
            numeric + "0".repeat(decimalPlaces - existingDecimals)
        }
    } else {
        numeric + decimalSeparator + "0".repeat(decimalPlaces)
    }
    return text.take(match.range.first) + paddedNumeric + text.drop(match.range.last + 1)
}

/**
 * Displays a numeric value with a rolling-digit entry animation followed by rolling-digit updates.
 *
 * On first composition the value starts at zero and rolls to [targetValue]. Subsequent changes to
 * [targetValue] are rendered with [RollingNumberText] so only the digits that change roll.
 *
 * @param targetValue the value to display.
 * @param formatValue formats [targetValue] into the string to display.
 * @param modifier applied to the text container.
 * @param style text style applied to the number.
 * @param color text colour; [Color.Unspecified] follows the ambient content colour.
 * @param suffix optional static text appended after the number (e.g. a unit label). When supplied,
 * it is rendered in its own character slots so the whole block shares a single baseline.
 * @param suffixStyle text style applied to [suffix]; defaults to [style].
 * @param hapticsEnabled when true, haptic feedback fires on value changes after entry.
 * @param direction roll direction for [RollingNumberText].
 * @param effects effect recipe for [RollingNumberText].
 * @param animationConfig spring and timing parameters for [RollingNumberText].
 * @param animateEntry when false, the entry roll is skipped and the value is shown directly with
 * rolling updates only on subsequent changes.
 * @param entryDelayMillis delay before the entry roll starts, in milliseconds.
 * @param entryStepCount number of linear near-target steps used for the entry roll. The default
 * of 3 gives a fast start and a slow landing.
 * @param entryStepSpacingMillis base spacing between entry steps. The actual interval is scaled
 * by [FastOutLinearInEasing] so the animation starts quickly and decelerates toward the target.
 */
@Composable
fun AnimatedNumber(
    targetValue: Double,
    formatValue: @Composable (Float) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    suffix: String? = null,
    suffixStyle: TextStyle? = null,
    hapticsEnabled: Boolean = true,
    direction: RollDirection = RollDirection.Auto,
    effects: RollingEffects = RollingEffects.Full,
    animationConfig: RollingNumberConfig = RollingNumberConfig(),
    animateEntry: Boolean = true,
    entryDelayMillis: Int = 0,
    entryStepCount: Int = 10,
    entryStepSpacingMillis: Int = DEFAULT_ENTRY_STEP_SPACING_MS
) {
    data class DisplayState(val value: Double, val isEntryStart: Boolean)

    var state by remember {
        mutableStateOf(
            if (animateEntry) DisplayState(0.0, true) else DisplayState(targetValue, false)
        )
    }

    val finalText = formatValue(targetValue.toFloat())
    val entryText = remember(finalText) {
        finalText.map { if (it.isDigit()) '0' else it }.joinToString("")
    }

    val locale = LocalConfiguration.current.locales[0]
    val stepCount = entryStepCount.coerceAtLeast(1)
    val numericPortion = remember(finalText) { extractNumericPortion(finalText) }
    val displayValue = remember(numericPortion, locale) {
        numericPortion?.let { parseFormattedNumber(it, locale) }
    }
    val decimalPlaces = remember(numericPortion, locale) {
        numericPortion?.let { countDecimalPlaces(it, locale) } ?: 0
    }

    val rawStepSize = remember(targetValue, displayValue, decimalPlaces, stepCount, locale) {
        if (displayValue != null && displayValue != 0.0) {
            val displayStep = 10.0.pow(-decimalPlaces)
            (targetValue / displayValue) * displayStep
        } else if (targetValue != 0.0) {
            targetValue / stepCount
        } else {
            0.0
        }
    }

    val steps = remember(targetValue, stepCount, rawStepSize) {
        (1..stepCount)
            .map { i -> (targetValue - (stepCount - i) * rawStepSize).coerceAtLeast(0.0) }
            .distinct()
    }

    val stepIntervals = remember(stepCount, entryStepSpacingMillis) {
        if (stepCount <= 1) {
            emptyList()
        } else {
            List(stepCount - 1) { index ->
                val fraction = (index + 1) / stepCount.toFloat()
                (entryStepSpacingMillis * Ease.transform(fraction)).toLong()
            }
        }
    }

    LaunchedEffect(targetValue) {
        if (state.isEntryStart) {
            delay(entryDelayMillis.milliseconds)
            steps.forEachIndexed { index, step ->
                state = DisplayState(step, isEntryStart = false)
                if (index < steps.lastIndex) {
                    delay(stepIntervals[index].milliseconds)
                }
            }
        } else {
            state = state.copy(value = targetValue)
        }
    }

    val formattedValue: @Composable (Float) -> String = { value ->
        if (state.isEntryStart) {
            entryText
        } else {
            padNumericDecimalPlaces(formatValue(value), decimalPlaces, locale)
        }
    }

    RollingNumberText(
        targetValue = state.value,
        formatValue = formattedValue,
        modifier = modifier,
        style = style,
        color = color,
        suffix = suffix,
        suffixStyle = suffixStyle,
        hapticsEnabled = hapticsEnabled,
        direction = direction,
        effects = effects,
        animationConfig = animationConfig,
        skipEntryAnimation = true
    )
}

/**
 * Direction in which rolling digits travel in [RollingNumberText].
 *
 * [Up] means the incoming character enters from below and the outgoing one leaves upwards
 * (the natural direction for an increasing value); [Down] is the reverse. [Auto] infers the
 * direction from the sign of the value change.
 */
enum class RollDirection { Auto, Up, Down }

/**
 * Effect recipe applied to changing characters in [RollingNumberText].
 *
 * [Full] combines the vertical roll with a blur that sharpens as the character lands and a
 * subtle spring scale pop. [Subtle] is the faithful numericText behaviour: slide and fade only.
 */
enum class RollingEffects { Full, Subtle }

/**
 * Tunable parameters for [RollingNumberText].
 *
 * All springs are described by their damping ratio and stiffness so they can be adjusted
 * independently. A damping ratio of 1.0 is critically damped (no overshoot); values below 1.0
 * produce increasing amounts of overshoot. Stiffness controls how fast the spring settles.
 */
data class RollingNumberConfig(
    /** Changes arriving faster than this are rendered instantly instead of animated. */
    val rapidChangeThresholdMs: Int = 0,

    /** Scale a transitioning character starts from (enter) and shrinks to (exit). */
    val transitionScale: Float = 0.4f,

    /** Radius applied to a transitioning character while it is not fully visible. */
    val blurRadius: Dp = 6.dp,

    /** Damping ratio for the vertical slide spring. */
    val slideDampingRatio: Float = 0.7f,

    /** Stiffness for the vertical slide spring. */
    val slideStiffness: Float = Spring.StiffnessMediumLow,

    /** Damping ratio for the scale pop spring. */
    val scaleDampingRatio: Float = 0.7f,

    /** Stiffness for the scale pop spring. */
    val scaleStiffness: Float = Spring.StiffnessMedium,

    /** Damping ratio for the blur spring. */
    val blurDampingRatio: Float = Spring.DampingRatioNoBouncy,

    /** Stiffness for the blur spring. */
    val blurStiffness: Float = Spring.StiffnessHigh,

    /** Damping ratio for the fade spring. */
    val fadeDampingRatio: Float = Spring.DampingRatioNoBouncy,

    /** Stiffness for the fade spring. */
    val fadeStiffness: Float = Spring.StiffnessMedium,

    /** Maximum X-axis rotation applied during digit-to-digit transitions. 0 = flat slide only. */
    val rotationAngle: Float = 40f,

    /** Camera distance for the 3-D rotation; lower values exaggerate perspective. */
    val cameraDistance: Float = 8f
)

/** Internal constants that do not need to be exposed in [RollingNumberConfig]. */
private object RollingNumberDefaults {
    /** Maximum number of character slots to keep ready for rolling animations. Extra slots
     *  are rendered with empty content and zero width, so they take no space until a character
     *  appears and the slot can animate in. */
    const val MAX_ROLLING_SLOTS = 64
}

/**
 * Text that displays a numeric value with a rolling-digit animation.
 *
 * The formatted [targetValue] is rendered as right-aligned character slots; when the value
 * changes, only the characters that change animate. Digits roll vertically — entering from below
 * when the value increases and from above when it decreases — with spring physics that let the
 * incoming digit slightly overshoot its landing position before settling, plus a blur that
 * sharpens and a subtle scale pop (see [RollingEffects]). Characters that do not change (units,
 * separators, static words) stay perfectly still, so [formatValue] may embed the number in a
 * longer string such as "1.2 of 2.5 L". Digits are rendered with tabular (`tnum`) figures so
 * their width stays stable mid-roll.
 *
 * Changes arriving faster than roughly [RollingNumberConfig.rapidChangeThresholdMs]
 * milliseconds apart (e.g. while dragging a slider) are rendered instantly without animation, so
 * the text always stays visible.
 *
 * @param targetValue the value to display.
 * @param formatValue formats [targetValue] into the string to display.
 * @param modifier applied to the row of character slots.
 * @param style text style applied to every character slot.
 * @param color text colour; [Color.Unspecified] follows the ambient content colour.
 * @param suffix optional static text appended after the formatted value. It is rendered in its
 * own slots with [suffixStyle] so the whole block shares a single baseline.
 * @param suffixStyle text style applied to [suffix]; defaults to [style].
 * @param hapticsEnabled when true, a single haptic tick fires per value change (suppressed during
 * rapid change streams).
 * @param direction roll direction; [RollDirection.Auto] follows the sign of the value change.
 * @param effects [RollingEffects.Full] for slide + fade + blur + scale, [RollingEffects.Subtle]
 * for slide + fade only.
 * @param animationConfig spring and timing parameters that control the animation feel.
 * @param skipEntryAnimation when true, the first value is rendered instantly instead of playing
 * the per-character entrance animation.
 */
@Composable
fun RollingNumberText(
    targetValue: Double,
    formatValue: @Composable (Float) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    suffix: String? = null,
    suffixStyle: TextStyle? = null,
    hapticsEnabled: Boolean = true,
    direction: RollDirection = RollDirection.Auto,
    effects: RollingEffects = RollingEffects.Full,
    animationConfig: RollingNumberConfig = RollingNumberConfig(),
    skipEntryAnimation: Boolean = false
) {
    val haptics = LocalHapticFeedback.current

    // Plain remembered holders (not snapshot state) so updating them never triggers
    // recomposition; the direction must be computed against the value from the previous
    // composition, and the haptic gate against the previous change time.
    val previousValue = remember { floatArrayOf(targetValue.toFloat()) }
    val lastChangeTime = remember { longArrayOf(0L) }
    val hasChangedOnce = remember { booleanArrayOf(false) }

    val changeDirection =
        if (targetValue.toFloat() >= previousValue[0]) RollDirection.Up else RollDirection.Down
    SideEffect { previousValue[0] = targetValue.toFloat() }

    // One haptic tick per discrete value change, suppressed while changes stream in faster than
    // the rapid-change threshold (e.g. during slider drags).
    LaunchedEffect(targetValue) {
        if (!hasChangedOnce[0]) {
            hasChangedOnce[0] = true
        } else {
            val now = System.nanoTime()
            if (hapticsEnabled &&
                now - lastChangeTime[0] >
                animationConfig.rapidChangeThresholdMs * 1_000_000L
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            }
            lastChangeTime[0] = now
        }
    }

    val displayedSuffix = suffix?.let { " ${it.trimStart()}" }
    RollingText(
        text = formatValue(targetValue.toFloat()) + (displayedSuffix ?: ""),
        suffix = displayedSuffix,
        direction = if (direction == RollDirection.Auto) changeDirection else direction,
        effects = effects,
        modifier = modifier,
        style = style,
        color = color,
        suffixStyle = suffixStyle,
        animationConfig = animationConfig,
        skipEntryAnimation = skipEntryAnimation
    )
}

/**
 * Renders [text] as a row of right-aligned character slots. Each slot is keyed by its position
 * from the right, so unchanged characters never animate while changed ones roll, fade, blur and
 * scale according to [direction] and [effects]. Slots that only involve non-digit characters
 * crossfade instead of rolling.
 *
 * When [suffix] is supplied, the trailing characters that match it are stripped from the rolling
 * portion and rendered with [suffixStyle]. This keeps the number and its unit label on the same
 * baseline even when they use different font sizes.
 *
 * When changes arrive faster than [RollingNumberConfig.rapidChangeThresholdMs] milliseconds
 * apart (e.g. a slider drag updating every frame), the animation is skipped entirely and the new
 * text is shown instantly — restarting a transition every frame would keep the digits perpetually
 * mid-enter and nearly invisible.
 */
@Composable
private fun RollingText(
    modifier: Modifier = Modifier,
    text: String,
    suffix: String? = null,
    direction: RollDirection,
    effects: RollingEffects,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    suffixStyle: TextStyle? = null,
    animationConfig: RollingNumberConfig = RollingNumberConfig(),
    skipEntryAnimation: Boolean = false
) {
    val effectiveSuffix = suffix ?: ""
    val baseText = if (effectiveSuffix.isNotEmpty() && text.endsWith(effectiveSuffix)) {
        text.dropLast(effectiveSuffix.length)
    } else {
        text
    }
    val fullText = baseText + effectiveSuffix
    val effectiveSuffixStyle = suffixStyle ?: style

    val mergedStyle = remember(style) {
        style.copy(fontFeatureSettings = "tnum")
    }
    val mergedSuffixStyle = remember(effectiveSuffixStyle) {
        effectiveSuffixStyle.copy(fontFeatureSettings = "tnum")
    }

    // Rapid-change guard: animate only when this change arrives after the previous one has had
    // time to finish animating. Plain holder; a stale write from an abandoned composition only
    // ever downgrades an animation to an instant swap, never breaks state.
    val lastChangeTime = remember { longArrayOf(0L) }
    val hasAnimatedOnce = remember { booleanArrayOf(false) }
    val animateChanges = remember(text) {
        val now = System.nanoTime()
        val animate = if (skipEntryAnimation && !hasAnimatedOnce[0]) {
            hasAnimatedOnce[0] = true
            false
        } else {
            now - lastChangeTime[0] >
                animationConfig.rapidChangeThresholdMs * 1_000_000L
        }
        lastChangeTime[0] = now
        animate
    }

    // Rebuild the spring specs whenever the tuning config changes so new transitions pick up
    // the new feel immediately.
    val slideSpring = remember(animationConfig) {
        spring<IntOffset>(
            dampingRatio = animationConfig.slideDampingRatio,
            stiffness = animationConfig.slideStiffness
        )
    }
    val scaleSpring = remember(animationConfig) {
        spring<Float>(
            dampingRatio = animationConfig.scaleDampingRatio,
            stiffness = animationConfig.scaleStiffness
        )
    }
    val blurSpring = remember(animationConfig) {
        spring<Dp>(
            dampingRatio = animationConfig.blurDampingRatio,
            stiffness = animationConfig.blurStiffness
        )
    }
    val fadeSpring = remember(animationConfig) {
        spring<Float>(
            dampingRatio = animationConfig.fadeDampingRatio,
            stiffness = animationConfig.fadeStiffness
        )
    }
    val rotationSpring = remember(animationConfig) {
        spring<Float>(
            dampingRatio = animationConfig.slideDampingRatio,
            stiffness = animationConfig.slideStiffness
        )
    }

    // Allocate a slot pool sized to the longest text seen so far plus one extra slot for
    // incoming leading characters. Empty slots collapse to zero width, so a smaller pool does
    // not change the visible layout. A hard ceiling is kept for safety.
    val maxLength = remember { intArrayOf(0) }
    val slotCount = min(
        max(maxLength[0], fullText.length) + 1,
        RollingNumberDefaults.MAX_ROLLING_SLOTS
    )
    SideEffect { maxLength[0] = max(maxLength[0], fullText.length) }

    Row(modifier = modifier) {
        for (rightIndex in slotCount - 1 downTo 0) {
            val charIndex = fullText.length - 1 - rightIndex
            val char = if (charIndex >= 0) fullText.getOrNull(charIndex) else null
            val isSuffixSlot = charIndex >= baseText.length
            key(rightIndex) {
                var previousChar by remember { mutableStateOf<Char?>(null) }
                SideEffect { previousChar = char }

                AnimatedContent(
                    modifier = Modifier.alignByBaseline(),
                    targetState = char,
                    transitionSpec = {
                        if (!animateChanges) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val rolling =
                                initialState?.isDigit() == true || targetState?.isDigit() == true
                            if (rolling) {
                                (slideInVertically(slideSpring) { fullHeight ->
                                    if (direction == RollDirection.Up) fullHeight else -fullHeight
                                } + fadeIn(fadeSpring)) togetherWith
                                    (slideOutVertically(slideSpring) { fullHeight ->
                                        if (direction == RollDirection.Up) -fullHeight else fullHeight
                                    } + fadeOut(fadeSpring))
                            } else {
                                fadeIn(fadeSpring) togetherWith
                                    fadeOut(fadeSpring)
                            }
                        } using SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.Center,
                    label = "rollingChar"
                ) { currentChar ->
                    val hasDigit =
                        previousChar?.isDigit() == true || char?.isDigit() == true

                    val blur by transition.animateDp(
                        transitionSpec = { blurSpring },
                        label = "rollingCharBlur"
                    ) { enterExit ->
                        if (enterExit == EnterExitState.Visible || effects == RollingEffects.Subtle) {
                            0.dp
                        } else {
                            animationConfig.blurRadius
                        }
                    }
                    val scale by transition.animateFloat(
                        transitionSpec = { scaleSpring },
                        label = "rollingCharScale"
                    ) { enterExit ->
                        if (effects == RollingEffects.Subtle) {
                            1f
                        } else {
                            if (enterExit == EnterExitState.Visible) {
                                1f
                            } else {
                                animationConfig.transitionScale
                            }
                        }
                    }
                    val rotationX by transition.animateFloat(
                        transitionSpec = { rotationSpring },
                        label = "rollingCharRotation"
                    ) { enterExit ->
                        if (!hasDigit || animationConfig.rotationAngle == 0f) {
                            0f
                        } else {
                            val angle = animationConfig.rotationAngle
                            when (enterExit) {
                                EnterExitState.PreEnter ->
                                    if (direction == RollDirection.Up) -angle else angle
                                EnterExitState.Visible -> 0f
                                EnterExitState.PostExit ->
                                    if (direction == RollDirection.Up) angle else -angle
                            }
                        }
                    }
                    val blurModifier = if (blur > 0.dp) {
                        Modifier.blur(blur, BlurredEdgeTreatment.Unbounded)
                    } else {
                        Modifier
                    }

                    Text(
                        // Empty text keeps an absent slot at zero width so the Row can shrink or
                        // grow smoothly; non-breaking spaces keep genuine spaces measurable.
                        text = currentChar?.toString()?.replace(' ', '\u00A0') ?: "",
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.rotationX = rotationX
                                this.cameraDistance = animationConfig.cameraDistance
                            }
                            .then(blurModifier),
                        style = if (isSuffixSlot) mergedSuffixStyle else mergedStyle,
                        color = color,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, name = "Rolling number — auto cycle")
@Composable
private fun RollingNumberAutoPreview() {
    HydroTrackerTheme {
        var target by remember { mutableDoubleStateOf(0.0) }
        LaunchedEffect(Unit) {
            val steps = listOf(250.0, 780.0, 995.0, 1250.0, 640.0, 1750.0, 2000.0, 0.0)
            while (true) {
                for (step in steps) {
                    target = step
                    delay(1500.milliseconds)
                }
            }
        }
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Auto-cycling value",
                style = MaterialTheme.typography.labelMedium
            )
            RollingNumberText(
                targetValue = target,
                formatValue = { value -> "${value.toInt()} of 2000 ml" },
                style = MaterialTheme.typography.displayMediumEmphasized,
                hapticsEnabled = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, name = "Rolling number — interactive")
@Composable
private fun RollingNumberInteractivePreview() {
    HydroTrackerTheme {
        var target by remember { mutableDoubleStateOf(1250.0) }
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Full (slide + fade + blur + scale)",
                style = MaterialTheme.typography.labelMedium
            )
            RollingNumberText(
                targetValue = target,
                formatValue = { value -> "${value.toInt()} of 2000 ml" },
                style = MaterialTheme.typography.headlineMediumEmphasized,
                hapticsEnabled = false
            )
            Text(
                text = "Subtle (slide + fade)",
                style = MaterialTheme.typography.labelMedium
            )
            RollingNumberText(
                targetValue = target,
                formatValue = { value -> "${value.toInt()} of 2000 ml" },
                style = MaterialTheme.typography.headlineMediumEmphasized,
                hapticsEnabled = false,
                effects = RollingEffects.Subtle
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { target = (target - 100).coerceAtLeast(0.0) }) {
                    Text("−100")
                }
                TextButton(onClick = { target += 50 }) { Text("+50") }
                TextButton(onClick = { target += 250 }) { Text("+250") }
                TextButton(onClick = { target = (0..2000).random().toDouble() }) {
                    Text("Random")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, name = "Animated number — entry from zero")
@Composable
private fun AnimatedNumberEntryPreview() {
    HydroTrackerTheme {
    val locale = LocalConfiguration.current.locales[0]
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Entry animation (51.5 L)",
                style = MaterialTheme.typography.labelMedium
            )
            AnimatedNumber(
                targetValue = 51.5,
                formatValue = { value -> String.format(locale, "%.1f L", value) },
                style = MaterialTheme.typography.displayMediumEmphasized,
                hapticsEnabled = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, name = "BlurMorph — auto cycle")
@Composable
private fun BlurMorphPreview() {
    HydroTrackerTheme {
        val phrases = listOf(
            "Every 30 minutes",
            "Every 2 hours",
            "Only when behind goal",
            "Every 45 minutes"
        )
        var index by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(2000.milliseconds)
                index = (index + 1) % phrases.size
            }
        }
        Column(modifier = Modifier.padding(24.dp)) {
            BlurMorph(targetState = phrases[index]) { text, blurModifier ->
                Text(
                    text = text,
                    modifier = blurModifier,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}
