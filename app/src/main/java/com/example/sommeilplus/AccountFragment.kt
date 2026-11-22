package com.example.sommeilplus

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import android.content.Intent

class AccountFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val c = requireContext()
        val root = Ui.page(c, "Mon compte")
        val email = c.getSharedPreferences("app", 0).getString("email", "") ?: ""
        val user = SleepDatabase(c).user(email)

        root.addView(Ui.card(c).apply {
            addView(Ui.text(c, "👤  ${user?.name ?: "Utilisateur"}", 21f, Ui.NAVY, true))
            addView(Ui.text(c, user?.email ?: email, 14f, Ui.MUTED))
            addView(Ui.text(c, "Date de naissance : ${user?.birth ?: "—"}", 13f, Ui.NAVY))
            addView(Ui.text(c, "Objectif quotidien : ${c.getSharedPreferences("goals", 0).getFloat("hours", 8f)} h", 13f, Ui.NAVY))
            addView(Ui.text(c, "Fuseau horaire : ${java.util.TimeZone.getDefault().id}", 13f, Ui.MUTED))
        })

        root.addView(Ui.button(c, "📚  Historique des nuits") {
            startActivity(Intent(c, HistoryActivity::class.java))
        })
        root.addView(Ui.button(c, "📈  Analyse du sommeil") {
            startActivity(Intent(c, AnalysisActivity::class.java))
        })
        root.addView(Ui.button(c, "📤  Exporter mes données") {
            startActivity(Intent(c, ExportActivity::class.java))
        })
        root.addView(Ui.space(c, 12))
        root.addView(Ui.button(c, "Déconnexion") {
            // La déconnexion arrête les alarmes et leurs répétitions.
            AlarmScheduler.cancelAll(c)
            Cloud.signOut(c)
            // Apres la deconnexion : Splash anime > Login (sans reafficher l'onboarding).
            c.getSharedPreferences("app", 0).edit()
                .remove("email").remove("loggedIn").remove("pendingEmail")
                .apply()
            startActivity(Intent(c, SplashActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        })
        return Ui.scroll(c, root)
    }
}
