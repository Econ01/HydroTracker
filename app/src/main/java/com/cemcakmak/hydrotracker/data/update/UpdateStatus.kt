package com.cemcakmak.hydrotracker.data.update

/** The update mechanism available in the current distribution channel. */
enum class UpdateMechanism {
    PLAY_IN_APP,
    GITHUB_APK
}

/** Localizable failures shared by every distribution channel. */
enum class UpdateError {
    UNKNOWN,
    CHECK_FAILED,
    START_FAILED,
    INSTALL_FAILED,
    MANAGED_EXTERNALLY,
    NO_DOWNLOADED_UPDATE,
    PLAY_RECHECK_REQUIRED,
    PLAY_UPDATE_UNAVAILABLE,
    UPDATE_NOT_AVAILABLE,
    INVALID_UPDATE_METADATA,
    DOWNLOAD_FAILED,
    VERIFICATION_FAILED,
    INSTALL_CONFIRMATION_UNAVAILABLE,
    INSECURE_URL,
    LOCAL_NETWORK_PERMISSION_REQUIRED
}

/** The result of an update check or the state of an active update session. */
sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data object Unsupported : UpdateStatus

    /** [technicalDetails] is diagnostic only; production UI renders [error] through string resources. */
    data class Failed(
        val error: UpdateError,
        val technicalDetails: String? = null
    ) : UpdateStatus

    /** A GitHub APK is being downloaded after an explicit user action. */
    data class Downloading(val progress: Int? = null) : UpdateStatus

    /** The downloaded APK is being checked before it can be installed. */
    data object Verifying : UpdateStatus

    /** The APK is ready; the user must explicitly start Android's installer. */
    data class ReadyToInstall(val available: Available) : UpdateStatus

    /** Android's unknown-apps authorisation is required before downloading can continue. */
    data class PermissionRequired(val available: Available) : UpdateStatus

    /** Android's package installer has been started or is awaiting confirmation. */
    data object Installing : UpdateStatus

    /** A newer version is available. */
    data class Available(
        val versionName: String,
        val mechanism: UpdateMechanism,
        val downloadUrl: String? = null,
        val downloadSize: Long? = null,
        val downloadSha256: String? = null
    ) : UpdateStatus
}

/** Carries a localizable error through non-UI layers while retaining diagnostic details. */
internal class UpdateException(
    val error: UpdateError,
    message: String,
    cause: Throwable? = null
) : java.io.IOException(message, cause)

/** Maps an unexpected exception to a safe UI error without losing its original diagnostic text. */
internal fun Throwable.toUpdateFailure(fallback: UpdateError): UpdateStatus.Failed {
    return UpdateStatus.Failed(
        error = (this as? UpdateException)?.error ?: fallback,
        technicalDetails = message
    )
}
