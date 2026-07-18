package com.cemcakmak.hydrotracker.utils

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/**
 * Per-app language, implemented WITHOUT AppCompat so the app keeps using a plain ComponentActivity.
 * (AppCompatActivity breaks Navigation3's NavigationEventDispatcher wiring used by NavDisplay.)
 *
 * SharedPreferences is the single source of truth and is applied in MainActivity.attachBaseContext
 * via [wrap] on every API level. On Android 13+ the choice is also mirrored to the framework
 * LocaleManager so it shows in the system "App languages" settings.
 */
object AppLocale {

    /**
     * BCP-47 language tags the app ships translations for. Keep in sync with
     * res/xml/locales_config.xml. A null/blank tag means "follow the system language".
     * Add a tag here when a values-XX/ translation lands.
     */
    val SUPPORTED_TAGS: List<String> = listOf("en")

    private const val PREFS = "app_locale_prefs"
    private const val KEY_TAG = "language_tag"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun isSupported(tag: String): Boolean =
        tag.isNotBlank() && (SUPPORTED_TAGS.contains(tag) || tag == "en")

    /** The current app-language tag, or null when following the system language. */
    fun currentTag(context: Context): String? =
        prefs(context).getString(KEY_TAG, null)
            ?.takeIf { it.isNotBlank() && isSupported(it) }

    /**
     * Persist [tag] (e.g. "de"), or null/blank to follow the system language. The caller should
     * recreate the activity afterwards so [wrap] re-applies it; this method does not recreate.
     * Unsupported tags are treated as "follow system" to avoid applying a locale the app does not
     * actually ship resources for.
     */
    fun apply(context: Context, tag: String?) {
        val normalized = tag?.takeIf { it.isNotBlank() && isSupported(it) }
        prefs(context).edit { putString(KEY_TAG, normalized) }

        // Mirror to the framework on Android 13+ so the system "App languages" screen stays in sync.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (normalized == null) LocaleList.getEmptyLocaleList()
                else LocaleList.forLanguageTags(normalized)
        }
    }

    /**
     * Wrap [base] with the stored app locale. Call from Activity.attachBaseContext.
     * If the stored tag is no longer supported, the base context is returned unchanged.
     */
    fun wrap(base: Context): Context {
        val tag = prefs(base).getString(KEY_TAG, null)
            ?.takeIf { it.isNotBlank() && isSupported(it) }
            ?: return base

        val locale = Locale.forLanguageTag(tag)
        // Synchronize the JVM default so that date/number formatting across the app matches
        // the chosen per-app locale. This is safe because the app intentionally runs in one
        // active locale at a time.
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /** Autonym-style display name for a tag (e.g. "Deutsch" for "de"), capitalized. */
    fun displayName(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
    }
}
