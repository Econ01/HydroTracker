package com.cemcakmak.hydrotracker.widgets

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.widgets.ui.HydrationWidgetContent

class HydroLargeWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HydroLargeGlanceWidget()
}

/** Adaptive hydration progress and quick-add widget, defaulting to the original 4x2 placement. */
class HydroLargeGlanceWidget : GlanceAppWidget() {

    companion object {
        private const val TAG = "HydroLargeWidget"

        // Generated picker previews represent the default roomy placement, not every resize state.
        private val SIZES = setOf(
            DpSize(250.dp, 180.dp),
            DpSize(330.dp, 180.dp),
            DpSize(330.dp, 260.dp),
        )
    }

    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // A newly pinned instance needs its initial snapshot; existing instances remain entirely
        // updater-driven so live sessions always recompose from the latest preferences write.
        val initialState = initialiseHydroWidgetState(context, id)
        provideContent {
            val preferences = currentState<Preferences>()
            val state = if (initialState != null && !preferences.hasHydroWidgetState()) {
                initialState
            } else {
                preferences.toHydroWidgetState()
            }
            HydroWidgetTheme(forceHydroColors = !state.useDynamicColors) {
                HydrationWidgetContent(state)
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val state = HydroWidgetStateLoader.load(context)
        provideContent {
            HydroWidgetTheme(forceHydroColors = !state.useDynamicColors) {
                HydrationWidgetContent(state)
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        Log.e(TAG, "❌ Hydration widget composition failed", throwable)
        super.onCompositionError(context, glanceId, appWidgetId, throwable)
    }
}

internal fun hydrationPreviewState(
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
        HydrationWidgetContent(hydrationPreviewState())
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 330, heightDp = 190)
@Composable
private fun LargeWidgetGoalPreview() {
    HydroWidgetTheme {
        HydrationWidgetContent(hydrationPreviewState(currentIntake = 3000.0))
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 330, heightDp = 180)
@Composable
private fun LargeWidgetHydroThemePreview() {
    GlanceTheme(colors = HydroWidgetColors.hydroColors) {
        HydrationWidgetContent(hydrationPreviewState())
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 57, heightDp = 102)
@Composable
private fun RingOnlyWidgetPreview() {
    HydroWidgetTheme {
        HydrationWidgetContent(hydrationPreviewState())
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 130, heightDp = 102)
@Composable
private fun LinearOnlyWidgetPreview() {
    HydroWidgetTheme {
        HydrationWidgetContent(hydrationPreviewState())
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 276, heightDp = 102)
@Composable
private fun LinearActionWidgetPreview() {
    HydroWidgetTheme {
        HydrationWidgetContent(hydrationPreviewState())
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 130, heightDp = 102)
@Composable
private fun LinearOnlyGoalWidgetPreview() {
    HydroWidgetTheme {
        HydrationWidgetContent(hydrationPreviewState(currentIntake = 3000.0))
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 412, heightDp = 51)
@Composable
private fun VeryShortLinearActionWidgetPreview() {
    HydroWidgetTheme {
        HydrationWidgetContent(hydrationPreviewState())
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 110, heightDp = 40)
@Composable
private fun VeryShortLinearOnlyWidgetPreview() {
    HydroWidgetTheme {
        HydrationWidgetContent(hydrationPreviewState())
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 180, heightDp = 180)
@Composable
private fun CompactSquareWidgetPreview() {
    HydroWidgetTheme {
        HydrationWidgetContent(hydrationPreviewState())
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 330, heightDp = 330)
@Composable
private fun LargeSquareWidgetPreview() {
    HydroWidgetTheme {
        HydrationWidgetContent(hydrationPreviewState())
    }
}
