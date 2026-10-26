package com.cemcakmak.hydrotracker.widgets

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.cemcakmak.hydrotracker.widgets.ui.QuickAddWidgetContent

/** Receiver for the standalone, adaptive quick-actions widget. */
class HydroQuickAddWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HydroQuickAddGlanceWidget()
}

/** One to three shared quick-add actions, defaulting to a 2x2 placement. */
class HydroQuickAddGlanceWidget : GlanceAppWidget() {

    companion object {
        private const val TAG = "HydroQuickAddWidget"
        private val SIZES = setOf(
            DpSize(110.dp, 110.dp),
            DpSize(180.dp, 180.dp),
        )
    }

    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialState = initialiseHydroWidgetState(context, id)
        provideContent {
            val preferences = currentState<Preferences>()
            val state = if (initialState != null && !preferences.hasHydroWidgetState()) {
                initialState
            } else {
                preferences.toHydroWidgetState()
            }
            HydroWidgetTheme(forceHydroColors = !state.useDynamicColors) {
                QuickAddWidgetContent(state)
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val state = HydroWidgetStateLoader.load(context)
        provideContent {
            HydroWidgetTheme(forceHydroColors = !state.useDynamicColors) {
                QuickAddWidgetContent(state)
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        Log.e(TAG, "❌ Quick-actions widget composition failed", throwable)
        super.onCompositionError(context, glanceId, appWidgetId, throwable)
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 57, heightDp = 102)
@Composable
private fun SingleQuickAddWidgetPreview() {
    HydroWidgetTheme {
        QuickAddWidgetContent(hydrationPreviewState())
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 130, heightDp = 102)
@Composable
private fun TwoQuickAddWidgetPreview() {
    HydroWidgetTheme {
        QuickAddWidgetContent(hydrationPreviewState())
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 180, heightDp = 180)
@Composable
private fun SquareQuickAddWidgetPreview() {
    HydroWidgetTheme {
        QuickAddWidgetContent(hydrationPreviewState())
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 330, heightDp = 180)
@Composable
private fun WideQuickAddWidgetPreview() {
    HydroWidgetTheme {
        QuickAddWidgetContent(hydrationPreviewState())
    }
}
