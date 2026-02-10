package com.notif2tg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d("BootReceiver", "Received ${intent.action}, starting services")
            if (Prefs.isConfigured(context)) {
                KeepAliveJob.startJob(context)
                context.startForegroundService(
                    Intent(context, KeepAliveService::class.java)
                )
            }
        }
    }
}
