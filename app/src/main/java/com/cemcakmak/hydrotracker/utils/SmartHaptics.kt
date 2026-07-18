package com.cemcakmak.hydrotracker.utils

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.annotation.SuppressLint
import android.view.View
import androidx.annotation.RequiresApi

/**
 * Smart haptic feedback engine with multi-tier fallback.
 *
 * Problem: OEMs like OnePlus/OPPO/Xiaomi intercept [HapticFeedbackConstants] via their proprietary
 * haptic engines (e.g. O-Haptics) and often collapse all constants into a single generic vibration for
 * third-party apps.
 *
 * Strategy:
 * 1. On "good" OEMs (Pixel, Samsung, etc.) use [HapticFeedbackConstants] — it respects system
 *    settings and is best integrated.
 * 2. On known-broken OEMs with API 31+ and primitive support, use [VibrationEffect.Composition]
 *    primitives — this bypasses the OEM constant-mapping layer.
 * 3. Fallback to [VibrationEffect.createPredefined] on API 29+.
 * 4. Final fallback to legacy [Vibrator.vibrate] on older devices.
 */
object SmartHaptics {

    var isGloballyEnabled: Boolean = true

    // OEMs known to have broken or generic HapticFeedbackConstants mapping for 3rd-party apps.
    private val BROKEN_OEM_SET = setOf(
        "xiaomi"
    )

    // Whether the current device is from an OEM with known broken constant mapping.
    val isBrokenOem: Boolean
        get() = Build.MANUFACTURER.lowercase() in BROKEN_OEM_SET

    /**
     * Play a haptic effect for the given [token].
     *
     * @param context Used to access the [Vibrator] service and (for Tier 1) the window decor view.
     * @param token The semantic haptic token to play.
     */
    fun perform(context: Context, token: SmartHapticToken) {
        if (!isGloballyEnabled) return
        when (resolveTier(context, token)) {
            // Tier 1: HapticFeedbackConstants on good OEMs.
            ForcedTier.CONSTANTS -> {
                val view = getDecorView(context)
                val constant = token.toHapticFeedbackConstant()
                if (view != null && constant != null) {
                    view.performHapticFeedback(constant)
                } else {
                    fallbackVibrate(context, token)
                }
            }

            // Tier 2: Composition primitives on API 31+.
            ForcedTier.PRIMITIVES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    vibrate(context, token.toPrimitiveComposition())
                } else {
                    fallbackVibrate(context, token)
                }
            }

