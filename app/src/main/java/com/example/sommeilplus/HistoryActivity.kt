package com.example.sommeilplus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.*

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = Ui.page(this, "Historique", showBack = true)
        val email = getSharedPreferences("app", MODE_PRIVATE).getString("email", "") ?: ""
        val sessions = SleepDatabase(this).sessions(email)
        sessions.forEach { s ->
            root.addView(Ui.card(this).apply {
                addView(Ui.text(this@HistoryActivity, "${s.date}  •  ${s.hours} h  •  Qualité ${s.quality}%", 15f, Ui.NAVY, true))
            })
        }
        if (sessions.isEmpty()) root.addView(Ui.text(this, "Aucune session enregistrée."))
        setContentView(Ui.edgeToEdge(this, Ui.withBottomNavigation(this, Ui.scroll(this, root), 4)))
    }
}
