package com.cemcakmak.hydrotracker.utils

import android.content.Context
import androidx.annotation.DrawableRes
import com.cemcakmak.hydrotracker.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Loads the project's contributors list from `assets/contributors.json`.
 *
 * Each entry references translatable string resources via `roleKey` and `bioKey`,
 * and a local avatar drawable via `avatar`. Resource names are mapped to compile-time
 * `R` references below so the build can verify them and avoid reflection.
 * The [ContributorType] drives per-type UI logic (e.g. avatar sizing).
 * This keeps the contributors list editable without touching Kotlin source code.
 */

enum class ContributorType {
    MAINTAINER,
    CONTRIBUTOR,
    TRANSLATOR
}

@Serializable
internal data class ContributorAssetEntry(
    val name: String,
    @SerialName("roleKey") val roleKey: String? = null,
    @SerialName("bioKey") val bioKey: String? = null,
    val avatar: String,
    val github: String,
    val type: ContributorType = ContributorType.CONTRIBUTOR
)

data class ContributorDisplay(
    val name: String,
    val role: String,
    val bio: String,
    @DrawableRes val avatarResId: Int,
    val githubUrl: String,
    val type: ContributorType
)

internal object ContributorsLoader {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val STRING_RESOURCES: Map<String, Int> = mapOf(
        "about_contributor_role" to R.string.about_contributor_role,
        "about_contributor_bio" to R.string.about_contributor_bio,
        "about_contributor_role_contributor" to R.string.about_contributor_role_contributor
    )

    private val AVATAR_RESOURCES: Map<String, Int> = mapOf(
        "econ01" to R.drawable.econ01,
        "lemmesleep247" to R.drawable.lemmesleep247
    )

    fun load(context: Context): List<ContributorDisplay> {
        val entries = context.assets.open(ASSET_FILE_NAME).bufferedReader().use { reader ->
            json.decodeFromString<List<ContributorAssetEntry>>(reader.readText())
        }

        return entries.map { entry ->
            ContributorDisplay(
                name = entry.name,
                role = resolveString(context, entry.roleKey),
                bio = resolveString(context, entry.bioKey),
                avatarResId = resolveDrawable(entry.avatar),
                githubUrl = "https://github.com/${entry.github}",
                type = entry.type
            )
        }
    }

    private fun resolveString(context: Context, key: String?): String {
        if (key == null) return ""
        val resId = STRING_RESOURCES[key]
        return if (resId != null) context.getString(resId) else ""
    }

    @DrawableRes
    private fun resolveDrawable(name: String): Int {
        return AVATAR_RESOURCES[name] ?: fallbackAvatar()
    }

    @DrawableRes
    private fun fallbackAvatar(): Int = R.drawable.econ01

    private const val ASSET_FILE_NAME = "contributors.json"
}
