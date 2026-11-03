package com.cemcakmak.hydrotracker.data.update

import android.content.Context
import androidx.core.content.edit

/** Runtime channel selection for the unified debug application. */
object DebugChannelConfig {
    private const val PREFS_NAME = "debug_update_channel"
    private const val KEY_CHANNEL = "channel"

    fun channel(context: Context): InstallSource {
        val stored = prefs(context).getString(KEY_CHANNEL, InstallSource.PLAY_STORE.name)
        return runCatching { InstallSource.valueOf(stored ?: InstallSource.PLAY_STORE.name) }
            .getOrDefault(InstallSource.PLAY_STORE)
    }

    fun save(context: Context, channel: InstallSource) {
        prefs(context).edit { putString(KEY_CHANNEL, channel.name) }
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
}

/** Persists the debug-only local server configuration outside the release build. */
object GitHubUpdateTestConfig {
    private const val PREFS_NAME = "github_update_test_config"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_SCENARIO = "scenario"

    const val DEFAULT_BASE_URL = "http://10.0.2.2:8765"

    val scenarios = listOf(
        "success",
        "up_to_date",
        "http_error",
        "invalid_json",
        "missing_asset",
        "bad_digest",
        "wrong_package",
        "old_version",
        "bad_signature",
        "slow_download"
    )

    fun baseUrl(context: Context): String = prefs(context).getString(KEY_BASE_URL, DEFAULT_BASE_URL)
        ?: DEFAULT_BASE_URL

    fun scenario(context: Context): String = prefs(context).getString(KEY_SCENARIO, scenarios.first())
        ?: scenarios.first()

    fun save(context: Context, baseUrl: String, scenario: String) {
        prefs(context).edit {
            putString(KEY_BASE_URL, baseUrl.trim().trimEnd('/'))
            putString(KEY_SCENARIO, scenario)
        }
    }

    fun feedUrl(context: Context): String {
        return "${baseUrl(context).trimEnd('/')}/release.json?scenario=${scenario(context)}"
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
}
