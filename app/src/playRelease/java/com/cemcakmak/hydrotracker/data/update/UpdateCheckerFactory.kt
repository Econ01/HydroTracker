package com.cemcakmak.hydrotracker.data.update

import android.content.Context

/** Creates the Play-backed checker for the Play release flavour. */
object UpdateCheckerFactory {
    fun create(context: Context): UpdateChecker = PlayUpdateChecker(context.applicationContext)
}
