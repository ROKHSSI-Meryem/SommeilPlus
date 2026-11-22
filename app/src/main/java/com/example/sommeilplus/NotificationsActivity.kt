package com.example.sommeilplus

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class NotificationsActivity : AppCompatActivity() {
    private lateinit var bedtimeSwitch: Switch
    private lateinit var wakeSwitch: Switch
    private lateinit var bedtimeTime: Button
    private lateinit var wakeTime: Button
    private lateinit var bedClock: TextView
    private lateinit var wakeClock: TextView
    private lateinit var trackingButton: Button
    private lateinit var saveAlarmsButton: Button
    private lateinit var disableAlarmsButton: Button
    private lateinit var notificationPermissionStatus: TextView
    private var bedHour = 22
    private var bedMinute = 0
    private var wakeHour = 7
    private var wakeMinute = 0
    private var alarmsConfigurationSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF07131D.toInt()
        window.navigationBarColor = 0xFF050D14.toInt()
        SleepNotifications.prepareChannels(this)
        val prefs = getSharedPreferences("reminders", MODE_PRIVATE)
        bedHour = prefs.getInt("bedHour", 22)
        bedMinute = prefs.getInt("bedMinute", 0)
        wakeHour = prefs.getInt("wakeHour", 7)
        wakeMinute = prefs.getInt("wakeMinute", 0)
        // L'état enregistré est déterminé par les alarmes réellement activées.
        // Certaines anciennes versions pouvaient laisser "alarmsConfigured" à false
        // même si bed/wake étaient déjà sauvegardés : on ne doit donc pas se fier
        // uniquement à cette ancienne clé, sinon les deux boutons apparaissent.
        alarmsConfigurationSaved = prefs.getBoolean("bed", false) || prefs.getBoolean("wake", false)

        val root = Ui.page(this, "Réveil & coucher", showBack = true)
        root.addView(Ui.text(this, "VOTRE ROUTINE DE SOMMEIL", 11f, Ui.MUTED, true))
        root.addView(Ui.card(this).apply {
            addView(Ui.text(this@NotificationsActivity, "🔔  Autorisation des notifications", 15f, Ui.BLUE, true))
            notificationPermissionStatus = Ui.text(this@NotificationsActivity, "", 13f, Ui.ORANGE, true)
            addView(notificationPermissionStatus)
            addView(Ui.button(this@NotificationsActivity, "Ouvrir les paramètres Android") {
                openNotificationSettings()
            })
        })
        refreshNotificationPermissionStatus()
        val clockCard = Ui.card(this).apply {
            gravity = android.view.Gravity.CENTER
            addView(Ui.text(this@NotificationsActivity, "☾", 34f, Ui.BLUE, true).apply { gravity = android.view.Gravity.CENTER })
            bedClock = Ui.text(this@NotificationsActivity, formatTime(bedHour, bedMinute), 34f, android.graphics.Color.WHITE, true).apply { gravity = android.view.Gravity.CENTER }
            addView(bedClock)
            addView(Ui.text(this@NotificationsActivity, "HEURE DU COUCHER", 10f, Ui.MUTED, true).apply { gravity = android.view.Gravity.CENTER })
            addView(Ui.text(this@NotificationsActivity, "•  •  •", 12f, Ui.BLUE, true).apply {
                gravity = android.view.Gravity.CENTER
                setPadding(0, Ui.dp(this@NotificationsActivity, 8), 0, Ui.dp(this@NotificationsActivity, 8))
            })
            wakeClock = Ui.text(this@NotificationsActivity, formatTime(wakeHour, wakeMinute), 29f, android.graphics.Color.WHITE, true).apply { gravity = android.view.Gravity.CENTER }
            addView(wakeClock)
            addView(Ui.text(this@NotificationsActivity, "HEURE DU RÉVEIL", 10f, Ui.MUTED, true).apply { gravity = android.view.Gravity.CENTER })
        }
        root.addView(clockCard)
        root.addView(Ui.text(this, "Choisissez vos heures et activez les rappels.", 13f, Ui.MUTED))

        val bedCard = Ui.card(this)
        bedCard.addView(Ui.text(this, "☾  Rappel du coucher", 17f, Ui.BLUE, true))
        bedCard.addView(Ui.text(this, "Recevez un rappel lorsqu'il est temps de vous préparer à dormir.", 13f, Ui.MUTED))
        bedtimeSwitch = Switch(this).apply {
            text = Translator.text(this@NotificationsActivity, "Activer le rappel du coucher")
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            isChecked = prefs.getBoolean("bed", false)
        }
        bedtimeTime = Ui.button(this, "Choisir l'heure · ${formatTime(bedHour, bedMinute)}") {
            TimePickerDialog(this, { _, h, m ->
                bedHour = h; bedMinute = m
                alarmsConfigurationSaved = false
                bedtimeTime.text = Translator.text(this, "Choisir l'heure · ${formatTime(h, m)}")
                bedClock.text = formatTime(h, m)
                updateAlarmActionButtons(bedtimeSwitch.isChecked || wakeSwitch.isChecked)
            }, bedHour, bedMinute, true).show()
        }
        bedtimeSwitch.setOnCheckedChangeListener { _, _ ->
            alarmsConfigurationSaved = false
            updateAlarmActionButtons(bedtimeSwitch.isChecked || wakeSwitch.isChecked)
        }
        bedCard.addView(bedtimeSwitch)
        bedCard.addView(bedtimeTime)
        root.addView(bedCard)

        val wakeCard = Ui.card(this)
        wakeCard.addView(Ui.text(this, "☀  Rappel du réveil", 17f, Ui.BLUE, true))
        wakeCard.addView(Ui.text(this, "Commencez la journée à une heure stable.", 13f, Ui.MUTED))
        wakeSwitch = Switch(this).apply {
            text = Translator.text(this@NotificationsActivity, "Activer le rappel du réveil")
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            isChecked = prefs.getBoolean("wake", false)
        }
        wakeTime = Ui.button(this, "Choisir l'heure · ${formatTime(wakeHour, wakeMinute)}") {
            TimePickerDialog(this, { _, h, m ->
                wakeHour = h; wakeMinute = m
                alarmsConfigurationSaved = false
                wakeTime.text = Translator.text(this, "Choisir l'heure · ${formatTime(h, m)}")
                wakeClock.text = formatTime(h, m)
                updateAlarmActionButtons(bedtimeSwitch.isChecked || wakeSwitch.isChecked)
            }, wakeHour, wakeMinute, true).show()
        }
        wakeSwitch.setOnCheckedChangeListener { _, _ ->
            alarmsConfigurationSaved = false
            updateAlarmActionButtons(bedtimeSwitch.isChecked || wakeSwitch.isChecked)
        }
        wakeCard.addView(wakeSwitch)
        wakeCard.addView(wakeTime)
        root.addView(wakeCard)

        root.addView(Ui.card(this).apply {
            addView(Ui.text(this@NotificationsActivity, "ℹ️  À savoir", 15f, Ui.PURPLE, true))
            addView(Ui.text(this@NotificationsActivity, "Les alarmes sont programmées chaque jour. Android peut appliquer des restrictions selon les réglages de batterie et d'autorisations.", 13f, Ui.MUTED))
        })
        root.addView(Ui.space(this, 12))
        val trackingPrefs = getSharedPreferences("sleep_tracking", MODE_PRIVATE)
        trackingButton = Ui.button(this, if (trackingPrefs.contains("startedAt")) "■  Terminer le suivi" else "▶  Démarrer") { toggleTracking() }
        root.addView(trackingButton)
        saveAlarmsButton = Ui.button(this, "Enregistrer les alarmes") { save() }
        disableAlarmsButton = Ui.secondaryButton(this, "Désactiver les deux alarmes") {
            bedtimeSwitch.isChecked = false
            wakeSwitch.isChecked = false
            save()
        }
        root.addView(saveAlarmsButton)
        root.addView(disableAlarmsButton)
        updateAlarmActionButtons(bedtimeSwitch.isChecked || wakeSwitch.isChecked)
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
        }
        screen.addView(Ui.scroll(this, root), LinearLayout.LayoutParams(-1, 0, 1f))
        screen.addView(createBottomNavigation())
        setContentView(Ui.edgeToEdge(this, screen))
        requestNotificationPermissionIfNeeded()
    }


    private fun createBottomNavigation(): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF0B1B27.toInt())
            elevation = 12f
            setPadding(Ui.dp(this@NotificationsActivity, 3), Ui.dp(this@NotificationsActivity, 4), Ui.dp(this@NotificationsActivity, 3), Ui.dp(this@NotificationsActivity, 4))
        }
        val labels = listOf("Accueil", "Alarme", "Buts", "Conseils", "Profil", "Réglages")
        val icons = listOf("⌂", "◷", "♡", "✿", "♙", "⚙")
        labels.forEachIndexed { index, label ->
            val item = TextView(this).apply {
                text = icons[index] + "\n" + Translator.text(this@NotificationsActivity, label)
                textSize = 10f
                gravity = android.view.Gravity.CENTER
                setTextColor(if (index == 1) Ui.BLUE else Ui.MUTED)
                setPadding(Ui.dp(this@NotificationsActivity, 2), Ui.dp(this@NotificationsActivity, 5), Ui.dp(this@NotificationsActivity, 2), Ui.dp(this@NotificationsActivity, 5))
                layoutParams = LinearLayout.LayoutParams(0, Ui.dp(this@NotificationsActivity, 60), 1f)
                setOnClickListener {
                    val intent = Intent(this@NotificationsActivity, MainActivity::class.java)
                        .putExtra("selected_tab", index)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    finish()
                }
            }
            nav.addView(item)
        }
        return nav
    }

    private fun toggleTracking() {
        val prefs = getSharedPreferences("sleep_tracking", MODE_PRIVATE)
        if (!prefs.contains("startedAt")) {
            prefs.edit().putLong("startedAt", System.currentTimeMillis()).apply()
            trackingButton.text = Translator.text(this, "■  Terminer le suivi")
            Toast.makeText(this, Translator.text(this, "Suivi du sommeil démarré"), Toast.LENGTH_SHORT).show()
            return
        }
        val startedAt = prefs.getLong("startedAt", System.currentTimeMillis())
        val hours = ((System.currentTimeMillis() - startedAt).coerceAtLeast(0L) / 3_600_000f).coerceIn(0.01f, 24f)
        val qualityInput = Ui.field(this, "Qualité de 0 à 100", android.text.InputType.TYPE_CLASS_NUMBER).apply { setText("80") }
        android.app.AlertDialog.Builder(this)
            .setTitle(Translator.text(this, "Terminer le suivi · %.1f h".format(hours)))
            .setMessage(Translator.text(this, "Évaluez la qualité de votre sommeil."))
            .setView(qualityInput)
            .setPositiveButton(Translator.text(this, "Enregistrer")) { _, _ ->
                val quality = qualityInput.text.toString().toIntOrNull()
                if (quality == null || quality !in 0..100) {
                    Toast.makeText(this, Translator.text(this, "Qualité invalide"), Toast.LENGTH_SHORT).show()
                } else {
                    val email = getSharedPreferences("app", MODE_PRIVATE).getString("email", "") ?: ""
                    SleepDatabase(this).addSession(hours, quality, email)
                    prefs.edit().remove("startedAt").apply()
                    trackingButton.text = Translator.text(this, "▶  Démarrer")
                    Toast.makeText(this, Translator.text(this, "Session de sommeil enregistrée"), Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(Translator.text(this, "Continuer le suivi"), null)
            .show().also { Ui.styleDialogButtons(it) }
    }

    private fun save() {
        getSharedPreferences("reminders", MODE_PRIVATE).edit()
            .putBoolean("bed", bedtimeSwitch.isChecked)
            .putBoolean("wake", wakeSwitch.isChecked)
            .putInt("bedHour", bedHour).putInt("bedMinute", bedMinute)
            .putInt("wakeHour", wakeHour).putInt("wakeMinute", wakeMinute)
            .putBoolean("alarmsConfigured", bedtimeSwitch.isChecked || wakeSwitch.isChecked)
            .commit()
        alarmsConfigurationSaved = bedtimeSwitch.isChecked || wakeSwitch.isChecked

        // Reprogramme les deux alarmes indépendamment à partir des préférences sauvegardées.
        // Ainsi, le rappel du coucher sonne lui aussi à l'heure choisie, pas seulement au réveil.
        try {
            AlarmScheduler.rescheduleSaved(this)
            Cloud.pushReminders(this)
            Toast.makeText(this, Translator.text(this, "Paramètres enregistrés"), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.e("SommeilPlusAlarm", "Erreur lors de l'enregistrement des alarmes", e)
            Toast.makeText(this, Translator.text(this, "Paramètres enregistrés, mais la programmation des alarmes a échoué."), Toast.LENGTH_LONG).show()
        }
        updateAlarmActionButtons(bedtimeSwitch.isChecked || wakeSwitch.isChecked)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionIfNeeded()
        }
        askForExactAlarmAccessIfNeeded()
    }

    private fun updateAlarmActionButtons(alarmsEnabled: Boolean) {
        // Les deux boutons sont mutuellement exclusifs :
        // - configuration non enregistrée (ou aucune alarme active) => Enregistrer seulement ;
        // - configuration enregistrée avec au moins une alarme active => Désactiver seulement.
        // Cette méthode est appelée à l'ouverture, après chaque modification et après sauvegarde.
        val showDisable = alarmsConfigurationSaved && alarmsEnabled
        saveAlarmsButton.visibility = if (showDisable) android.view.View.GONE else android.view.View.VISIBLE
        disableAlarmsButton.visibility = if (showDisable) android.view.View.VISIBLE else android.view.View.GONE
        saveAlarmsButton.isEnabled = !showDisable
        disableAlarmsButton.isEnabled = showDisable
    }

    override fun onResume() {
        super.onResume()
        if (::notificationPermissionStatus.isInitialized) refreshNotificationPermissionStatus()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val manager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (manager.canScheduleExactAlarms()) AlarmScheduler.rescheduleSaved(this)
        }
    }

    private fun refreshNotificationPermissionStatus() {
        val permissionAllowed = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        val appAllowed = manager.areNotificationsEnabled()
        val channelsAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = manager.getNotificationChannel(SleepNotifications.alarmChannelId(this))
            val ringChannel = manager.getNotificationChannel(SleepNotifications.ringChannelId(this))
            alarmChannel?.importance != android.app.NotificationManager.IMPORTANCE_NONE &&
                ringChannel?.importance != android.app.NotificationManager.IMPORTANCE_NONE
        } else true
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (getSystemService(ALARM_SERVICE) as android.app.AlarmManager).canScheduleExactAlarms()

        val status = when {
            !permissionAllowed -> "○ Autorisation des notifications non accordée dans Android"
            !appAllowed || !channelsAllowed -> "○ Notifications ou canal Sommeil Plus désactivé dans Android"
            !exactAllowed -> "● Notifications autorisées · Autorisez aussi les alarmes exactes pour respecter l'heure"
            else -> "● Notifications et alarmes autorisées"
        }
        notificationPermissionStatus.text = Translator.text(this, status)
        notificationPermissionStatus.setTextColor(if (permissionAllowed && appAllowed && channelsAllowed && exactAllowed) Ui.GREEN else Ui.ORANGE)
    }

    private fun openNotificationSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= 26) {
                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            }
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun askForExactAlarmAccessIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!manager.canScheduleExactAlarms() && (bedtimeSwitch.isChecked || wakeSwitch.isChecked)) {
                android.app.AlertDialog.Builder(this)
                    .setTitle(Translator.text(this, "Autoriser les alarmes précises"))
                    .setMessage(Translator.text(this, "Pour déclencher les rappels au plus près de l'heure choisie, autorisez Sommeil Plus à programmer des alarmes exactes dans les paramètres Android. Sans cette autorisation, Android peut retarder les rappels."))
                    .setPositiveButton(Translator.text(this, "Ouvrir les paramètres")) { _, _ ->
                        try {
                            startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:$packageName")))
                        } catch (_: Exception) {
                            Toast.makeText(this, Translator.text(this, "Ouvrez les paramètres des alarmes de l'application."), Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton(Translator.text(this, "Plus tard"), null)
                    .show()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 501)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 501) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            Toast.makeText(this, Translator.text(this, if (granted) "Notifications autorisées" else "Autorisation refusée : ouvrez les paramètres Android pour activer les notifications de Sommeil Plus."), Toast.LENGTH_LONG).show()
            refreshNotificationPermissionStatus()
            if (granted) AlarmScheduler.rescheduleSaved(this)
        }
    }

    private fun formatTime(hour: Int, minute: Int) = "%02d:%02d".format(hour, minute)
}
