package com.cemcakmak.hydrotracker.data.update

import android.content.Context

/** F-Droid manages updates externally; this flavour contains no self-update implementation. */
object UpdateCheckerFactory {
    fun create(@Suppress("UNUSED_PARAMETER") context: Context): UpdateChecker = FdroidUpdateChecker
}
