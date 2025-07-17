package com.example.parentcontrol

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager // Добавлен импорт
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.parentcontrol.databinding.ActivityMainBinding
import org.joda.time.DateTime

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("parent_control_prefs", MODE_PRIVATE)

        binding.startButton.setOnClickListener {
            if (hasUsageStatsPermission()) {
                startServiceAndHide()
            } else {
                showPermissionExplanationDialog()
            }
        }

        binding.settingsButton.setOnClickListener {
            // Для демонстрации - сброс лимита
            prefs.edit().putLong("daily_usage", 0).apply()
            updateDailyUsageDisplay()
        }

        updateDailyUsageDisplay()
    }

    private fun updateDailyUsageDisplay() {
        val dailyUsage = prefs.getLong("daily_usage", 0)
        val dailyLimit = prefs.getLong("daily_limit", 60 * 60 * 1000) // 1 час по умолчанию
        val usagePercent = (dailyUsage.toFloat() / dailyLimit * 100).toInt()

        binding.dailyUsageText.text = "Usage: ${formatTime(dailyUsage)} / ${formatTime(dailyLimit)}"
        binding.progressBar.progress = usagePercent
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app requires usage stats permission to monitor screen time. Please grant the permission in the next screen.")
            .setPositiveButton("Grant") { _, _ -> requestUsageStatsPermission() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startServiceAndHide() {
        // Запускаем сервис
        val serviceIntent = Intent(this, TrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Скрываем иконку приложения - ИСПРАВЛЕННЫЙ КОД
        packageManager.setComponentEnabledSetting(
            ComponentName(this, MainActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        finish()
    }

    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}