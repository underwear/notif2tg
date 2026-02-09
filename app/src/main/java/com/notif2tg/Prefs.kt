package com.notif2tg

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "notif2tg_prefs"
    private const val KEY_BOT_TOKEN = "bot_token"
    private const val KEY_CHAT_ID = "chat_id"

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
}
