package com.cemcakmak.hydrotracker.utils

import android.content.Context
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * A Compose [HapticFeedback] implementation that delegates to [SmartHaptics].
 *
 * Usage: provide this via [androidx.compose.ui.platform.LocalHapticFeedback] at the root of your
 * composition so that every existing `LocalHapticFeedback.current.performHapticFeedback(...)`
 * call site automatically uses the SmartHaptics engine.
 */
class SmartComposeHapticFeedback(private val context: Context) : HapticFeedback {

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        val token = hapticFeedbackType.toSmartHapticToken()
        if (token != null) {
            SmartHaptics.perform(context, token)
        }
    }

    private fun HapticFeedbackType.toSmartHapticToken(): SmartHapticToken? = when (this) {
        HapticFeedbackType.LongPress -> SmartHapticToken.LongPress
        HapticFeedbackType.TextHandleMove -> SmartHapticToken.TextHandleMove
        HapticFeedbackType.SegmentTick -> SmartHapticToken.SegmentTick
        HapticFeedbackType.SegmentFrequentTick -> SmartHapticToken.SegmentFrequentTick
        HapticFeedbackType.Confirm -> SmartHapticToken.Confirm
        HapticFeedbackType.Reject -> SmartHapticToken.Reject
        HapticFeedbackType.ContextClick -> SmartHapticToken.ContextClick
        HapticFeedbackType.ToggleOn -> SmartHapticToken.ToggleOn
        HapticFeedbackType.ToggleOff -> SmartHapticToken.ToggleOff
        HapticFeedbackType.GestureEnd -> SmartHapticToken.GestureEnd
        HapticFeedbackType.GestureThresholdActivate -> SmartHapticToken.GestureThresholdActive
        HapticFeedbackType.VirtualKey -> SmartHapticToken.VirtualKey
        // Tokens with no Compose HapticFeedbackType equivalent (ClockTick, DragStart, GestureStart,
        // GestureThresholdDeactive, VirtualKeyRelease) must be played via SmartHaptics.perform(...).
        else -> null
    }
}
