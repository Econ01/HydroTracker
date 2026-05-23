package com.dev.hydrotracker.widgets

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.dev.hydrotracker.data.repository.UserRepository
import com.dev.hydrotracker.data.database.DatabaseInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Background service to periodically update widgets
 * Runs every 30 minutes to keep widget data fresh
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class WidgetUpdateService : JobService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val JOB_ID = 1001
        private const val UPDATE_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes - more frequent updates
        
        fun scheduleUpdates(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                
                val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, WidgetUpdateService::class.java))
                    .setPersisted(true)
                    .setPeriodic(UPDATE_INTERVAL_MS)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false)
                    .build()
                
                jobScheduler.schedule(jobInfo)
            }
        }
        
        fun cancelUpdates(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                jobScheduler.cancel(JOB_ID)
            }
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        serviceScope.launch {
            try {
                // Update all widgets
                WidgetUpdateHelper.updateAllWidgets(applicationContext)
                
                // Job completed successfully
                jobFinished(params, false)
            } catch (e: Exception) {
                // Job failed, retry later
                jobFinished(params, true)
            }
        }
        
        // Return true because we're doing work asynchronously
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // Return true to retry the job later
        return true
    }
}