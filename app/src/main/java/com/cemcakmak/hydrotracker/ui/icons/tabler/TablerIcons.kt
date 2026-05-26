package com.cemcakmak.hydrotracker.ui.icons.tabler

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cemcakmak.hydrotracker.R

/**
 * Auto-generated wrapper for Tabler Icons.
 *
 * Naming convention:
 * - Outline icons:  iconname.xml      → TablerIcons.Iconname
 * - Filled icons:   iconname_filled.xml → TablerIcons.IconnameFilled
 *
 * To add new icons manually:
 * 1. Use Android Studio: right-click res/drawable → New → Vector Asset
 * 2. Name outline SVGs as:  palette, bell, heart, ...
 * 3. Name filled SVGs as:  palette_filled, bell_filled, heart_filled, ...
 * 4. Add the matching property below.
 */
object TablerIcons {
    val BellFilled: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.tabler_bell_filled)

    val CalendarEventFilled: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.tabler_calendar_event_filled)

    val CodeCircleFilled: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.tabler_code_circle_filled)

    val EditFilled: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.tabler_edit_filled)

    val HeartFilled: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.tabler_heart_filled)

    val InfoCircleFilled: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.tabler_info_circle_filled)

    val PaletteFilled: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.tabler_palette_filled)
}
