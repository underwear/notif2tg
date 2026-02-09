package com.notif2tg

import android.util.Log
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

object TelegramSender {
    private const val TAG = "TelegramSender"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun send(botToken: String, chatId: String, text: String, callback: ((Boolean, String?) -> Unit)? = null) {
        val url = "https://api.telegram.org/bot$botToken/sendMessage"
        val body = mapOf(
            "chat_id" to chatId,
            "text" to text,
            "parse_mode" to "HTML",
            "disable_web_page_preview" to true
        )
        val json = gson.toJson(body)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(JSON_MEDIA))
            .build()

        sendWithRetry(request, 0, callback)
    }

    private fun sendWithRetry(request: Request, attempt: Int, callback: ((Boolean, String?) -> Unit)?) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Send failed (attempt ${attempt + 1}): ${e.message}")
                if (attempt < 2) {
                    val delay = (1L shl attempt) * 1000L // 1s, 2s
                    Thread.sleep(delay)
                    sendWithRetry(request, attempt + 1, callback)
                } else {
                    callback?.invoke(false, e.message)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        Log.d(TAG, "Message sent successfully")
                        callback?.invoke(true, null)
                    } else {
                        val errBody = it.body?.string() ?: "unknown error"
                        Log.e(TAG, "Telegram API error ${it.code}: $errBody")
                        if (attempt < 2 && it.code >= 500) {
                            val delay = (1L shl attempt) * 1000L
                            Thread.sleep(delay)
                            sendWithRetry(request, attempt + 1, callback)
                        } else {
                            callback?.invoke(false, "HTTP ${it.code}: $errBody")
                        }
                    }
                }
            }
        })
    }
}
