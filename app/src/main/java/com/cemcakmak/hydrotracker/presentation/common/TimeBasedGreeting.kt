/*
 *
 *  * HydroTracker - A modern and private water intake tracking application
 *  * Copyright (c) 2026 Ali Cem Çakmak
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.cemcakmak.hydrotracker.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cemcakmak.hydrotracker.R
import java.time.LocalTime

@Composable
fun timeBasedGreeting(): String {
    val currentHour = LocalTime.now().hour
    return when (currentHour) {
        in 5..11 -> stringResource(R.string.profile_greeting_morning)
        in 12..16 -> stringResource(R.string.profile_greeting_afternoon)
        in 17..21 -> stringResource(R.string.profile_greeting_evening)
        else -> stringResource(R.string.profile_greeting_default)
    }
}