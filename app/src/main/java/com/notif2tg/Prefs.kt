package com.notif2tg

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "notif2tg_prefs"
    private const val KEY_BOT_TOKEN = "bot_token"
    private const val KEY_CHAT_ID = "chat_id"
    private const val KEY_BOT_USERNAME = "bot_username"
    private const val KEY_CHAT_TITLE = "chat_title"
    private const val KEY_CHAT_TYPE = "chat_type"
    private const val KEY_SETUP_COMPLETED = "setup_completed"
    private const val KEY_SEEN_APPS = "seen_apps"
    private const val KEY_DISABLED_APPS = "disabled_apps"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getBotToken(context: Context): String =
        prefs(context).getString(KEY_BOT_TOKEN, "") ?: ""

    fun getChatId(context: Context): String =
        prefs(context).getString(KEY_CHAT_ID, "") ?: ""

    fun getBotUsername(context: Context): String =
        prefs(context).getString(KEY_BOT_USERNAME, "") ?: ""

    fun getChatTitle(context: Context): String =
        prefs(context).getString(KEY_CHAT_TITLE, "") ?: ""

    fun getChatType(context: Context): String =
        prefs(context).getString(KEY_CHAT_TYPE, "") ?: ""

    fun isSetupCompleted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SETUP_COMPLETED, false)

    fun saveBotInfo(context: Context, botToken: String, botUsername: String) {
        prefs(context).edit()
            .putString(KEY_BOT_TOKEN, botToken)
            .putString(KEY_BOT_USERNAME, botUsername)
            .apply()
    }

    fun saveChatInfo(context: Context, chatId: String, chatTitle: String, chatType: String) {
        prefs(context).edit()
            .putString(KEY_CHAT_ID, chatId)
            .putString(KEY_CHAT_TITLE, chatTitle)
            .putString(KEY_CHAT_TYPE, chatType)
            .apply()
    }

    fun setSetupCompleted(context: Context, completed: Boolean) {
        prefs(context).edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
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
