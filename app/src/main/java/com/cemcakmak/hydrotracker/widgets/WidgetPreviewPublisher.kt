package com.cemcakmak.hydrotracker.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass

/**
 * Publishes "generated previews" (live, data-accurate widget picker previews) on
 * Android 15+ (API 35) via [GlanceAppWidgetManager.setWidgetPreviews].
 *
 * Each widget renders its preview through [androidx.glance.appwidget.GlanceAppWidget.providePreview]
 * using real repository data. The API is rate-limited (~2 calls/hour), so we only publish
 * when a provider has no generated preview yet, or when [PREVIEW_REVISION] has moved past
 * the revision this install last published (e.g. after the preview layout changed).
 * Older devices fall back to the static `previewImage` assets declared in the provider XML.
 */
object WidgetPreviewPublisher {

    private const val TAG = "WidgetPreviewPublisher"

    /**
     * Bump whenever the generated preview output changes (layout, sizes, colours). Installs
     * that published an older revision re-publish once; the revision is only persisted on
     * success, so rate-limited attempts are retried on a later app launch.
     */
    private const val PREVIEW_REVISION = 2

    private val receivers: List<KClass<out GlanceAppWidgetReceiver>> = listOf(
        HydroLargeWidget::class,
    )

    suspend fun publishIfNeeded(context: Context, userRepository: UserRepository) {
        if (Build.VERSION.SDK_INT < 35) return
        val publishedRevision = userRepository.appPreferences.first().widgetPreviewRevision
        val manager = GlanceAppWidgetManager(context)
        receivers.forEach { receiver ->
            try {
                val upToDate = publishedRevision >= PREVIEW_REVISION &&
                    hasGeneratedPreview(context, receiver)
                if (upToDate) return@forEach
                val result = manager.setWidgetPreviews(receiver)
                if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
                    userRepository.updateWidgetPreviewRevision(PREVIEW_REVISION)
                }
                Log.d(TAG, "🖼️ Generated preview for ${receiver.simpleName}: result=$result")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to publish preview for ${receiver.simpleName}", e)
            }
        }
    }

    // Callers must hold the API 35 guard (see publishIfNeeded); generatedPreviewCategories
    // does not exist below VANILLA_ICE_CREAM.
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun hasGeneratedPreview(
        context: Context,
        receiver: KClass<out GlanceAppWidgetReceiver>,
    ): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val info = appWidgetManager.installedProviders.firstOrNull {
            it.provider == ComponentName(context, receiver.java)
        } ?: return false
        return info.generatedPreviewCategories != 0
    }
}
