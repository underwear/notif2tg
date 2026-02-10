package com.notif2tg

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobService
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class KeepAliveJob : JobService() {
    companion object {
        private const val TAG = "KeepAliveJob"
        private const val JOB_ID = 10

        fun startJob(context: Context) {
            val jobScheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobInfo = JobInfo.Builder(
                JOB_ID,
                ComponentName(context.packageName, KeepAliveJob::class.java.name)
            )
                .setPersisted(true)
                .setMinimumLatency(5_000L)
                .setOverrideDeadline(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS)
                .build()
            jobScheduler.schedule(jobInfo)
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        if (Prefs.isConfigured(applicationContext)) {
            Log.d(TAG, "Pulling up KeepAliveService")
            applicationContext.startForegroundService(
                Intent(applicationContext, KeepAliveService::class.java)
            )
        }
        jobFinished(params, false)
        startJob(applicationContext)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}
