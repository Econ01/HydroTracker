package com.cemcakmak.hydrotracker.presentation.settings

import androidx.annotation.StringRes
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.update.UpdateError

/** Maps structured update failures to localized, user-safe messages at the UI boundary. */
@StringRes
internal fun UpdateError.messageRes(): Int = when (this) {
    UpdateError.UNKNOWN -> R.string.update_error_unknown
    UpdateError.CHECK_FAILED -> R.string.update_error_check_failed
    UpdateError.START_FAILED -> R.string.update_error_start_failed
    UpdateError.INSTALL_FAILED -> R.string.update_error_install_failed
    UpdateError.MANAGED_EXTERNALLY -> R.string.updates_status_f_droid_desc
    UpdateError.NO_DOWNLOADED_UPDATE -> R.string.update_error_no_downloaded_update
    UpdateError.PLAY_RECHECK_REQUIRED -> R.string.update_error_play_recheck_required
    UpdateError.PLAY_UPDATE_UNAVAILABLE -> R.string.update_error_play_update_unavailable
    UpdateError.UPDATE_NOT_AVAILABLE -> R.string.update_error_update_not_available
    UpdateError.INVALID_UPDATE_METADATA -> R.string.update_error_invalid_update_metadata
    UpdateError.DOWNLOAD_FAILED -> R.string.update_error_download_failed
    UpdateError.VERIFICATION_FAILED -> R.string.update_error_verification_failed
    UpdateError.INSTALL_CONFIRMATION_UNAVAILABLE -> R.string.update_error_install_confirmation_unavailable
    UpdateError.INSECURE_URL -> R.string.update_error_insecure_url
    UpdateError.LOCAL_NETWORK_PERMISSION_REQUIRED -> R.string.update_error_local_network_permission
}
