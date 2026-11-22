package com.example.sommeilplus

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Programme le pré-rappel 30 minutes avant l'heure de coucher, puis le renouvelle chaque jour. */
object ReminderScheduler {
    private const val UNIQUE_NAME = "sommes_plus_pre_sleep_reminder"

    fun scheduleNext(context: Context, fromWorker: Boolean = false) {
        val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("bed", false)) {
            cancel(context)
            return
        }

        val hour = prefs.getInt("bedHour", 22)
        val minute = prefs.getInt("bedMinute", 0)
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -30)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = (next.timeInMillis - System.currentTimeMillis()).coerceAtLeast(10 * 60 * 1000L)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString("message", "Prépare-toi : ton heure de coucher approche 🌙").build())
            .build()
        val policy = if (fromWorker) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, policy, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }
}
