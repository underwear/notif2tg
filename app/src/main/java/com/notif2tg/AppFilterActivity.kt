package com.notif2tg

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    var enabled: Boolean
)

class AppFilterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_filter)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_apps)
        val tvEmpty = findViewById<TextView>(R.id.tv_empty)

        val seenApps = Prefs.getSeenApps(this)
        if (seenApps.isEmpty()) {
            tvEmpty.visibility = android.view.View.VISIBLE
            recyclerView.visibility = android.view.View.GONE
            return
        }

        val pm = packageManager
        val apps = seenApps.mapNotNull { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                AppInfo(
                    packageName = pkg,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo),
                    enabled = Prefs.isAppEnabled(this, pkg)
                )
            } catch (e: PackageManager.NameNotFoundException) {
                AppInfo(pkg, pkg, null, Prefs.isAppEnabled(this, pkg))
            }
        }.sortedBy { it.appName.lowercase() }

        toolbar.title = "App Filters (${apps.size})"

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AppAdapter(apps) { app, enabled ->
            Prefs.setAppEnabled(this, app.packageName, enabled)
        }
    }
}

class AppAdapter(
    private val apps: List<AppInfo>,
    private val onToggle: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
    ) {
        val icon: ImageView = itemView.findViewById(R.id.iv_icon)
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val pkg: TextView = itemView.findViewById(R.id.tv_package)
        val toggle: MaterialSwitch = itemView.findViewById(R.id.sw_toggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.appName
        holder.pkg.text = app.packageName
        holder.toggle.setOnCheckedChangeListener(null)
        holder.toggle.isChecked = app.enabled
        holder.toggle.setOnCheckedChangeListener { _, checked ->
            app.enabled = checked
            onToggle(app, checked)
        }
    }
}
