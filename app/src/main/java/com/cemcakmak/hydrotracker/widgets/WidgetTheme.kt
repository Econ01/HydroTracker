package com.cemcakmak.hydrotracker.widgets

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.widget.RemoteViews

/**
 * Material 3 theming manager for widgets
 * Provides consistent color application across all widget types
 * with support for dynamic colors (Android 12+) and custom palettes (Android 11-)
 */
object WidgetTheme {

    /**
     * Apply Material 3 theme to widget RemoteViews
     */
    fun applyTheme(
        context: Context,
        views: RemoteViews,
        containerId: Int? = null,
        progressBarId: Int? = null,
        textViewIds: List<Int> = emptyList(),
        accentTextViewIds: List<Int> = emptyList(),
        variantTextViewIds: List<Int> = emptyList(),
     buttonTextViewIds: List<Int> = emptyList(),
        titleTextViewIds: List<Int> = emptyList()
    ) {
        val isDarkMode = isDarkMode(context)

        // On Android 12+ (API 31), dynamic colors are handled entirely by the layout-v31 XML files 
        // using ?attr/ system variables. We must NOT override them programmatically.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            applyCustomColors(context, views, containerId, progressBarId, textViewIds, accentTextViewIds, variantTextViewIds, buttonTextViewIds, titleTextViewIds, isDarkMode)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
    private fun applyDynamicColors(
        context: Context,
        views: RemoteViews,
        containerId: Int?,
        progressBarId: Int?,
        textViewIds: List<Int>,
        accentTextViewIds: List<Int>,
        variantTextViewIds: List<Int>,
        buttonTextViewIds: List<Int>,
        titleTextViewIds: List<Int>,
        isDarkMode: Boolean
    ) {
        try {
            // Background: Use the official adaptive widget background color (API 31+)
            val resId = context.resources.getIdentifier("system_app_widget_background_color", "color", "android")
            val surfaceColor = if (resId != 0) {
                context.getColor(resId)
            } else {
                if (isDarkMode) context.getColor(android.R.color.system_neutral1_800) 
                else context.getColor(android.R.color.system_neutral1_50)
            }

            // Text: Tone 95 for maximum brightness in Dark Mode
            val onSurfaceColor = if (isDarkMode) context.getColor(android.R.color.system_neutral1_50) else context.getColor(android.R.color.system_neutral1_900)
            
            // Progress: Primary Accent (Tone 80/40)
            val primaryColor = if (isDarkMode) context.getColor(android.R.color.system_accent1_200) else context.getColor(android.R.color.system_accent1_600)
            
            // Track: Surface Variant / Secondary Container (Tone 30/90)
            val trackColor = if (isDarkMode) context.getColor(android.R.color.system_neutral2_700) else context.getColor(android.R.color.system_neutral2_100)

            // Secondary Text: Tone 80 (system_neutral2_200)
            val onSurfaceVariantColor = if (isDarkMode) context.getColor(android.R.color.system_neutral2_200) else context.getColor(android.R.color.system_neutral2_700)
            
            // Buttons: Secondary Accent Container
            val onSecondaryContainerColor = if (isDarkMode) context.getColor(android.R.color.system_accent2_100) else context.getColor(android.R.color.system_accent2_800)

            // Apply background tint
            containerId?.let { views.setColorStateList(it, "setBackgroundTintList", ColorStateList.valueOf(surfaceColor)) }
            
            // Apply to progress bar (API 31+)
            progressBarId?.let { 
                views.setColorStateList(it, "setProgressTintList", ColorStateList.valueOf(primaryColor))
                views.setColorStateList(it, "setProgressBackgroundTintList", ColorStateList.valueOf(trackColor))
            }

            // Apply to text
            textViewIds.forEach { views.setTextColor(it, onSurfaceColor) }
            titleTextViewIds.forEach { views.setTextColor(it, onSurfaceColor) }
            accentTextViewIds.forEach { views.setTextColor(it, primaryColor) }
            variantTextViewIds.forEach { views.setTextColor(it, onSurfaceVariantColor) }
            buttonTextViewIds.forEach { views.setTextColor(it, onSecondaryContainerColor) }

        } catch (_: Exception) {
            applyCustomColors(context, views, containerId, progressBarId, textViewIds, accentTextViewIds, variantTextViewIds, buttonTextViewIds, titleTextViewIds, isDarkMode)
        }
    }

    private fun applyCustomColors(
        context: Context,
        views: RemoteViews,
        containerId: Int?,
        progressBarId: Int?,
        textViewIds: List<Int>,
        accentTextViewIds: List<Int>,
        variantTextViewIds: List<Int>,
        buttonTextViewIds: List<Int>,
        titleTextViewIds: List<Int>,
        isDarkMode: Boolean
    ) {
        // Fallback colors
        val surfaceColor = if (isDarkMode) 0xFF2B2930.toInt() else 0xFFF3F3F7.toInt()
        val onSurfaceColor = if (isDarkMode) 0xFFFDFBFF.toInt() else 0xFF1A1C1E.toInt()
        val primaryColor = if (isDarkMode) 0xFF9ACBFF.toInt() else 0xFF0077BE.toInt()
        val trackColor = if (isDarkMode) 0xFF44474E.toInt() else 0xFFE1E2EC.toInt()
        val onSurfaceVariantColor = if (isDarkMode) 0xFFC4C6D0.toInt() else 0xFF44474E.toInt()
        val onSecondaryContainerColor = if (isDarkMode) 0xFFD3E4FF.toInt() else 0xFF003258.toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            containerId?.let { views.setColorStateList(it, "setBackgroundTintList", ColorStateList.valueOf(surfaceColor)) }
            progressBarId?.let { 
                views.setColorStateList(it, "setProgressTintList", ColorStateList.valueOf(primaryColor))
                views.setColorStateList(it, "setProgressBackgroundTintList", ColorStateList.valueOf(trackColor))
            }
        } else {
            containerId?.let { views.setInt(it, "setBackgroundColor", surfaceColor) }
        }
        
        textViewIds.forEach { views.setTextColor(it, onSurfaceColor) }
        titleTextViewIds.forEach { views.setTextColor(it, onSurfaceColor) }
        accentTextViewIds.forEach { views.setTextColor(it, primaryColor) }
        variantTextViewIds.forEach { views.setTextColor(it, onSurfaceVariantColor) }
        buttonTextViewIds.forEach { views.setTextColor(it, onSecondaryContainerColor) }
    }

    private fun isDarkMode(context: Context): Boolean {
        return context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
}
