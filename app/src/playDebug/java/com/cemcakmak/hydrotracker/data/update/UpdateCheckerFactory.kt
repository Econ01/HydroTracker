package com.cemcakmak.hydrotracker.data.update

import android.content.Context

/** Creates the unified debug checker with runtime channel selection. */
object UpdateCheckerFactory {
    fun create(context: Context): UpdateChecker = DebugChannelUpdateChecker(context.applicationContext)
}
