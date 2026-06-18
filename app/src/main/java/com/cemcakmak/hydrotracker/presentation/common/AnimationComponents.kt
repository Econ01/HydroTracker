package com.cemcakmak.hydrotracker.presentation.common

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import com.cemcakmak.hydrotracker.utils.SmartHaptics
import androidx.compose.ui.unit.dp

@Composable
fun <T> BlurMorph(
    modifier: Modifier = Modifier,
    targetState: T,
    content: @Composable (state: T, blurModifier: Modifier) -> Unit
) {
    val fadeSpec = tween<Float>(durationMillis = 600, delayMillis = 100, easing = EaseInOut)
    val blurSpec = tween<Dp>(durationMillis = 800, easing = EaseInOut)
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

@Composable
fun rememberAnimatedDouble(
    targetValue: Double,
    hapticsEnabled: Boolean = true,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = 800, easing = EaseInOut)
): Float {
    val view = LocalView.current
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatable.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = animationSpec
        )
    }

    if (hapticsEnabled) {
        LaunchedEffect(Unit) {
            var lastTick = -1
            snapshotFlow { animatable.value }.collect { animatedValue ->
                val step = 0.2
                val currentTick = (animatedValue / step).toInt()
                if (currentTick != lastTick && animatedValue > 0f) {
                    lastTick = currentTick
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        SmartHaptics.performRaw(view.context, HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                    } else {
                        SmartHaptics.performRaw(view.context, HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
            }
        }
    }

    return animatable.value
}
