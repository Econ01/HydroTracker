package com.cemcakmak.hydrotracker.data.update

/** Tracks whether the user can already see the in-app update flow, for notification decisions. */
object UpdateUiVisibility {
    @Volatile
    var isUpdateModalVisible: Boolean = false

    @Volatile
    var isAppInForeground: Boolean = false
}
