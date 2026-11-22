package com.example.sommeilplus

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

/**
 * Full-screen alarm UI using the existing SommeilPlus colors and drawable styles.
 * The notification remains posted; only an explicit button action dismisses it.
 */
class AlarmScreenActivity : Activity() {

    private var alarmType: String = AlarmScheduler.WAKEUP
    private var alarmId: Int = 7001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_alarm_screen)
        readAlarmIntent(intent)

        val title = findViewById<TextView>(R.id.txtAlarmTitle)
        val message = findViewById<TextView>(R.id.txtAlarmMessage)
        val stop = findViewById<Button>(R.id.btnAlarmStop)
        val snooze = findViewById<Button>(R.id.btnAlarmSnooze)

        title.text = Translator.text(
            this,
            if (alarmType == AlarmScheduler.BEDTIME) "Il est temps de dormir 🌙" else "Bonjour ☀️"
        )
        message.text = Translator.text(
            this,
            if (alarmType == AlarmScheduler.BEDTIME)
                "Prépare-toi pour une bonne nuit de sommeil."
            else
                "C'est l'heure de se réveiller. Bonne journée !"
        )
        stop.text = Translator.text(this, "Arrêter")
        snooze.text = Translator.text(this, "Répéter")

        stop.setOnClickListener { performAlarmAction(AlarmActionReceiver.ACTION_STOP) }
        snooze.setOnClickListener { performAlarmAction(AlarmActionReceiver.ACTION_SNOOZE) }
    }

    private fun readAlarmIntent(source: Intent?) {
        alarmType = source?.getStringExtra("type") ?: AlarmScheduler.WAKEUP
        alarmId = source?.getIntExtra(
            "id",
            if (alarmType == AlarmScheduler.BEDTIME) 2201 else 7001
        ) ?: 7001
    }

    private fun performAlarmAction(action: String) {
        val actionIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            this.action = action
            putExtra("type", alarmType)
            putExtra("id", alarmId)
        }
        sendBroadcast(actionIntent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readAlarmIntent(intent)
    }
}
