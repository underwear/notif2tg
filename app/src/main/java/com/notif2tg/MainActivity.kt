package com.notif2tg

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Prefs.isSetupCompleted(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // Start keep-alive service
        startForegroundService(Intent(this, KeepAliveService::class.java))

        findViewById<Button>(R.id.btn_app_filter).setOnClickListener {
            startActivity(Intent(this, AppFilterActivity::class.java))
        }

        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btn_fix_access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
    }

    private fun updateDashboard() {
        val statusCard = findViewById<LinearLayout>(R.id.ll_status_card)
        val tvTitle = findViewById<TextView>(R.id.tv_status_title)
        val tvDetail = findViewById<TextView>(R.id.tv_status_detail)
        val llWarning = findViewById<LinearLayout>(R.id.ll_warning)

        val listenerEnabled = isNotificationListenerEnabled()
        val chatTitle = Prefs.getChatTitle(this)
        val chatType = Prefs.getChatType(this)

        if (listenerEnabled) {
            statusCard.setBackgroundColor(0xFFE8F5E9.toInt())
            tvTitle.text = "Forwarding active"
            tvTitle.setTextColor(0xFF2E7D32.toInt())
            val typeLabel = when (chatType) {
                "private" -> "private chat"
                "group" -> "group"
                "supergroup" -> "supergroup"
                "channel" -> "channel"
                else -> ""
            }
            tvDetail.text = if (typeLabel.isNotBlank()) "to $chatTitle ($typeLabel)" else "to $chatTitle"
            tvDetail.setTextColor(0xFF388E3C.toInt())
            llWarning.visibility = View.GONE
        } else {
            statusCard.setBackgroundColor(0xFFEEEEEE.toInt())
            tvTitle.text = "Forwarding paused"
            tvTitle.setTextColor(0xFF616161.toInt())
            tvDetail.text = "Notification Access not enabled"
            tvDetail.setTextColor(0xFF9E9E9E.toInt())
            llWarning.visibility = View.VISIBLE
        }

        updateHealth(listenerEnabled)
    }

    private fun updateHealth(listenerEnabled: Boolean) {
        val green = 0xFF4CAF50.toInt()
        val red = 0xFFF44336.toInt()

        val pm = getSystemService(PowerManager::class.java)
        val batteryOk = pm.isIgnoringBatteryOptimizations(packageName)
        val botOk = Prefs.isConfigured(this)

        setHealthDot(R.id.tv_health_notif_dot, R.id.tv_health_notif,
            listenerEnabled, "Notification Access", green, red)
        setHealthDot(R.id.tv_health_battery_dot, R.id.tv_health_battery,
            batteryOk, "Battery Optimization", green, red)
        setHealthDot(R.id.tv_health_bot_dot, R.id.tv_health_bot,
            botOk, "Bot configured", green, red)
    }

    private fun setHealthDot(dotId: Int, textId: Int, ok: Boolean, label: String, green: Int, red: Int) {
        val dot = findViewById<TextView>(dotId)
        val tv = findViewById<TextView>(textId)
        dot.setBackgroundColor(if (ok) green else red)
        tv.text = label
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, NotificationService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }
}
