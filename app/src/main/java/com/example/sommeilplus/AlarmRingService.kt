package com.example.sommeilplus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Vraie alarme : le son d'alarme et la vibration se repetent en boucle
 * jusqu'a ce que l'utilisateur appuie sur "Arreter" ou "Repeter 5 min".
 * (securite : le son s'arrete tout seul apres 10 minutes)
 */
class AlarmRingService : Service() {
    companion object {
        const val ACTION_START = "com.example.sommeilplus.RING_START"
        const val ACTION_STOP = "com.example.sommeilplus.RING_STOP"
        private const val MAX_RING_MS = 10 * 60 * 1000L

        fun start(context: Context, type: String, id: Int) {
            val intent = Intent(context, AlarmRingService::class.java)
                .setAction(ACTION_START).putExtra("type", type).putExtra("id", id)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingService::class.java))
        }

        @Volatile private var instance: AlarmRingService? = null

        /** Remet la notification (Arreter / Repeter) si l'utilisateur la balaie pendant que l'alarme sonne. */
        fun repost(type: String, id: Int): Boolean {
            val service = instance ?: return false
            service.repostNotification(type, id)
            return true
        }

    }

    private var player: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { stopRinging() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val type = intent?.getStringExtra("type") ?: AlarmScheduler.WAKEUP
        val id = intent?.getIntExtra("id", if (type == AlarmScheduler.BEDTIME) 2201 else 7001) ?: 7001

        instance = this
        val notification = buildNotification(type, id)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ServiceCompat.startForeground(this, id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(id, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("SommeilPlusAlarm", "Impossible de demarrer le service d'alarme", e)
            // Dernier recours : simple notification avec les boutons.
            SleepNotifications.show(
                this, title(type), message(type), id, type
            )
            stopSelf()
            return START_NOT_STICKY
        }

        acquireWakeLock()
        startSound()
        startVibration()
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, MAX_RING_MS)
        return START_NOT_STICKY
    }

    fun repostNotification(type: String, id: Int) {
        try {
            val notification = buildNotification(type, id)
            if (Build.VERSION.SDK_INT >= 29) {
                ServiceCompat.startForeground(this, id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(id, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("SommeilPlusAlarm", "Impossible de remettre la notification", e)
        }
    }

    private fun title(type: String) = Translator.text(
        this, if (type == AlarmScheduler.BEDTIME) "Il est temps de dormir 🌙" else "Bonjour ☀️"
    )

    private fun message(type: String) = Translator.text(
        this,
        if (type == AlarmScheduler.BEDTIME) "Prépare-toi pour une bonne nuit de sommeil."
        else "C'est l'heure de se réveiller. Bonne journée !"
    )

    private fun buildNotification(type: String, id: Int): Notification {
        SleepNotifications.prepareChannels(this)
        val channel = SleepNotifications.ringChannelId(this)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val stopPending = PendingIntent.getBroadcast(
            this, id + 10,
            Intent(this, AlarmActionReceiver::class.java)
                .setAction(AlarmActionReceiver.ACTION_STOP).putExtra("type", type).putExtra("id", id),
            flags
        )
        val snoozePending = PendingIntent.getBroadcast(
            this, id + 20,
            Intent(this, AlarmActionReceiver::class.java)
                .setAction(AlarmActionReceiver.ACTION_SNOOZE).putExtra("type", type).putExtra("id", id),
            flags
        )
        val openPending = PendingIntent.getActivity(
            this, id + 40, Intent(this, NotificationsActivity::class.java), flags
        )
        val reshowPending = PendingIntent.getBroadcast(
            this, id + 50,
            Intent(this, AlarmActionReceiver::class.java)
                .setAction(AlarmActionReceiver.ACTION_RESHOW).putExtra("type", type).putExtra("id", id),
            flags
        )
        val fullScreenIntent = Intent(this, AlarmScreenActivity::class.java).apply {
            putExtra("type", type)
            putExtra("id", id)
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPending = PendingIntent.getActivity(
            this, id + 60, fullScreenIntent, flags
        )
        val customView = RemoteViews(packageName, R.layout.notification_alarm).apply {
            setTextViewText(R.id.alarm_notification_title, title(type))
            setTextViewText(R.id.alarm_notification_message, message(type))
            setTextViewText(R.id.alarm_notification_stop, Translator.text(this@AlarmRingService, "Arrêter"))
            setTextViewText(R.id.alarm_notification_snooze, Translator.text(this@AlarmRingService, "Répéter 5 min"))
            setOnClickPendingIntent(R.id.alarm_notification_stop, stopPending)
            setOnClickPendingIntent(R.id.alarm_notification_snooze, snoozePending)
        }
        return NotificationCompat.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setColor(Ui.BLUE)
            .setContentTitle(title(type))
            .setContentText(message(type))
            .setContentIntent(openPending)
            .setFullScreenIntent(fullScreenPending, true)
            .setDeleteIntent(reshowPending)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCustomContentView(customView)
            .setCustomBigContentView(customView)
            .setCustomHeadsUpContentView(customView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()
    }

    private fun alarmUri() =
        RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun startSound() {
        stopSound()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(this@AlarmRingService, alarmUri())
                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
                android.util.Log.i("SommeilPlusAlarm", "Sonnerie d'alarme démarrée")
            }
        } catch (e: Exception) {
            android.util.Log.e("SommeilPlusAlarm", "MediaPlayer indisponible, utilisation de Ringtone", e)
            try {
                player?.release(); player = null
                ringtone = RingtoneManager.getRingtone(this, alarmUri())?.apply {
                    audioAttributes = attrs
                    if (Build.VERSION.SDK_INT >= 28) isLooping = true
                    play()
                    android.util.Log.i("SommeilPlusAlarm", "Sonnerie de secours démarrée")
                }
            } catch (e2: Exception) {
                android.util.Log.e("SommeilPlusAlarm", "Aucun son d'alarme possible", e2)
            }
        }
    }

    private fun stopSound() {
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
        try { ringtone?.stop() } catch (_: Exception) {}
        ringtone = null
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private fun startVibration() {
        val pattern = longArrayOf(0, 700, 400, 700, 400)
        try {
            val v = vibrator() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION") v.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            android.util.Log.w("SommeilPlusAlarm", "Vibration impossible", e)
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SommeilPlus:alarm").apply {
                acquire(MAX_RING_MS + 5_000L)
            }
        } catch (_: Exception) {}
    }

    /** Arrete le son/vibration (la notification reste avec ses boutons). */
    private fun stopRinging() {
        stopSound()
        try { vibrator()?.cancel() } catch (_: Exception) {}
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacks(timeout)
        stopRinging()
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (_: Exception) {}
        super.onDestroy()
    }
}
