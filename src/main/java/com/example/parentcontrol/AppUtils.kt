package com.example.parentcontrol

import android.content.Context
import android.content.SharedPreferences
import org.joda.time.DateTime

class AppUtils {
    companion object {
        fun resetDailyUsageIfNeeded(prefs: SharedPreferences) {
            val lastReset = prefs.getLong("last_reset", 0)
            val now = DateTime.now()

            if (lastReset == 0L || now.dayOfYear != DateTime(lastReset).dayOfYear) {
                prefs.edit()
                    .putLong("daily_usage", 0)
                    .putLong("last_reset", now.millis)
                    .apply()
            }
        }

        fun getAvailableTime(prefs: SharedPreferences): Long {
            val dailyLimit = prefs.getLong("daily_limit", 60 * 60 * 1000) // 1 час
            val dailyUsage = prefs.getLong("daily_usage", 0)
            val extraTime = prefs.getLong("extra_time", 0)

            return dailyLimit - dailyUsage + extraTime
        }
    }
}