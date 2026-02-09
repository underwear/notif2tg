package com.notif2tg

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        setupBotSection()
        setupChatSection()
        setupActions()
    }

    private fun setupBotSection() {
        val tvBotInfo = findViewById<TextView>(R.id.tv_bot_info)
        val tvBotToken = findViewById<TextView>(R.id.tv_bot_token_masked)
        val llDisplay = findViewById<LinearLayout>(R.id.ll_bot_display)
        val llEdit = findViewById<LinearLayout>(R.id.ll_bot_edit)
        val etNewToken = findViewById<EditText>(R.id.et_new_token)
        val tvResult = findViewById<TextView>(R.id.tv_token_result)
        val btnVerify = findViewById<Button>(R.id.btn_verify_token)

        refreshBotDisplay(tvBotInfo, tvBotToken)

        findViewById<Button>(R.id.btn_change_token).setOnClickListener {
            llDisplay.visibility = View.GONE
            llEdit.visibility = View.VISIBLE
            tvResult.visibility = View.GONE
            etNewToken.setText("")
        }

        findViewById<Button>(R.id.btn_cancel_token).setOnClickListener {
            llEdit.visibility = View.GONE
            llDisplay.visibility = View.VISIBLE
        }

        btnVerify.setOnClickListener {
            val token = etNewToken.text.toString().trim()
            if (token.isBlank()) {
                Toast.makeText(this, "Paste a token", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnVerify.isEnabled = false
            btnVerify.text = "Verifying..."

            TelegramSender.getMe(token) { info, err ->
                runOnUiThread {
                    btnVerify.isEnabled = true
                    btnVerify.text = "Verify & Save"
                    if (info != null) {
                        Prefs.saveBotInfo(this, token, info.username)
                        tvResult.text = "Saved — @${info.username}"
                        tvResult.setTextColor(0xFF2E7D32.toInt())
                        tvResult.visibility = View.VISIBLE
                        refreshBotDisplay(tvBotInfo, tvBotToken)
                        llEdit.visibility = View.GONE
                        llDisplay.visibility = View.VISIBLE
                        Toast.makeText(this, "Bot updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        tvResult.text = "Invalid token: $err"
                        tvResult.setTextColor(0xFFB71C1C.toInt())
                        tvResult.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun refreshBotDisplay(tvBotInfo: TextView, tvBotToken: TextView) {
        val username = Prefs.getBotUsername(this)
        tvBotInfo.text = if (username.isNotBlank()) "@$username" else "Not configured"
        val token = Prefs.getBotToken(this)
        tvBotToken.text = if (token.length > 10) "${token.take(6)}...${token.takeLast(4)}" else "***"
    }

    private fun setupChatSection() {
        val tvChatInfo = findViewById<TextView>(R.id.tv_chat_info)
        val tvChatDetail = findViewById<TextView>(R.id.tv_chat_detail)
        val btnChange = findViewById<Button>(R.id.btn_change_chat)
        val llEdit = findViewById<LinearLayout>(R.id.ll_chat_edit)
        val etNewChatId = findViewById<EditText>(R.id.et_new_chat_id)

        refreshChatDisplay(tvChatInfo, tvChatDetail)

        btnChange.setOnClickListener {
            btnChange.visibility = View.GONE
            llEdit.visibility = View.VISIBLE
            etNewChatId.setText(Prefs.getChatId(this))
        }

        findViewById<Button>(R.id.btn_cancel_chat).setOnClickListener {
            llEdit.visibility = View.GONE
            btnChange.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.btn_save_chat).setOnClickListener {
            val chatId = etNewChatId.text.toString().trim()
            if (chatId.isBlank()) {
                Toast.makeText(this, "Enter a Chat ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Prefs.saveChatInfo(this, chatId, chatId, "manual")
            refreshChatDisplay(tvChatInfo, tvChatDetail)
            llEdit.visibility = View.GONE
            btnChange.visibility = View.VISIBLE
            Toast.makeText(this, "Chat updated!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshChatDisplay(tvChatInfo: TextView, tvChatDetail: TextView) {
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
    }

    private fun setupActions() {
        findViewById<Button>(R.id.btn_test_message).setOnClickListener {
            val token = Prefs.getBotToken(this)
            val chatId = Prefs.getChatId(this)
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

        findViewById<Button>(R.id.btn_clear_data).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear all data?")
                .setMessage("This will delete your bot token, chat settings, and app filters. You'll need to set up the app again.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear") { _, _ ->
                    getSharedPreferences("notif2tg_prefs", MODE_PRIVATE).edit().clear().apply()
                    val intent = Intent(this, SetupActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                }
                .show()
        }
    }
}
