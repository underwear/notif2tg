package com.notif2tg

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationService : NotificationListenerService() {
    companion object {
        private const val TAG = "NotificationService"
        private const val MAX_CACHE_SIZE = 200
    }

    private val recentKeys = LinkedHashSet<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName) return

        val context = applicationContext
        if (!Prefs.isConfigured(context)) return

        Prefs.addSeenApp(context, sbn.packageName)
        if (!Prefs.isAppEnabled(context, sbn.packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val dedupeKey = "${sbn.key}|$title|$text"
        synchronized(recentKeys) {
            if (!recentKeys.add(dedupeKey)) return
            if (recentKeys.size > MAX_CACHE_SIZE) {
                val it = recentKeys.iterator()
                it.next()
                it.remove()
            }
        }

        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        val message = buildString {
            append("<b>$appName</b>")
            if (title.isNotBlank()) append("\n$title")
            if (text.isNotBlank()) append("\n$text")
            if (subText.isNotBlank()) append("\n<i>$subText</i>")
        }

        Log.d(TAG, "Forwarding notification from $appName")

        TelegramSender.send(
            Prefs.getBotToken(context),
            Prefs.getChatId(context),
            message
        )
    }
}
