package com.example.sommeilplus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat

class SettingsActivity : AppCompatActivity() {
    private lateinit var permissionStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.BG
        window.navigationBarColor = 0xFF050D14.toInt()
        val root = Ui.page(this, "Paramètres", showBack = true)

        root.addView(Ui.text(this, "NOTIFICATIONS", 15f, Ui.BLUE, true))
        val notificationCard = Ui.card(this)
        notificationCard.addView(Ui.text(this, "🔔  Autorisation des notifications", 18f, Ui.NAVY, true))
        notificationCard.addView(Ui.text(this, "Autorisez Sommeil Plus à vous envoyer les rappels du coucher et du réveil.", 14f, Ui.MUTED))
        permissionStatus = Ui.text(this, "", 14f, Ui.GREEN, true)
        notificationCard.addView(permissionStatus)
        notificationCard.addView(Ui.button(this, "Autoriser les notifications") { requestNotificationPermission() })
        notificationCard.addView(Ui.button(this, "Ouvrir les paramètres Android") {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            startActivity(intent)
        })
        root.addView(notificationCard)

        root.addView(Ui.text(this, "LANGUE / LANGUAGE / اللغة", 15f, Ui.BLUE, true))
        root.addView(Ui.card(this).apply {
            addView(Ui.text(this@SettingsActivity, "Choisissez la langue de l'application", 16f, Ui.NAVY, true))
            addView(Ui.button(this@SettingsActivity, "🇫🇷  Français") { setLanguage("fr") })
            addView(Ui.button(this@SettingsActivity, "🇬🇧  English") { setLanguage("en") })
            addView(Ui.button(this@SettingsActivity, "🇲🇦  العربية") { setLanguage("ar") })
        })
        root.addView(Ui.text(this, "Le changement de langue est appliqué par AndroidX et peut recréer l'écran.", 13f, Ui.MUTED))
        setContentView(Ui.edgeToEdge(this, Ui.withBottomNavigation(this, Ui.scroll(this, root), 5)))
        refreshPermissionStatus()
    }

    private fun setLanguage(language: String) {
        getSharedPreferences("app", MODE_PRIVATE).edit().putString("language", language).apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 501)
            } else {
                refreshPermissionStatus()
            }
        } else {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        }
    }

    private fun refreshPermissionStatus() {
        val allowed = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        permissionStatus.text = Translator.text(this, if (allowed) "● Autorisation accordée" else "○ Autorisation non accordée")
        permissionStatus.setTextColor(if (allowed) Ui.GREEN else Ui.ORANGE)
    }

    override fun onResume() {
        super.onResume()
        if (::permissionStatus.isInitialized) refreshPermissionStatus()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 501) refreshPermissionStatus()
    }
}
