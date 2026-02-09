package com.notif2tg

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

data class BotInfo(val id: Long, val username: String, val firstName: String)

data class ChatInfo(
    val chatId: String,
    val title: String,
    val type: String // private, group, supergroup, channel
)

object TelegramSender {
    private const val TAG = "TelegramSender"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    val gson: Gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val longPollClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
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

    fun getMe(botToken: String, callback: (BotInfo?, String?) -> Unit) {
        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/getMe")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(null, "HTTP ${it.code}")
                        return
                    }
                    try {
                        val json = gson.fromJson(it.body?.string(), JsonObject::class.java)
                        val result = json.getAsJsonObject("result")
                        val info = BotInfo(
                            id = result.get("id").asLong,
                            username = result.get("username")?.asString ?: "",
                            firstName = result.get("first_name")?.asString ?: ""
                        )
                        callback(info, null)
                    } catch (e: Exception) {
                        callback(null, e.message)
                    }
                }
            }
        })
    }

    fun getUpdates(
        botToken: String,
        offset: Long,
        timeout: Int = 30,
        callback: (List<Pair<Long, ChatInfo>>?, String?) -> Unit
    ) {
        val url = "https://api.telegram.org/bot$botToken/getUpdates?offset=$offset&timeout=$timeout&allowed_updates=[\"message\",\"channel_post\"]"
        val request = Request.Builder().url(url).get().build()

        longPollClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!call.isCanceled()) callback(null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(null, "HTTP ${it.code}")
                        return
                    }
                    try {
                        val json = gson.fromJson(it.body?.string(), JsonObject::class.java)
                        val results = json.getAsJsonArray("result") ?: run {
                            callback(emptyList(), null)
                            return
                        }
                        val chats = results.mapNotNull { el ->
                            val update = el.asJsonObject
                            val updateId = update.get("update_id").asLong
                            val msg = update.getAsJsonObject("message")
                                ?: update.getAsJsonObject("channel_post")
                                ?: return@mapNotNull null
                            val chat = msg.getAsJsonObject("chat") ?: return@mapNotNull null
                            val type = chat.get("type")?.asString ?: "private"
                            val title = when {
                                chat.has("title") -> chat.get("title").asString
                                else -> {
                                    val first = chat.get("first_name")?.asString ?: ""
                                    val last = chat.get("last_name")?.asString ?: ""
                                    "$first $last".trim()
                                }
                            }
                            updateId to ChatInfo(
                                chatId = chat.get("id").asLong.toString(),
                                title = title,
                                type = type
                            )
                        }
                        callback(chats, null)
                    } catch (e: Exception) {
                        callback(null, e.message)
                    }
                }
            }
        })
    }

    fun cancelAll() {
        longPollClient.dispatcher.cancelAll()
    }

    private fun sendWithRetry(request: Request, attempt: Int, callback: ((Boolean, String?) -> Unit)?) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Send failed (attempt ${attempt + 1}): ${e.message}")
                if (attempt < 2) {
                    val delay = (1L shl attempt) * 1000L
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
