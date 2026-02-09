package com.notif2tg

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ViewFlipper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout

class SetupActivity : AppCompatActivity() {

    private lateinit var flipper: ViewFlipper
    private var polling = false
    private var pollOffset = 0L
    private var detectedChat: ChatInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Prefs.isSetupCompleted(this) && !intent.getBooleanExtra("rerun", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_setup)
        flipper = findViewById(R.id.vf_steps)

        setupWelcome()
        setupBotToken()
        setupChat()
        setupPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }

    // ── Step 0: Welcome ──

    private fun setupWelcome() {
        findViewById<Button>(R.id.btn_get_started).setOnClickListener {
            flipper.displayedChild = 1
        }
    }

    // ── Step 1: Bot Token ──

    private fun setupBotToken() {
        val etToken = findViewById<EditText>(R.id.et_bot_token)
        val btnConnect = findViewById<Button>(R.id.btn_connect_bot)
        val llResult = findViewById<LinearLayout>(R.id.ll_bot_result)
        val tvBotName = findViewById<TextView>(R.id.tv_bot_name)
        val btnNext = findViewById<Button>(R.id.btn_next_to_chat)

        val existing = Prefs.getBotToken(this)
        if (existing.isNotBlank()) etToken.setText(existing)

        btnConnect.setOnClickListener {
            val token = etToken.text.toString().trim()
            if (token.isBlank()) {
                Toast.makeText(this, "Paste your bot token", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnConnect.isEnabled = false
            btnConnect.text = "Connecting..."

            TelegramSender.getMe(token) { info, err ->
                runOnUiThread {
                    btnConnect.isEnabled = true
                    btnConnect.text = "Connect"
                    if (info != null) {
                        Prefs.saveBotInfo(this, token, info.username)
                        tvBotName.text = "@${info.username}"
                        llResult.visibility = View.VISIBLE
                        btnNext.isEnabled = true
                    } else {
                        llResult.visibility = View.GONE
                        btnNext.isEnabled = false
                        Toast.makeText(this, "Invalid token: $err", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btnNext.setOnClickListener { flipper.displayedChild = 2 }
    }

    // ── Step 2: Connect Chat ──

    private fun setupChat() {
        val btnAuto = findViewById<Button>(R.id.btn_auto_detect)
        val llWaiting = findViewById<LinearLayout>(R.id.ll_waiting)
        val tvWaitingMsg = findViewById<TextView>(R.id.tv_waiting_msg)
        val btnStopPolling = findViewById<Button>(R.id.btn_stop_polling)
        val llResult = findViewById<LinearLayout>(R.id.ll_chat_result)
        val tvTitle = findViewById<TextView>(R.id.tv_chat_title)
        val tvType = findViewById<TextView>(R.id.tv_chat_type)
        val btnManual = findViewById<Button>(R.id.btn_manual_chat)
        val tilManual = findViewById<TextInputLayout>(R.id.til_manual_chat)
        val etManual = findViewById<EditText>(R.id.et_manual_chat_id)
        val btnSaveManual = findViewById<Button>(R.id.btn_save_manual_chat)
        val btnNext = findViewById<Button>(R.id.btn_next_to_perms)

        btnAuto.setOnClickListener {
            val botUsername = Prefs.getBotUsername(this)
            btnAuto.visibility = View.GONE
            llWaiting.visibility = View.VISIBLE
            tvWaitingMsg.text = "Send any message to @$botUsername\nfrom the chat you want to use"
            startPolling { chat ->
                runOnUiThread {
                    llWaiting.visibility = View.GONE
                    detectedChat = chat
                    showChatResult(tvTitle, tvType, llResult, btnNext, chat)
                }
            }
        }

        btnStopPolling.setOnClickListener {
            stopPolling()
            llWaiting.visibility = View.GONE
            btnAuto.visibility = View.VISIBLE
        }

        btnManual.setOnClickListener {
            tilManual.visibility = View.VISIBLE
            btnSaveManual.visibility = View.VISIBLE
            btnManual.visibility = View.GONE
        }

        btnSaveManual.setOnClickListener {
            val chatId = etManual.text.toString().trim()
            if (chatId.isBlank()) {
                Toast.makeText(this, "Enter a Chat ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            stopPolling()
            val chat = ChatInfo(chatId, chatId, "manual")
            Prefs.saveChatInfo(this, chat.chatId, chat.title, chat.type)
            detectedChat = chat
            tilManual.visibility = View.GONE
            btnSaveManual.visibility = View.GONE
            llWaiting.visibility = View.GONE
            btnAuto.visibility = View.GONE
            showChatResult(tvTitle, tvType, llResult, btnNext, chat)
        }

        btnNext.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
                    )
                }
            }
            flipper.displayedChild = 3
        }
    }

    private fun showChatResult(
        tvTitle: TextView, tvType: TextView,
        llResult: LinearLayout, btnNext: Button,
        chat: ChatInfo
    ) {
        tvTitle.text = chat.title
        val typeLabel = when (chat.type) {
            "private" -> "Private chat"
            "group" -> "Group"
            "supergroup" -> "Supergroup"
            "channel" -> "Channel"
            else -> chat.type
        }
        tvType.text = "$typeLabel  (${chat.chatId})"
        llResult.visibility = View.VISIBLE
        btnNext.isEnabled = true
    }

    private fun startPolling(onChat: (ChatInfo) -> Unit) {
        polling = true
        pollOffset = 0

        // First, flush old updates
        val token = Prefs.getBotToken(this)
        TelegramSender.getUpdates(token, -1, 0) { updates, _ ->
            pollOffset = if (updates != null && updates.isNotEmpty()) {
                updates.last().first + 1
            } else {
                0
            }
            doPoll(token, onChat)
        }
    }

    private fun doPoll(token: String, onChat: (ChatInfo) -> Unit) {
        if (!polling) return
        TelegramSender.getUpdates(token, pollOffset, 30) { updates, err ->
            if (!polling) return@getUpdates
            if (updates != null && updates.isNotEmpty()) {
                val (updateId, chat) = updates.first()
                pollOffset = updateId + 1
                Prefs.saveChatInfo(this, chat.chatId, chat.title, chat.type)
                polling = false
                onChat(chat)
            } else {
                doPoll(token, onChat)
            }
        }
    }

    private fun stopPolling() {
        polling = false
        TelegramSender.cancelAll()
    }

    // ── Step 3: Permissions ──

    private fun setupPermissions() {
        val btnNotif = findViewById<Button>(R.id.btn_notif_access)
        val btnBattery = findViewById<Button>(R.id.btn_battery_opt)
        val btnFinish = findViewById<Button>(R.id.btn_finish)

        btnNotif.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnBattery.setOnClickListener {
            val pm = getSystemService(PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Already disabled", Toast.LENGTH_SHORT).show()
            }
        }

        btnFinish.setOnClickListener {
            Prefs.setSetupCompleted(this, true)

            // Start keep-alive service
            val serviceIntent = Intent(this, KeepAliveService::class.java)
            startForegroundService(serviceIntent)

            // Send test message
            if (Prefs.isConfigured(this)) {
                TelegramSender.send(
                    Prefs.getBotToken(this),
                    Prefs.getChatId(this),
                    "Notif2TG setup complete — notifications will be forwarded here!"
                )
            }

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (flipper.displayedChild == 3) {
            updatePermissionStatus()
        }
    }

    private fun updatePermissionStatus() {
        val tvNotif = findViewById<TextView>(R.id.tv_notif_status)
        val tvBattery = findViewById<TextView>(R.id.tv_battery_status)

        val listenerEnabled = isNotificationListenerEnabled()
        tvNotif.text = if (listenerEnabled) "Granted" else "Not granted"
        tvNotif.setTextColor(if (listenerEnabled) 0xFF2E7D32.toInt() else 0xFFB71C1C.toInt())

        val pm = getSystemService(PowerManager::class.java)
        val batteryOk = pm.isIgnoringBatteryOptimizations(packageName)
        tvBattery.text = if (batteryOk) "Disabled" else "Not disabled"
        tvBattery.setTextColor(if (batteryOk) 0xFF2E7D32.toInt() else 0xFFB71C1C.toInt())
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, NotificationService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }
}
