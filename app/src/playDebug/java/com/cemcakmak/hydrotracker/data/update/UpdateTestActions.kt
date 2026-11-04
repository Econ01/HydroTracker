package com.cemcakmak.hydrotracker.data.update

import android.content.Context

/** Variant-specific debug actions exposed to the Update Testing screen. */
interface UpdateTestActionHandler {
    val supportsServerTesting: Boolean get() = false
    val supportsInstallResultSimulation: Boolean get() = false
    val supportsChannelSelection: Boolean get() = false
    val scenarios: List<String> get() = emptyList()

    fun baseUrl(context: Context): String = ""
    fun scenario(context: Context): String = ""
    fun saveServerConfiguration(context: Context, baseUrl: String, scenario: String) = Unit
    fun channel(context: Context): InstallSource = InstallSource.PLAY_STORE
    fun saveChannel(context: Context, channel: InstallSource) = Unit
    fun simulateInstallationResult(success: Boolean, message: String? = null) = Unit
}

/** Unified debug actions for channel, server and installer-result testing. */
object UpdateTestActions : UpdateTestActionHandler {
    override val supportsServerTesting = true
    override val supportsInstallResultSimulation = true
    override val supportsChannelSelection = true
    override val scenarios = GitHubUpdateTestConfig.scenarios

    override fun baseUrl(context: Context) = GitHubUpdateTestConfig.baseUrl(context)

    override fun scenario(context: Context) = GitHubUpdateTestConfig.scenario(context)

    override fun saveServerConfiguration(context: Context, baseUrl: String, scenario: String) {
        GitHubUpdateTestConfig.save(context, baseUrl, scenario)
    }

    override fun channel(context: Context): InstallSource = DebugChannelConfig.channel(context)

    override fun saveChannel(context: Context, channel: InstallSource) {
        DebugChannelConfig.save(context, channel)
    }

    override fun simulateInstallationResult(success: Boolean, message: String?) {
        GitHubApkInstaller.cancelActiveInstallation(if (success) null else message ?: "Simulated failure")
    }
}