            // Tier 3 (predefined) & Tier 4 (legacy) are both handled by fallbackVibrate.
            ForcedTier.PREDEFINED, ForcedTier.LEGACY, ForcedTier.AUTO -> fallbackVibrate(context, token)
        }
    }

    /**
     * Resolve which tier [perform] will route [token] to on this device/context, *without* playing
     * anything. Single source of truth shared by [perform] and the debug UI, so the displayed tier
     * always matches what actually fires. Never returns [ForcedTier.AUTO].
     *
     * The result can vary per token: a token whose required primitives aren't supported drops a tier,
     * and a constant with no mapping at this API level skips Tier 1.
     */
    fun resolveTier(context: Context, token: SmartHapticToken): ForcedTier {
        // Tier 1: HapticFeedbackConstants on good OEMs with a usable decor view + a mapping.
        if (!isBrokenOem && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return if (getDecorView(context) != null && token.toHapticFeedbackConstant() != null) {
                ForcedTier.CONSTANTS
            } else {
                resolveFallbackTier(token)
            }
        }
        // Tier 2: composition primitives (broken OEMs, or Tier 1 unavailable).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && primitivesSupported(context, token)) {
            return ForcedTier.PRIMITIVES
        }
        // Tier 3 / Tier 4
        return resolveFallbackTier(token)
    }

    /**
     * Human-readable tier label for the debug UI (e.g. "Tier 3 · Predefined"). Uses a representative
     * token by default; pass a specific [token] to see its exact routing.
     */
    fun resolveTierLabel(context: Context, token: SmartHapticToken = SmartHapticToken.Confirm): String =
        when (resolveTier(context, token)) {
            ForcedTier.CONSTANTS -> "Tier 1 · Constants"
            ForcedTier.PRIMITIVES -> "Tier 2 · Primitives"
            ForcedTier.PREDEFINED -> "Tier 3 · Predefined"
            ForcedTier.LEGACY -> "Tier 4 · Legacy"
            ForcedTier.AUTO -> "Auto"
        }

    /**
     * Play a raw [HapticFeedbackConstants] value through the SmartHaptics pipeline.
     */
    fun performRaw(context: Context, constant: Int) {
        if (!isGloballyEnabled) return
        val token = constant.toSmartHapticToken()
        if (token != null) {
            perform(context, token)
        } else {
            // Unknown constant — try direct View path on good OEMs, otherwise generic fallback
            if (!isBrokenOem) {
                getDecorView(context)?.performHapticFeedback(constant)
            } else {
                genericFallbackVibrate(context)
            }
        }
    }

    /**
     * Play a haptic effect forcing a specific [tier].
     *
     * Used by the haptic test screen to bypass OEM-aware routing and compare tiers in isolation.
     */
    fun performForced(context: Context, token: SmartHapticToken, tier: ForcedTier) {
        if (!isGloballyEnabled) return
        when (tier) {
            ForcedTier.AUTO -> perform(context, token)
            ForcedTier.CONSTANTS -> {
                val view = getDecorView(context)
                if (view != null) {
                    val constant = token.toHapticFeedbackConstant()
                    if (constant != null) {
                        view.performHapticFeedback(constant)
                        return
                    }
                }
                fallbackVibrate(context, token)
            }
            ForcedTier.PRIMITIVES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val effect = token.toPrimitiveComposition()
                    if (primitivesSupported(context, token)) {
                        vibrate(context, effect)
                    } else {
                        fallbackVibrate(context, token)
                    }
                } else {
                    fallbackVibrate(context, token)
                }
            }
            ForcedTier.PREDEFINED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val effect = token.toPredefinedEffect()
                    if (effect != null) {
                        vibrate(context, effect)
                        return
                    }
                }
                legacyVibrate(context, token)
            }
            ForcedTier.LEGACY -> legacyVibrate(context, token)
        }
    }

    /**
     * Play an arbitrary primitive composition. Used by the Haptics Primitive Lab to preview custom
     * combinations live without rebuilding the app. The caller is responsible for checking primitive
     * support (e.g. via [Vibrator.arePrimitivesSupported]); unsupported primitives are dropped by the
     * platform. Scales are clamped to [0, 1] and delays to >= 0.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun playPrimitiveSteps(context: Context, steps: List<PrimitiveStep>) {
        if (!isGloballyEnabled) return
        if (steps.isEmpty()) return
        val comp = VibrationEffect.startComposition()
        steps.forEach { step ->
            comp.addPrimitive(
                step.primitiveId,
                step.scale.coerceIn(0f, 1f),
                step.delayMs.coerceAtLeast(0)
            )
        }
        vibrate(context, comp.compose())
    }

    // ---------------------------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------------------------

    private fun fallbackVibrate(context: Context, token: SmartHapticToken) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val effect = token.toPredefinedEffect()
                if (effect != null) {
                    vibrate(context, effect)
                    return
                }
            }
        }
        legacyVibrate(context, token)
    }

    // Mirror of [fallbackVibrate]'s decision: Tier 3 (predefined) on API 29+, else Tier 4 (legacy).
    private fun resolveFallbackTier(token: SmartHapticToken): ForcedTier {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && token.toPredefinedEffect() != null) {
            ForcedTier.PREDEFINED
        } else {
            ForcedTier.LEGACY
        }
    }

    private fun vibrate(context: Context, effect: VibrationEffect) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
            vibrator.vibrate(effect, attributes)
        } else {
            vibrator.vibrate(effect)
        }
    }

    private fun legacyVibrate(context: Context, token: SmartHapticToken) {
        val vibrator = getVibrator(context) ?: return
        val duration = when (token) {
            SmartHapticToken.Confirm,
            SmartHapticToken.ContextClick,
            SmartHapticToken.ToggleOn,
            SmartHapticToken.VirtualKey -> 40L

            SmartHapticToken.Reject -> 100L
            SmartHapticToken.LongPress,
            SmartHapticToken.DragStart -> 80L
            else -> 30L
        }
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun genericFallbackVibrate(context: Context) {
        val vibrator = getVibrator(context) ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun getDecorView(context: Context): View? {
        return when (context) {
            is android.app.Activity -> context.window?.decorView
            else -> null
        }
    }

    // Check whether the primitives required for [token] are supported by this device's vibrator.
    @RequiresApi(Build.VERSION_CODES.S)
    private fun primitivesSupported(context: Context, token: SmartHapticToken): Boolean {
        val vibrator = getVibrator(context) ?: return false
        val primitives = token.requiredPrimitives()
        if (primitives.isEmpty()) return false

        val supportedArray = vibrator.arePrimitivesSupported(*primitives.toIntArray())
        return supportedArray.all { it }
    }

    // ---------------------------------------------------------------------------------------------
    // Mapping tables
    // ---------------------------------------------------------------------------------------------

    @SuppressLint("NewApi")
    private fun SmartHapticToken.toHapticFeedbackConstant(): Int? = when (this) {
        SmartHapticToken.ClockTick -> HapticFeedbackConstants.CLOCK_TICK
        SmartHapticToken.Confirm -> HapticFeedbackConstants.CONFIRM
        SmartHapticToken.ContextClick -> HapticFeedbackConstants.CONTEXT_CLICK
        SmartHapticToken.DragStart -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.DRAG_START
        } else null

        SmartHapticToken.GestureEnd -> HapticFeedbackConstants.GESTURE_END
        SmartHapticToken.GestureStart -> HapticFeedbackConstants.GESTURE_START
        SmartHapticToken.GestureThresholdActive -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
        } else null

        SmartHapticToken.GestureThresholdDeactive -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE
        } else null

        SmartHapticToken.LongPress -> HapticFeedbackConstants.LONG_PRESS
        SmartHapticToken.Reject -> HapticFeedbackConstants.REJECT
        SmartHapticToken.SegmentFrequentTick -> HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        SmartHapticToken.SegmentTick -> HapticFeedbackConstants.SEGMENT_TICK
        SmartHapticToken.TextHandleMove -> HapticFeedbackConstants.TEXT_HANDLE_MOVE
        SmartHapticToken.ToggleOff -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.TOGGLE_OFF
        } else null

        SmartHapticToken.ToggleOn -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.TOGGLE_ON
        } else null

        SmartHapticToken.VirtualKey -> HapticFeedbackConstants.VIRTUAL_KEY
        SmartHapticToken.VirtualKeyRelease -> HapticFeedbackConstants.VIRTUAL_KEY_RELEASE
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun SmartHapticToken.toPrimitiveComposition(): VibrationEffect {
        val comp = VibrationEffect.startComposition()
        when (this) {
            // The user has pressed either an hour or minute tick of a Clock.
            SmartHapticToken.ClockTick -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.8f)

            // A haptic effect to signal the confirmation or successful completion of a user interaction.
            SmartHapticToken.Confirm -> {
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f)
            }

            // The user has performed a context click on an object.
            SmartHapticToken.ContextClick -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f)

            // The user has started a drag-and-drop gesture. Lift (rise) into a firm grab.
            SmartHapticToken.DragStart -> {
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.4f)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f)
            }

            // The user has finished a gesture (e.g. on the soft keyboard). A tick that falls off.
            SmartHapticToken.GestureEnd -> {
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.4f)
            }

            // The user has started a gesture (e.g. on the soft keyboard). A light rising onset.
            SmartHapticToken.GestureStart -> {
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.3f)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f)
            }

            // The user is executing a swipe/drag-style gesture, such as pull-to-refresh,
            // where the gesture action is "eligible" at a certain threshold of movement,
            // and can be cancelled by moving back past the threshold. Snap into "eligible".
            SmartHapticToken.GestureThresholdActive -> {
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f)
            }

            // The user is executing a swipe/drag-style gesture, such as pull-to-refresh,
            // where the gesture action is "eligible" at a certain threshold of movement,
            // and can be cancelled by moving back past the threshold. Un-snap (mirrors Active).
            SmartHapticToken.GestureThresholdDeactive -> {
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.3f)
            }

            // The user has performed a long press on an object that is resulting in an action being performed.
            // Pressure builds (slow rise) then the action fires (firm click).
            SmartHapticToken.LongPress -> {
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.5f)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
            }

            // A haptic effect to signal the rejection or failure of a user interaction.
            // Descending triple "no"; gaps widened to >=50ms so the pulses stay distinct.
            SmartHapticToken.Reject -> {
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 60)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f, 60)
            }

            // The user is switching between a series of many potential choices, for example minutes on a clock face, or individual percentages.
            // Rapid-fire: kept a single very soft tick so repeats stay light.
            SmartHapticToken.SegmentFrequentTick -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.3f)

            // The user is switching between a series of potential choices, for example items in a list or discrete points on a slider.
            // Rapid-fire: single crisp tick.
            SmartHapticToken.SegmentTick -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f)

            // The user has performed a selection/insertion handle move on text field.
            // Rapid-fire: single soft low tick (texture-like).
            SmartHapticToken.TextHandleMove -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.3f)

            // The user has toggled a switch or button into the off position. Firm release that falls away.
            SmartHapticToken.ToggleOff -> {
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.5f)
            }

            // The user has toggled a switch or button into the on position. Rising "snap on".
            SmartHapticToken.ToggleOn -> {
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.15f)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f, 50)
            }

            // The user has pressed on a virtual on-screen key. Rapid-fire: single crisp click.
            SmartHapticToken.VirtualKey -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f)

            // The user has released a virtual key. Rapid-fire: single light tick.
            SmartHapticToken.VirtualKeyRelease -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f)
        }
        return comp.compose()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun SmartHapticToken.requiredPrimitives(): List<Int> = when (this) {
        // Single CLICK
        SmartHapticToken.Confirm,
        SmartHapticToken.ContextClick,
        SmartHapticToken.Reject,
        SmartHapticToken.VirtualKey ->
            listOf(VibrationEffect.Composition.PRIMITIVE_CLICK)

        // Single TICK
        SmartHapticToken.ClockTick,
        SmartHapticToken.SegmentTick,
        SmartHapticToken.VirtualKeyRelease ->
            listOf(VibrationEffect.Composition.PRIMITIVE_TICK)

        // Single LOW_TICK
        SmartHapticToken.SegmentFrequentTick,
        SmartHapticToken.TextHandleMove ->
            listOf(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)

        // QUICK_RISE -> CLICK
        SmartHapticToken.ToggleOn,
        SmartHapticToken.DragStart ->
            listOf(
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_CLICK
            )

        // CLICK -> QUICK_FALL
        SmartHapticToken.ToggleOff ->
            listOf(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
            )

        // QUICK_RISE -> TICK
        SmartHapticToken.GestureStart ->
            listOf(
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_TICK
            )

        // TICK -> QUICK_FALL
        SmartHapticToken.GestureEnd,
        SmartHapticToken.GestureThresholdDeactive ->
            listOf(
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
            )

        // TICK -> CLICK
        SmartHapticToken.GestureThresholdActive ->
            listOf(
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK
            )

        // SLOW_RISE -> CLICK
        SmartHapticToken.LongPress ->
            listOf(
                VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
                VibrationEffect.Composition.PRIMITIVE_CLICK
            )
    }

    @SuppressLint("NewApi")
    private fun SmartHapticToken.toPredefinedEffect(): VibrationEffect? = when (this) {
        SmartHapticToken.Confirm -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        SmartHapticToken.Reject -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        SmartHapticToken.ContextClick,
        SmartHapticToken.ToggleOn,
        SmartHapticToken.VirtualKey,
        SmartHapticToken.GestureThresholdActive -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)

        SmartHapticToken.LongPress,
        SmartHapticToken.DragStart -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)

        SmartHapticToken.ClockTick,
        SmartHapticToken.SegmentTick,
        SmartHapticToken.SegmentFrequentTick,
        SmartHapticToken.TextHandleMove,
        SmartHapticToken.GestureStart,
        SmartHapticToken.GestureEnd,
        SmartHapticToken.GestureThresholdDeactive,
        SmartHapticToken.VirtualKeyRelease,
        SmartHapticToken.ToggleOff -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
    }

    private fun Int.toSmartHapticToken(): SmartHapticToken? = when (this) {
        HapticFeedbackConstants.CLOCK_TICK -> SmartHapticToken.ClockTick
        HapticFeedbackConstants.CONFIRM -> SmartHapticToken.Confirm
        HapticFeedbackConstants.CONTEXT_CLICK -> SmartHapticToken.ContextClick
        HapticFeedbackConstants.GESTURE_END -> SmartHapticToken.GestureEnd
        HapticFeedbackConstants.GESTURE_START -> SmartHapticToken.GestureStart
        HapticFeedbackConstants.LONG_PRESS -> SmartHapticToken.LongPress
        HapticFeedbackConstants.REJECT -> SmartHapticToken.Reject
        HapticFeedbackConstants.SEGMENT_FREQUENT_TICK -> SmartHapticToken.SegmentFrequentTick
        HapticFeedbackConstants.SEGMENT_TICK -> SmartHapticToken.SegmentTick
        HapticFeedbackConstants.TEXT_HANDLE_MOVE -> SmartHapticToken.TextHandleMove
        HapticFeedbackConstants.VIRTUAL_KEY -> SmartHapticToken.VirtualKey
        HapticFeedbackConstants.VIRTUAL_KEY_RELEASE -> SmartHapticToken.VirtualKeyRelease
        else -> null
    }

}

/**
 * Forced tier selector for the haptic test screen.
 * Bypasses the normal OEM-aware routing so each tier can be tested in isolation.
 */
enum class ForcedTier {
    AUTO,
    CONSTANTS,
    PRIMITIVES,
    PREDEFINED,
    LEGACY
}

/**
 * One primitive in a custom composition built in the Haptics Primitive Lab.
 *
 * @param primitiveId a [VibrationEffect.Composition] PRIMITIVE_* id.
 * @param scale intensity 0.0–1.0 (0.0 = minimum perceivable, not off).
 * @param delayMs pause in milliseconds before this primitive (since the previous one ended).
 */
data class PrimitiveStep(
    val primitiveId: Int,
    val scale: Float,
    val delayMs: Int = 0
)

/**
 * Semantic haptic tokens. These are hardware-agnostic names for haptic events.
 */
enum class SmartHapticToken {
    ClockTick,
    Confirm,
    ContextClick,
    DragStart,
    GestureEnd,
    GestureStart,
    GestureThresholdActive,
    GestureThresholdDeactive,
    LongPress,
    Reject,
    SegmentFrequentTick,
    SegmentTick,
    TextHandleMove,
    ToggleOff,
    ToggleOn,
    VirtualKey,
    VirtualKeyRelease
}
