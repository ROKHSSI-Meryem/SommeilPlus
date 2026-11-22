package com.example.sommeilplus

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ReminderScheduler.scheduleNext(applicationContext, fromWorker = true)
            return Result.success()
        }
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val language = applicationContext.getSharedPreferences("app", Context.MODE_PRIVATE).getString("language", "fr") ?: "fr"
        val channel = "sleep_reminders_" + language
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channel, Translator.text(applicationContext, "Rappels sommeil"), NotificationManager.IMPORTANCE_DEFAULT).apply { description = Translator.text(applicationContext, "Prenez soin de votre sommeil.") })
        }
        val notification = NotificationCompat.Builder(applicationContext, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Sommeil Plus")
            .setContentText(Translator.text(applicationContext, inputData.getString("message") ?: "Prenez soin de votre sommeil."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
        // WorkManager planifie le prochain pré-rappel quotidien si le rappel coucher est actif.
        ReminderScheduler.scheduleNext(applicationContext, fromWorker = true)
        return Result.success()
    }
}
