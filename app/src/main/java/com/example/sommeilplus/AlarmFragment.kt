package com.example.sommeilplus

import android.content.Intent
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment

class AlarmFragment : Fragment() {
    private var bedTimeView: TextView? = null
    private var wakeTimeView: TextView? = null
    private var bedStatusView: TextView? = null
    private var wakeStatusView: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: android.os.Bundle?): View {
        val c = requireContext()
        val root = Ui.page(c, "Alarmes")
        val prefs = c.getSharedPreferences("reminders", 0)
        val bedTime = "%02d:%02d".format(prefs.getInt("bedHour", 22), prefs.getInt("bedMinute", 0))
        val wakeTime = "%02d:%02d".format(prefs.getInt("wakeHour", 7), prefs.getInt("wakeMinute", 0))
        val bedEnabled = prefs.getBoolean("bed", false)
        val wakeEnabled = prefs.getBoolean("wake", false)

        root.addView(Ui.text(c, "Organisez votre rythme de sommeil", 17f, Ui.MUTED))
        root.addView(Ui.card(c).apply {
            addView(Ui.text(c, "☾  ALARME DU COUCHER", 14f, Ui.BLUE, true))
            bedTimeView = Ui.text(c, bedTime, 38f, android.graphics.Color.WHITE, true)
            addView(bedTimeView)
            bedStatusView = Ui.text(c, if (bedEnabled) "● Activée" else "○ Désactivée", 15f, if (bedEnabled) Ui.GREEN else Ui.MUTED, true)
            addView(bedStatusView)
            addView(Ui.text(c, "Un rappel pour vous préparer à dormir.", 14f, Ui.MUTED))
        })
        root.addView(Ui.card(c).apply {
            addView(Ui.text(c, "☀  ALARME DU RÉVEIL", 14f, Ui.BLUE, true))
            wakeTimeView = Ui.text(c, wakeTime, 38f, android.graphics.Color.WHITE, true)
            addView(wakeTimeView)
            wakeStatusView = Ui.text(c, if (wakeEnabled) "● Activée" else "○ Désactivée", 15f, if (wakeEnabled) Ui.GREEN else Ui.MUTED, true)
            addView(wakeStatusView)
            addView(Ui.text(c, "Un rappel pour commencer la journée.", 14f, Ui.MUTED))
        })
        root.addView(Ui.button(c, "⚙  Configurer les alarmes") {
            startActivity(Intent(c, NotificationsActivity::class.java))
        })
        root.addView(Ui.text(c, "Les heures et les activations affichées ici sont celles enregistrées dans vos paramètres.", 13f, Ui.MUTED))
        return Ui.scroll(c, root)
    }

    override fun onResume() {
        super.onResume()
        val c = context ?: return
        val prefs = c.getSharedPreferences("reminders", 0)
        val bedEnabled = prefs.getBoolean("bed", false)
        val wakeEnabled = prefs.getBoolean("wake", false)
        bedTimeView?.text = "%02d:%02d".format(prefs.getInt("bedHour", 22), prefs.getInt("bedMinute", 0))
        wakeTimeView?.text = "%02d:%02d".format(prefs.getInt("wakeHour", 7), prefs.getInt("wakeMinute", 0))
        bedStatusView?.apply {
            text = Translator.text(c, if (bedEnabled) "● Activée" else "○ Désactivée")
            setTextColor(if (bedEnabled) Ui.GREEN else Ui.MUTED)
        }
        wakeStatusView?.apply {
            text = Translator.text(c, if (wakeEnabled) "● Activée" else "○ Désactivée")
            setTextColor(if (wakeEnabled) Ui.GREEN else Ui.MUTED)
        }
    }
}
