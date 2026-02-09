package com.notif2tg

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        val tvBotInfo = findViewById<TextView>(R.id.tv_bot_info)
        val tvBotToken = findViewById<TextView>(R.id.tv_bot_token_masked)
        val tvChatInfo = findViewById<TextView>(R.id.tv_chat_info)
        val tvChatDetail = findViewById<TextView>(R.id.tv_chat_detail)

        val username = Prefs.getBotUsername(this)
        tvBotInfo.text = if (username.isNotBlank()) "@$username" else "Not configured"

        val token = Prefs.getBotToken(this)
        tvBotToken.text = if (token.length > 10) "${token.take(6)}...${token.takeLast(4)}" else "***"

        val chatTitle = Prefs.getChatTitle(this)
        val chatType = Prefs.getChatType(this)
        val chatId = Prefs.getChatId(this)
        tvChatInfo.text = chatTitle.ifBlank { chatId }
        val typeLabel = when (chatType) {
            "private" -> "Private chat"
            "group" -> "Group"
            "supergroup" -> "Supergroup"
            "channel" -> "Channel"
            else -> ""
        }
        tvChatDetail.text = if (typeLabel.isNotBlank()) "$typeLabel  |  ID: $chatId" else "ID: $chatId"

        findViewById<Button>(R.id.btn_test_message).setOnClickListener {
            TelegramSender.send(token, chatId, "Notif2TG test message — forwarding is working!") { ok, err ->
                runOnUiThread {
                    if (ok) {
                        Toast.makeText(this, "Test message sent!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed: $err", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        findViewById<Button>(R.id.btn_rerun_wizard).setOnClickListener {
            val intent = Intent(this, SetupActivity::class.java)
            intent.putExtra("rerun", true)
            startActivity(intent)
            finish()
        }
    }
}
