package com.notif2tg

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "notif2tg_prefs"
    private const val KEY_BOT_TOKEN = "bot_token"
    private const val KEY_CHAT_ID = "chat_id"
    private const val KEY_SEEN_APPS = "seen_apps"
    private const val KEY_DISABLED_APPS = "disabled_apps"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getBotToken(context: Context): String =
        prefs(context).getString(KEY_BOT_TOKEN, "") ?: ""

    fun getChatId(context: Context): String =
        prefs(context).getString(KEY_CHAT_ID, "") ?: ""

    fun save(context: Context, botToken: String, chatId: String) {
        prefs(context).edit()
            .putString(KEY_BOT_TOKEN, botToken)
            .putString(KEY_CHAT_ID, chatId)
            .apply()
    }

    fun isConfigured(context: Context): Boolean =
        getBotToken(context).isNotBlank() && getChatId(context).isNotBlank()

    fun addSeenApp(context: Context, packageName: String) {
        val seen = getSeenApps(context).toMutableSet()
        if (seen.add(packageName)) {
            prefs(context).edit().putStringSet(KEY_SEEN_APPS, seen).apply()
        }
    }

    fun getSeenApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_SEEN_APPS, emptySet()) ?: emptySet()

    fun isAppEnabled(context: Context, packageName: String): Boolean =
        !getDisabledApps(context).contains(packageName)

    fun setAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        val disabled = getDisabledApps(context).toMutableSet()
        if (enabled) disabled.remove(packageName) else disabled.add(packageName)
        prefs(context).edit().putStringSet(KEY_DISABLED_APPS, disabled).apply()
    }

    fun getDisabledApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_DISABLED_APPS, emptySet()) ?: emptySet()
}
