package com.example.sommeilplus

import android.os.Bundle
import android.graphics.Color
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class AnalysisActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF07131D.toInt()
        window.navigationBarColor = 0xFF050D14.toInt()
        try {
            showAnalysis()
        } catch (e: Exception) {
            android.util.Log.e("SommeilPlus", "Erreur analyse sommeil", e)
            val fallback = Ui.page(this, "Analyse du sommeil", showBack = true)
            fallback.addView(Ui.card(this).apply {
                addView(Ui.text(this@AnalysisActivity, "Impossible de charger les graphiques pour le moment.", 15f, Ui.NAVY, true))
                addView(Ui.text(this@AnalysisActivity, e.localizedMessage ?: "Erreur inconnue", 12f, Ui.MUTED))
            })
            setContentView(Ui.edgeToEdge(this, Ui.withBottomNavigation(this, Ui.scroll(this, fallback), 4)))
        }
    }

    private fun showAnalysis() {
        val root = Ui.page(this, "Analyse du sommeil", showBack = true)
        val email = getSharedPreferences("app", MODE_PRIVATE).getString("email", "") ?: ""
        val sessions = SleepDatabase(this).sessions(email).reversed()
        val avg = if (sessions.isEmpty()) 0f else sessions.map { it.hours }.average().toFloat()
        val avgQuality = if (sessions.isEmpty()) 0 else sessions.map { it.quality }.average().toInt()
        val best = sessions.maxByOrNull { it.hours }
        val stats = Ui.row(this)
        listOf("MOYENNE" to "%.1f h".format(avg), "QUALITÉ" to "$avgQuality%", "MEILLEURE NUIT" to "${best?.hours ?: 0f} h").forEach { pair ->
            val card = Ui.card(this).apply {
                addView(Ui.text(this@AnalysisActivity, pair.first, 9f, Ui.MUTED, true))
                addView(Ui.text(this@AnalysisActivity, pair.second, 15f, Color.WHITE, true))
            }
            card.layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(3, 3, 3, 3) }
            stats.addView(card)
        }
        root.addView(stats)

        root.addView(Ui.text(this, "Qualité de sommeil", 15f, Color.WHITE, true))
        val qualityChart = BarChart(this)
        if (sessions.isNotEmpty()) {
            val qEntries = sessions.mapIndexed { i, s -> BarEntry(i.toFloat(), s.quality.toFloat()) }
            qualityChart.data = BarData(BarDataSet(qEntries, "Qualité (%)").apply {
                color = Ui.BLUE; valueTextColor = Color.WHITE; valueTextSize = 9f
            }).apply { barWidth = 0.48f }
            styleBar(qualityChart)
            qualityChart.axisLeft.axisMaximum = 100f
            qualityChart.xAxis.valueFormatter = IndexAxisValueFormatter(sessions.map { it.date.takeLast(5) })
            qualityChart.animateY(500)
        } else {
            qualityChart.visibility = android.view.View.GONE
            root.addView(Ui.card(this).apply { addView(Ui.text(this@AnalysisActivity, "Les graphiques apparaîtront après vos premières nuits enregistrées.", 12f, Ui.MUTED)) })
        }
        if (sessions.isNotEmpty()) root.addView(qualityChart, LinearLayout.LayoutParams(-1, 205))

        root.addView(Ui.text(this, "Durée de sommeil", 15f, Color.WHITE, true))
        if (sessions.isNotEmpty()) {
            val durationChart = LineChart(this)
            val entries = sessions.mapIndexed { i, s -> Entry(i.toFloat(), s.hours) }
            durationChart.data = LineData(LineDataSet(entries, "Heures").apply {
                color = Ui.BLUE; setCircleColor(0xFF42D5FF.toInt()); lineWidth = 2.5f; circleRadius = 3.5f
                setDrawValues(false); setDrawFilled(true); fillColor = 0xFF087FB8.toInt(); fillAlpha = 55
            })
            durationChart.description.isEnabled = false; durationChart.legend.isEnabled = false
            durationChart.axisRight.isEnabled = false; durationChart.setBackgroundColor(Ui.CARD)
            durationChart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            durationChart.xAxis.granularity = 1f; durationChart.xAxis.textColor = Ui.MUTED
            durationChart.xAxis.textSize = 9f; durationChart.xAxis.setDrawGridLines(false)
            durationChart.xAxis.valueFormatter = IndexAxisValueFormatter(sessions.map { it.date.takeLast(5) })
            durationChart.axisLeft.textColor = Ui.MUTED; durationChart.axisLeft.axisMinimum = 0f
            durationChart.axisLeft.gridColor = Ui.BORDER; durationChart.animateX(500)
            root.addView(durationChart, LinearLayout.LayoutParams(-1, 205))
        }
        root.addView(Ui.card(this).apply {
            addView(Ui.text(this@AnalysisActivity, "Analyse", 13f, Ui.BLUE, true))
            addView(Ui.text(this@AnalysisActivity, "${sessions.size} nuits analysées · qualité moyenne $avgQuality% · durée moyenne %.1f heures".format(avg), 12f, Ui.NAVY))
        })
        setContentView(Ui.edgeToEdge(this, Ui.withBottomNavigation(this, Ui.scroll(this, root), 4)))
    }

    private fun styleBar(chart: BarChart) {
        chart.description.isEnabled = false; chart.legend.isEnabled = false; chart.axisRight.isEnabled = false
        chart.setDrawGridBackground(false); chart.setBackgroundColor(Ui.CARD); chart.setFitBars(true)
        chart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f; chart.xAxis.textColor = Ui.MUTED; chart.xAxis.textSize = 9f
        chart.xAxis.setDrawGridLines(false); chart.axisLeft.textColor = Ui.MUTED
        chart.axisLeft.axisMinimum = 0f; chart.axisLeft.gridColor = Ui.BORDER
    }
}
