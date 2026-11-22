package com.example.sommeilplus

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.widget.RemoteViews
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object SleepNotifications {
    // Versionner l'identifiant permet de recréer le canal avec le son d'alarme
    // même si une ancienne version avait créé un canal silencieux.
    fun alarmChannelId(context: Context) = "sleep_alarm_v8_" +
        (context.getSharedPreferences("app", Context.MODE_PRIVATE).getString("language", "fr") ?: "fr")

    fun ringChannelId(context: Context) = "sleep_alarm_ring_v4_" +
        (context.getSharedPreferences("app", Context.MODE_PRIVATE).getString("language", "fr") ?: "fr")

    /** Crée les deux canaux avant la première alarme pour que l'utilisateur puisse vérifier leurs réglages. */
    fun prepareChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        manager.createNotificationChannel(NotificationChannel(
            alarmChannelId(context), Translator.text(context, "Rappels sommeil"), NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = Translator.text(context, "Notification visible avec sonnerie d’alarme aux heures de coucher et de réveil")
            enableVibration(true)
            setSound(alarmSound, alarmAudio)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        })
        manager.createNotificationChannel(NotificationChannel(
            ringChannelId(context), Translator.text(context, "Alarme Sommeil Plus"), NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = Translator.text(context, "Notification persistante avec les actions Arrêter et Répéter")
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        })
    }

    fun show(context: Context, title: String, message: String, id: Int, alarmType: String? = null) {
        val translatedTitle = Translator.text(context, title)
        val translatedMessage = Translator.text(context, message)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            android.util.Log.w("SommeilPlusAlarm", "POST_NOTIFICATIONS refusée : Android masque les notifications dans le tiroir.")
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.areNotificationsEnabled()) {
            android.util.Log.w("SommeilPlusAlarm", "Notifications désactivées dans les paramètres Android")
            return
        }
        val channel = alarmChannelId(context)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        prepareChannels(context)

        val type = alarmType ?: if (id == 2201) AlarmScheduler.BEDTIME else AlarmScheduler.WAKEUP
        val stopIntent = Intent(context, AlarmActionReceiver::class.java)
            .setAction(AlarmActionReceiver.ACTION_STOP).putExtra("type", type).putExtra("id", id)
        val stopPending = PendingIntent.getBroadcast(context, id + 10, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val snoozeIntent = Intent(context, AlarmActionReceiver::class.java)
            .setAction(AlarmActionReceiver.ACTION_SNOOZE).putExtra("type", type).putExtra("id", id)
        val snoozePending = PendingIntent.getBroadcast(context, id + 20, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val reshowPending = PendingIntent.getBroadcast(context, id + 50,
            Intent(context, AlarmActionReceiver::class.java)
                .setAction(AlarmActionReceiver.ACTION_RESHOW).putExtra("type", type).putExtra("id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val openIntent = Intent(context, NotificationsActivity::class.java)
        val openPending = PendingIntent.getActivity(context, id + 40, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val fullScreenIntent = Intent(context, AlarmScreenActivity::class.java).apply {
            putExtra("type", type)
            putExtra("id", id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPending = PendingIntent.getActivity(
            context, id + 60, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val customView = RemoteViews(context.packageName, R.layout.notification_alarm).apply {
            setTextViewText(R.id.alarm_notification_title, translatedTitle)
            setTextViewText(R.id.alarm_notification_message, translatedMessage)
            setTextViewText(R.id.alarm_notification_stop, Translator.text(context, "Arrêter"))
            setTextViewText(R.id.alarm_notification_snooze, Translator.text(context, "Répéter"))
            setOnClickPendingIntent(R.id.alarm_notification_stop, stopPending)
            setOnClickPendingIntent(R.id.alarm_notification_snooze, snoozePending)
        }
        val notification = NotificationCompat.Builder(context, channel)
            .apply { if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) setSound(alarmSound) }
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setColor(Ui.BLUE)
            .setContentIntent(openPending)
            .setFullScreenIntent(fullScreenPending, true)
            .setDeleteIntent(reshowPending)
            .setContentTitle(translatedTitle)
            .setContentText(translatedMessage)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 700, 300, 700, 300, 700))
            .setOnlyAlertOnce(true)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCustomContentView(customView)
            .setCustomBigContentView(customView)
            .setCustomHeadsUpContentView(customView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()
        manager.notify(id, notification)
    }

    fun cancel(context: Context, id: Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
    }
}

class AlarmActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_STOP = "com.example.sommeilplus.ALARM_STOP"
        const val ACTION_SNOOZE = "com.example.sommeilplus.ALARM_SNOOZE"
        const val ACTION_RESHOW = "com.example.sommeilplus.ALARM_RESHOW"
        const val SNOOZE_MINUTES = 5
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", 7001)
        val type = intent.getStringExtra("type") ?: AlarmScheduler.WAKEUP
        if (intent.action == ACTION_RESHOW) {
            // La notification a ete balayee alors que l'alarme n'est pas encore arretee : on la remet.
            if (!AlarmRingService.repost(type, id)) {
                val bedtime = type == AlarmScheduler.BEDTIME
                SleepNotifications.show(
                    context,
                    if (bedtime) "Il est temps de dormir 🌙" else "Bonjour ☀️",
                    if (bedtime) "Prépare-toi pour une bonne nuit de sommeil." else "C'est l'heure de se réveiller. Bonne journée !",
                    id, type
                )
            }
            return
        }
        // Arrete le son + la vibration et retire la notification.
        AlarmRingService.stop(context)
        SleepNotifications.cancel(context, id)
        if (intent.action == ACTION_SNOOZE) {
            AlarmScheduler.scheduleSnooze(context, type, id, SNOOZE_MINUTES)
        }
    }
}

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type") ?: AlarmScheduler.WAKEUP
        val bedtime = type == AlarmScheduler.BEDTIME
        AlarmScheduler.ring(context, type, intent.getIntExtra("id", if (bedtime) 2201 else 7001))
    }
}
