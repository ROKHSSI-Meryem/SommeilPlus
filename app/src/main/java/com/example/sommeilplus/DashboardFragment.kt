package com.example.sommeilplus

import android.os.Bundle
import android.graphics.Color
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val c = requireContext()
        val root = Ui.page(c, "Tableau de bord")
        val email = c.getSharedPreferences("app", 0).getString("email", "") ?: ""
        val sessions = SleepDatabase(c).sessions(email)
        val latest = sessions.firstOrNull()
        val target = c.getSharedPreferences("goals", 0).getFloat("hours", 8f)

        root.addView(Ui.card(c).apply {
            addView(Ui.text(c, "DERNIÈRE NUIT", 13f, Ui.MUTED, true))
            addView(Ui.text(c, "☾  ${"%.1f".format(latest?.hours ?: 0f)} h", 34f, Ui.BLUE, true))
            addView(Ui.text(c, "Durée de sommeil · ${latest?.date ?: "Aucune session enregistrée"}", 15f, Ui.MUTED))
            addView(Ui.text(c, "✦  Qualité : ${latest?.quality ?: 0}%", 17f, Ui.GREEN, true))
        })

        val stats = Ui.row(c)
        val avg = if (sessions.isEmpty()) 0f else sessions.take(7).map { it.hours }.average().toFloat()
        fun stat(label: String, value: String) = Ui.card(c).apply {
            addView(Ui.text(c, label, 12f, Ui.MUTED, true))
            addView(Ui.text(c, value, 23f, Color.WHITE, true))
        }
        val a = stat("MOYENNE", "${"%.1f".format(avg)} h")
        val b = stat("OBJECTIF", "${"%.1f".format(target)} h")
        a.layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = Ui.dp(c, 5) }
        b.layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = Ui.dp(c, 5) }
        stats.addView(a); stats.addView(b); root.addView(stats)

        root.addView(Ui.text(c, "Durée de sommeil · 7 derniers jours", 18f, Color.WHITE, true))
        val chartCard = Ui.card(c)
        val chart = LineChart(c)
        val recent = sessions.take(7).reversed()
        val entries = if (recent.isNotEmpty()) {
            recent.mapIndexed { i, session -> Entry(i.toFloat(), session.hours) }
        } else {
            // Le graphique reste visible dès la première ouverture, sans inventer de données.
            (0..6).map { Entry(it.toFloat(), 0f) }
        }
        val labels = if (recent.isNotEmpty()) recent.map { it.date.takeLast(5) }
            else listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
        chart.data = LineData(LineDataSet(entries, "Heures de sommeil").apply {
            color = Ui.BLUE
            setCircleColor(0xFF43D6FF.toInt())
            lineWidth = 3f
            circleRadius = 4f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = 0xFF087FB8.toInt()
            fillAlpha = 65
            mode = LineDataSet.Mode.CUBIC_BEZIER
        })
        styleChart(chart)
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.axisLeft.axisMaximum = 10f
        chart.axisLeft.axisMinimum = 0f
        chart.xAxis.axisMaximum = 6.4f
        chartCard.addView(chart, LinearLayout.LayoutParams(-1, Ui.dp(c, 235)))
        if (recent.isEmpty()) {
            chartCard.addView(Ui.text(c, "Aucune nuit enregistrée : le graphique se remplira après votre première session.", 14f, Ui.MUTED))
        }
        root.addView(chartCard)

        root.addView(Ui.card(c).apply {
            addView(Ui.text(c, "✦  Conseil du jour", 17f, Ui.BLUE, true))
            addView(Ui.text(c, "Gardez des horaires de coucher et de réveil réguliers.", 15f, Ui.NAVY))
        })
        root.addView(Ui.button(c, "+  Ajouter une session") {
            val hours = Ui.field(c, "Durée en heures (ex. 7.5)", 8194)
            val quality = Ui.field(c, "Qualité de 0 à 100", 2)
            val box = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL; setPadding(Ui.dp(c, 18), Ui.dp(c, 8), Ui.dp(c, 18), 0); addView(hours); addView(quality); setBackgroundColor(Ui.CARD) }
            val dialog = android.app.AlertDialog.Builder(c)
                .setTitle(Translator.text(c, "Nouvelle session"))
                .setView(box)
                .setPositiveButton(Translator.text(c, "Enregistrer"), null)
                .setNegativeButton(Translator.text(c, "Annuler"), null)
                .create()
            dialog.setOnShowListener {
                Ui.styleDialogButtons(dialog)
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val h = hours.text.toString().replace(',', '.').toFloatOrNull()
                    val q = quality.text.toString().toIntOrNull()
                    if (h != null && h in 0.1f..24f && q != null && q in 0..100) {
                        SleepDatabase(c).addSession(h, q, email)
                        dialog.dismiss()
                        parentFragmentManager.beginTransaction().replace(id, DashboardFragment()).commit()
                    } else {
                        Toast.makeText(c, Translator.text(c, "Valeurs invalides"), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            dialog.show()
        })
        return Ui.scroll(c, root)
    }

    private fun styleChart(chart: LineChart) {
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setBackgroundColor(Ui.CARD)
        chart.setNoDataText("Aucune donnée")
        chart.setNoDataTextColor(Color.WHITE)
        chart.setExtraOffsets(8f, 10f, 14f, 8f)
        chart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f
        chart.xAxis.textColor = Ui.MUTED
        chart.xAxis.textSize = 11f
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.textColor = Ui.MUTED
        chart.axisLeft.textSize = 11f
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.gridColor = Ui.BORDER
        chart.animateY(550)
        chart.invalidate()
    }
}
