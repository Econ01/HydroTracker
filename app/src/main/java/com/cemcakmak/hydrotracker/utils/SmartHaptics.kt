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

    // OEMs known to have broken or generic HapticFeedbackConstants mapping for 3rd-party apps.
    private val COMPATIBLE_OEM_SET = setOf(
        "google",
        "samsung",
    )

    // Whether the current device is from an OEM with known broken constant mapping.
    val isBrokenOem: Boolean
        get() = Build.MANUFACTURER.lowercase() !in COMPATIBLE_OEM_SET

    /**
     * Play a haptic effect for the given [token].
     *
     * @param context Used to access the [Vibrator] service and (for Tier 1) the window decor view.
     * @param token The semantic haptic token to play.
     */
    fun perform(context: Context, token: SmartHapticToken) {
        when {
            // Tier 1: HapticFeedbackConstants on good OEMs (API 27+ for most rich constants)
            !isBrokenOem && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> {
                val view = getDecorView(context)
                if (view != null) {
                    val constant = token.toHapticFeedbackConstant()
                    if (constant != null) {
                        view.performHapticFeedback(constant)
                        return
                    }
                }
                // Fall through if no view or no mapping
                fallbackVibrate(context, token)
            }

            // Tier 2: Composition primitives on API 31+ (broken OEMs or missing Tier 1 mapping)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val effect = token.toPrimitiveComposition()
                if (primitivesSupported(context, token)) {
                    vibrate(context, effect)
                } else {
                    fallbackVibrate(context, token)
                }
            }

            // Tier 3 & 4: Predefined or legacy
            else -> fallbackVibrate(context, token)
        }
    }

    /**
     * Play a raw [HapticFeedbackConstants] value through the SmartHaptics pipeline.
     */
    fun performRaw(context: Context, constant: Int) {
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
            SmartHapticToken.ClockTick -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)

            // A haptic effect to signal the confirmation or successful completion of a user interaction.
            SmartHapticToken.Confirm -> {
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 50)
            }

            // The user has performed a context click on an object.
            SmartHapticToken.ContextClick -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f)

            // The user has started a drag-and-drop gesture.
            SmartHapticToken.DragStart -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.9f)

            // The user has finished a gesture (e.g. on the soft keyboard).
            SmartHapticToken.GestureEnd -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.4f)

            // The user has started a gesture (e.g. on the soft keyboard).
            SmartHapticToken.GestureStart -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)

            // The user is executing a swipe/drag-style gesture, such as pull-to-refresh,
            // where the gesture action is "eligible" at a certain threshold of movement,
            // and can be canceled by moving back past the threshold.
            SmartHapticToken.GestureThresholdActive -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f)

            // The user is executing a swipe/drag-style gesture, such as pull-to-refresh,
            // where the gesture action is "eligible" at a certain threshold of movement,
            // and can be canceled by moving back past the threshold.
            SmartHapticToken.GestureThresholdDeactive -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f)

            // The user has performed a long press on an object that is resulting in an action being performed.
            SmartHapticToken.LongPress -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f)

            // A haptic effect to signal the rejection or failure of a user interaction.
            SmartHapticToken.Reject -> {
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 30)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 30)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f, 30)
            }

            // The user is switching between a series of many potential choices, for example minutes on a clock face, or individual percentages.
            SmartHapticToken.SegmentFrequentTick -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.3f)

            // The user is switching between a series of potential choices, for example items in a list or discrete points on a slider.
            SmartHapticToken.SegmentTick -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f)

            // The user has performed a selection/insertion handle move on text field.
            SmartHapticToken.TextHandleMove -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.2f)

            // The user has toggled a switch or button into the off position.
            SmartHapticToken.ToggleOff -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.6f)

            // The user has toggled a switch or button into the on position.
            SmartHapticToken.ToggleOn -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f)

            // The user has pressed on a virtual on-screen key.
            SmartHapticToken.VirtualKey -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f)

            // The user has released a virtual key.
            SmartHapticToken.VirtualKeyRelease -> comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.3f)
        }
        return comp.compose()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun SmartHapticToken.requiredPrimitives(): List<Int> = when (this) {
        SmartHapticToken.ClockTick,
        SmartHapticToken.Confirm,
        SmartHapticToken.ContextClick,
        SmartHapticToken.GestureStart,
        SmartHapticToken.GestureThresholdActive,
        SmartHapticToken.GestureThresholdDeactive,
        SmartHapticToken.Reject,
        SmartHapticToken.SegmentTick,
        SmartHapticToken.TextHandleMove,
        SmartHapticToken.ToggleOn,
        SmartHapticToken.VirtualKey,
        SmartHapticToken.VirtualKeyRelease ->
            listOf(VibrationEffect.Composition.PRIMITIVE_CLICK, VibrationEffect.Composition.PRIMITIVE_TICK)

        SmartHapticToken.SegmentFrequentTick,
        SmartHapticToken.GestureEnd,
        SmartHapticToken.ToggleOff ->
            listOf(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)

        SmartHapticToken.DragStart,
        SmartHapticToken.LongPress ->
            listOf(VibrationEffect.Composition.PRIMITIVE_THUD)
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
