package com.example.sommeilplus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {
    const val BEDTIME = "bedtime"
    const val WAKEUP = "wakeup"

    fun schedule(context: Context, type: String, hour: Int, minute: Int, minLeadMs: Long = 0L) {
        require(type == BEDTIME || type == WAKEUP) { "Type d'alarme invalide" }
        require(hour in 0..23 && minute in 0..59) { "Heure d'alarme invalide" }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SleepAlarmReceiver::class.java).putExtra("type", type)
        val requestCode = if (type == BEDTIME) 2201 else 7001
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + minLeadMs) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.cancel(pending)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                // AlarmClock is treated by Android as a user-visible alarm and is more reliable
                // for bedtime/wakeup reminders than an ordinary background timer.
                val showIntent = Intent(context, NotificationsActivity::class.java)
                val showPending = PendingIntent.getActivity(
                    context,
                    requestCode + 100,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmInfo = AlarmManager.AlarmClockInfo(next.timeInMillis, showPending)
                alarmManager.setAlarmClock(alarmInfo, pending)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Sans autorisation exacte, Android peut retarder cette alarme.
                android.util.Log.w("SommeilPlusAlarm", "Autorisation exacte absente : programmation approximative")
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
            }
            android.util.Log.i("SommeilPlusAlarm", "Alarme $type programmée pour ${next.time}")
        } catch (security: SecurityException) {
            android.util.Log.e("SommeilPlusAlarm", "Accès aux alarmes exactes refusé", security)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
                }
            } catch (fallbackError: Exception) {
                android.util.Log.e("SommeilPlusAlarm", "Impossible de programmer l'alarme $type", fallbackError)
                throw fallbackError
            }
        }
    }

    /** Demarre la vraie alarme (son en boucle + notification Arreter / Repeter 5 min). */
    fun ring(context: Context, type: String, id: Int) {
        val bedtime = type == BEDTIME
        val title = Translator.text(context, if (bedtime) "Il est temps de dormir 🌙" else "Bonjour ☀️")
        val message = Translator.text(context, if (bedtime) "Prépare-toi pour une bonne nuit de sommeil." else "C'est l'heure de se réveiller. Bonne journée !")

        // Publie d'abord une notification de secours. Le service la remplace ensuite
        // par sa notification persistante avec les boutons et le son en boucle.
        // Ainsi, si Android refuse le démarrage du service en arrière-plan,
        // l'utilisateur reçoit quand même l'alerte visible.
        SleepNotifications.show(context, title, message, id, type)
        try {
            AlarmRingService.start(context, type, id)
        } catch (e: Exception) {
            android.util.Log.e("SommeilPlusAlarm", "Service d'alarme impossible; notification de secours conservée", e)
        }
    }

    /** "Repeter 5 min" : la meme alarme sonnera a nouveau dans [minutes] minutes. */
    fun scheduleSnooze(context: Context, type: String, id: Int, minutes: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val again = Intent(context, SnoozeReceiver::class.java).putExtra("type", type).putExtra("id", id)
        val pending = PendingIntent.getBroadcast(
            context, id + 30, again,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val whenMillis = System.currentTimeMillis() + minutes * 60_000L
        try {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
            if (canExact) {
                val show = PendingIntent.getActivity(
                    context, id + 140, Intent(context, NotificationsActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(whenMillis, show), pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
        }
        android.util.Log.i("SommeilPlusAlarm", "Alarme $type repetee dans $minutes min")
    }

    fun cancel(context: Context, type: String) {
        val requestCode = if (type == BEDTIME) 2201 else 7001
        val intent = Intent(context, SleepAlarmReceiver::class.java).putExtra("type", type)
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pending)
        val snooze = PendingIntent.getBroadcast(
            context, requestCode + 30,
            Intent(context, SnoozeReceiver::class.java).putExtra("type", type).putExtra("id", requestCode),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.cancel(snooze)
    }

    /** Désactive et annule toutes les alarmes lorsque l'utilisateur se déconnecte. */
    fun cancelAll(context: Context) {
        context.getSharedPreferences("reminders", Context.MODE_PRIVATE).edit()
            .putBoolean("bed", false)
            .putBoolean("wake", false)
            .apply()
        cancel(context, BEDTIME)
        cancel(context, WAKEUP)
        AlarmRingService.stop(context)
        SleepNotifications.cancel(context, 2201)
        SleepNotifications.cancel(context, 7001)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.cancel(2201)
        manager.cancel(7001)
    }

    fun rescheduleSaved(context: Context) {
        val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        if (prefs.getBoolean("bed", false)) {
            schedule(context, BEDTIME, prefs.getInt("bedHour", 22), prefs.getInt("bedMinute", 0))
        } else {
            cancel(context, BEDTIME)
        }
        if (prefs.getBoolean("wake", false)) {
            schedule(context, WAKEUP, prefs.getInt("wakeHour", 7), prefs.getInt("wakeMinute", 0))
        } else {
            cancel(context, WAKEUP)
        }
    }
}

class SleepAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type") ?: return
        val bedtime = type == AlarmScheduler.BEDTIME
        AlarmScheduler.ring(context, type, if (bedtime) 2201 else 7001)
        val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(if (bedtime) "bed" else "wake", false)
        if (enabled) {
            // Reprogramme pour demain (minLeadMs evite de re-sonner si Android declenche un peu en avance).
            AlarmScheduler.schedule(
                context,
                type,
                prefs.getInt(if (bedtime) "bedHour" else "wakeHour", if (bedtime) 22 else 7),
                prefs.getInt(if (bedtime) "bedMinute" else "wakeMinute", 0),
                minLeadMs = 60_000L
            )
        }
    }
}

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) AlarmScheduler.rescheduleSaved(context)
    }
}
