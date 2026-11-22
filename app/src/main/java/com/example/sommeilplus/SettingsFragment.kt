package com.example.sommeilplus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {
    private var permissionStatus: TextView? = null

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val root = Ui.page(ctx, "Paramètres")

        root.addView(Ui.text(ctx, "NOTIFICATIONS", 16f, Ui.BLUE, true))
        val notificationCard = Ui.card(ctx)
        notificationCard.addView(Ui.text(ctx, "🔔  Autorisation des notifications", 18f, Ui.NAVY, true))
        notificationCard.addView(Ui.text(ctx, "Autorisez Sommeil Plus à vous envoyer les rappels du coucher et du réveil.", 15f, Ui.MUTED))
        permissionStatus = Ui.text(ctx, "", 15f, Ui.GREEN, true)
        notificationCard.addView(permissionStatus)
        notificationCard.addView(Ui.button(ctx, "Autoriser les notifications") { requestNotificationPermission() })
        notificationCard.addView(Ui.button(ctx, "Ouvrir les paramètres Android") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${requireContext().packageName}")))
        })
        root.addView(notificationCard)

        root.addView(Ui.space(ctx, Ui.dp(ctx, 10)))
        root.addView(Ui.text(ctx, "LANGUE / LANGUAGE / اللغة", 16f, Ui.BLUE, true))
        val languageCard = Ui.card(ctx)
        languageCard.addView(Ui.text(ctx, "Choisissez la langue de l'application", 17f, Ui.NAVY, true))
        languageCard.addView(Ui.button(ctx, "🇫🇷  Français") { setLanguage("fr") })
        languageCard.addView(Ui.button(ctx, "🇬🇧  English") { setLanguage("en") })
        languageCard.addView(Ui.button(ctx, "🇲🇦  العربية") { setLanguage("ar") })
        root.addView(languageCard)
        root.addView(Ui.text(ctx, "Le changement de langue est appliqué par Android et peut recréer l'écran.", 14f, Ui.MUTED))

        refreshPermissionStatus()
        return Ui.scroll(ctx, root)
    }

    private fun setLanguage(language: String) {
        requireContext().getSharedPreferences("app", android.content.Context.MODE_PRIVATE)
            .edit().putString("language", language).apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 501)
            } else refreshPermissionStatus()
        } else {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName))
        }
    }

    private fun refreshPermissionStatus() {
        val allowed = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        permissionStatus?.text = Translator.text(requireContext(), if (allowed) "● Autorisation accordée" else "○ Autorisation non accordée")
        permissionStatus?.setTextColor(if (allowed) Ui.GREEN else Ui.ORANGE)
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
    }

    @Deprecated("Deprecated in Android")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 501) refreshPermissionStatus()
    }
}
