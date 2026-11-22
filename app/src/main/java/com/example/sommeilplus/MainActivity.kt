package com.example.sommeilplus

import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var container: FrameLayout
    private lateinit var navItems: List<TextView>
    private var selected = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF07131D.toInt()
        window.navigationBarColor = 0xFF050D14.toInt()
        if (!getSharedPreferences("app", MODE_PRIVATE).getBoolean("loggedIn", false) ||
            getSharedPreferences("app", MODE_PRIVATE).getString("email", null) == null) {
            startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            layoutDirection = if (getSharedPreferences("app", MODE_PRIVATE).getString("language", "fr") == "ar") android.view.View.LAYOUT_DIRECTION_RTL else android.view.View.LAYOUT_DIRECTION_LTR
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 8))
            background = Ui.gradient(0xFF0B1B27.toInt(), 0xFF125A83.toInt(), 0f)
            minimumHeight = Ui.dp(this@MainActivity, 64)
        }
        toolbar.addView(Ui.logo(this, Ui.dp(this, 42)))
        toolbar.addView(Ui.text(this, "Sommeil Plus", 23f, Color.WHITE, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })

        container = FrameLayout(this).apply { id = android.view.View.generateViewId() }

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF0B1B27.toInt())
            elevation = 12f
            setPadding(Ui.dp(this@MainActivity, 4), Ui.dp(this@MainActivity, 5), Ui.dp(this@MainActivity, 4), Ui.dp(this@MainActivity, 5))
            minimumHeight = Ui.dp(this@MainActivity, 68)
        }
        val labels = listOf("Accueil", "Alarme", "Buts", "Conseils", "Profil", "Réglages")
        val icons = listOf("⌂", "◷", "♡", "✿", "♙", "⚙")
        navItems = labels.mapIndexed { index, label ->
            TextView(this).apply {
                text = icons[index] + "\n" + Translator.text(this@MainActivity, label)
                textSize = 9f
                maxLines = 2
                gravity = Gravity.CENTER
                includeFontPadding = false
                setLineSpacing(0f, 1f)
                setPadding(Ui.dp(this@MainActivity, 1), Ui.dp(this@MainActivity, 4), Ui.dp(this@MainActivity, 1), Ui.dp(this@MainActivity, 4))
                layoutParams = LinearLayout.LayoutParams(0, Ui.dp(this@MainActivity, 60), 1f)
                setOnClickListener { show(index) }
                nav.addView(this)
            }
        }

        root.addView(toolbar)
        root.addView(container, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(nav)
        setContentView(Ui.edgeToEdge(this, root) { keyboard -> nav.visibility = if (keyboard) android.view.View.GONE else android.view.View.VISIBLE })
        show(intent.getIntExtra("selected_tab", 0).coerceIn(0, 5))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        show(intent.getIntExtra("selected_tab", 0).coerceIn(0, 5))
    }

    fun goHomeFromHeader() {
        if (::navItems.isInitialized) show(0)
    }

    private fun show(index: Int) {
        selected = index
        navItems.forEachIndexed { i, item ->
            item.setTextColor(if (i == index) Ui.BLUE else Ui.MUTED)
            item.textSize = if (i == index) 9.5f else 9f
            item.setTypeface(null, if (i == index) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        val fragment = when (index) {
            1 -> AlarmFragment()
            2 -> GoalsFragment()
            3 -> AdviceFragment()
            4 -> AccountFragment()
            5 -> SettingsFragment()
            else -> DashboardFragment()
        }
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(container.id, fragment)
            .commit()
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (selected != 0) show(0) else super.onBackPressed()
    }
}
