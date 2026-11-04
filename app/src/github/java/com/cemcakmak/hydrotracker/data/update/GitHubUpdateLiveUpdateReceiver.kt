package com.cemcakmak.hydrotracker.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Records a user's decision to dismiss a Live Update for the current download only. */
class GitHubUpdateLiveUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != GitHubUpdateDownloadService.ACTION_DISMISS_LIVE_UPDATE) return
        val downloadId = intent.getStringExtra(GitHubUpdateDownloadService.EXTRA_DOWNLOAD_ID)
        GitHubUpdateDownloadService.dismissLiveUpdate(downloadId)
    }
}
