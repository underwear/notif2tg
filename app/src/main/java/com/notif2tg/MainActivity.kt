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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var etBotToken: EditText
    private lateinit var etChatId: EditText
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etBotToken = findViewById(R.id.et_bot_token)
        etChatId = findViewById(R.id.et_chat_id)
        tvStatus = findViewById(R.id.tv_status)

        etBotToken.setText(Prefs.getBotToken(this))
        etChatId.setText(Prefs.getChatId(this))

        findViewById<Button>(R.id.btn_save_test).setOnClickListener {
            val token = etBotToken.text.toString().trim()
            val chatId = etChatId.text.toString().trim()
            if (token.isBlank() || chatId.isBlank()) {
                Toast.makeText(this, "Both fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Prefs.save(this, token, chatId)
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()

            TelegramSender.send(token, chatId, "Notif2TG test message — forwarding is working!") { ok, err ->
                runOnUiThread {
                    if (ok) {
                        Toast.makeText(this, "Test message sent!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Send failed: $err", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        findViewById<Button>(R.id.btn_notification_access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btn_battery).setOnClickListener {
            val pm = getSystemService(PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Battery optimization already disabled", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btn_app_filter).setOnClickListener {
            startActivity(Intent(this, AppFilterActivity::class.java))
        }

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
                )
            }
        }

        // Start keep-alive service
        val serviceIntent = Intent(this, KeepAliveService::class.java)
        startForegroundService(serviceIntent)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val listenerEnabled = isNotificationListenerEnabled()
        val configured = Prefs.isConfigured(this)

        val status = buildString {
            append("Notification Access: ")
            append(if (listenerEnabled) "ENABLED" else "DISABLED")
            append("\nBot configured: ")
            append(if (configured) "YES" else "NO")
        }
        tvStatus.text = status
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, NotificationService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }
}
