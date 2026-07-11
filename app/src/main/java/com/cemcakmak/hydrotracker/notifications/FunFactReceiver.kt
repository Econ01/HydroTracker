/*
 *
 *  * HydroTracker - A modern and private water intake tracking application
 *  * Copyright (c) 2026 Ali Cem Çakmak
 *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.cemcakmak.hydrotracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles the daily fun-fact alarm.
 *
 * Shows the fun-fact notification and schedules the next one for the following day.
 */
class FunFactReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW_FUN_FACT = "com.cemcakmak.hydrotracker.SHOW_FUN_FACT"
        private const val TAG = "FunFactReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW_FUN_FACT) return

        Log.d(TAG, "Fun-fact alarm received")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userRepository = UserRepository(context)
                val userProfile = userRepository.userProfile.first()

                if (userProfile == null || !userProfile.isOnboardingCompleted) {
                    Log.d(TAG, "No eligible user profile, skipping fun fact")
                    return@launch
                }

                if (!userProfile.funFactsEnabled) {
                    Log.d(TAG, "Fun facts disabled, skipping")
                    return@launch
                }

                if (!NotificationPermissionManager.hasNotificationPermission(context)) {
                    Log.w(TAG, "Notification permission not granted")
                    return@launch
                }

                HydroNotificationService(context).showFunFact()
                HydroNotificationScheduler.scheduleFunFact(context, userProfile)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling fun-fact alarm", e)
            }
        }
    }
}
