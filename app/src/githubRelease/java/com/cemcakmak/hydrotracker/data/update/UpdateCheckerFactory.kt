package com.cemcakmak.hydrotracker.data.update

import android.content.Context

/** Creates the production GitHub checker with HTTPS-only update endpoints. */
object UpdateCheckerFactory {
    fun create(context: Context): UpdateChecker = GitHubUpdateChecker(context.applicationContext)
}
