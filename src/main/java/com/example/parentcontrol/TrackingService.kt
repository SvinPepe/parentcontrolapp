package com.example.parentcontrol

import android.annotation.SuppressLint
import android.content.pm.ServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TrackingService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var prefs: SharedPreferences

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "tracking_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        prefs = getSharedPreferences("parent_control_prefs", MODE_PRIVATE)
        startForegroundService()
        startTracking()
    }

    @SuppressLint("ForegroundServiceType")
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun startForegroundService() {
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Parental Control")
            .setContentText("Tracking screen time")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Для Android 10+ указываем тип сервиса
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            // Для старых версий Android
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Screen Time Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks device usage time"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startTracking() {
        // Сброс дневного лимита в полночь
        scheduleDailyReset()

        // Проверка каждые 30 секунд
        handler.postDelayed(trackingRunnable, 30000)
    }

    private val trackingRunnable = object : Runnable {
        override fun run() {
            val dailyUsage = getTotalUsageToday()
            val dailyLimit = prefs.getLong("daily_limit", 60 * 60 * 1000) // 1 час

            prefs.edit().putLong("daily_usage", dailyUsage).apply()

            if (dailyUsage >= dailyLimit) {
                showTimeLimitScreen()
            }

            handler.postDelayed(this, 30000)
        }
    }

    private fun getTotalUsageToday(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats: List<UsageStats> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
        } else {
            @Suppress("DEPRECATION")
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )
        }

        return stats.sumOf { it.totalTimeInForeground }
    }

    private fun showTimeLimitScreen() {
        val intent = Intent(this, TimeLimitActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }

    private fun scheduleDailyReset() {
        val now = DateTime.now()
        val tomorrow = now.plusDays(1).withTimeAtStartOfDay()
        val millisUntilMidnight = tomorrow.millis - now.millis

        handler.postDelayed({
            prefs.edit().putLong("daily_usage", 0).apply()
            scheduleDailyReset()
        }, millisUntilMidnight)
    }

    override fun onDestroy() {
        handler.removeCallbacks(trackingRunnable)
        super.onDestroy()
    }
}