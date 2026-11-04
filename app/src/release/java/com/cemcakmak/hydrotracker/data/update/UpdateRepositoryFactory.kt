package com.cemcakmak.hydrotracker.data.update

import android.content.Context

/** Release builds never expose the debug update-state controls. */
object UpdateRepositoryFactory {
    fun create(context: Context): UpdateRepository = UpdateRepository(context.applicationContext)
}
