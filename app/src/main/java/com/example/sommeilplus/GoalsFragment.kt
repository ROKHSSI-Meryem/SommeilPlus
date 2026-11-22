package com.example.sommeilplus

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment

class GoalsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val c = requireContext()
        val root = Ui.page(c, "Objectifs")
        val prefs = c.getSharedPreferences("goals", 0)
        val hours = Ui.field(c, "Objectif quotidien en heures", 8194).apply { setText(prefs.getFloat("hours", 8f).toString()) }
        val quality = Ui.field(c, "Qualité cible (%)", 2).apply { setText(prefs.getInt("quality", 70).toString()) }
        root.addView(Ui.text(c, "Définir un objectif", 18f, Ui.NAVY, true))
        root.addView(hours); root.addView(quality)
        root.addView(Ui.button(c, "Enregistrer") {
            val h = hours.text.toString().toFloatOrNull()
            val q = quality.text.toString().toIntOrNull()
            if (h == null || h !in 1f..16f || q == null || q !in 1..100) Toast.makeText(c, Translator.text(c, "Objectif invalide"), Toast.LENGTH_SHORT).show()
            else {
                prefs.edit().putFloat("hours", h).putInt("quality", q).apply()
                Toast.makeText(c, Translator.text(c, "Objectifs enregistrés"), Toast.LENGTH_SHORT).show()
                // Recalcule immédiatement la carte de progression avec le nouvel objectif.
                parentFragmentManager.beginTransaction().replace(id, GoalsFragment()).commit()
            }
        })
        val email = c.getSharedPreferences("app", 0).getString("email", "") ?: ""
        val sessions = SleepDatabase(c).sessions(email)
        val goal = prefs.getFloat("hours", 8f)
        val avg = if (sessions.isEmpty()) 0f else sessions.take(7).map { it.hours }.average().toFloat()
        val progress = (avg / goal * 100).toInt().coerceIn(0, 100)
        root.addView(Ui.card(c).apply {
            addView(Ui.text(c, "$progress% de l'objectif atteint", 22f, Ui.GREEN, true))
            addView(ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; this.progress = progress })
            addView(Ui.text(c, "Moyenne récente : %.1f h / %.1f h".format(avg, goal)))
        })
        root.addView(Ui.text(c, "Historique des sessions", 18f, Ui.NAVY, true))
        sessions.forEach { root.addView(Ui.card(c).apply { addView(Ui.text(c, "${it.date}  •  ${it.hours} h  •  ${it.quality}%")) }) }
        return Ui.scroll(c, root)
    }
}
