package com.example.sommeilplus

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : Activity() {
    private lateinit var pager: ViewPager2
    private lateinit var dots: TextView
    private lateinit var next: Button
    private lateinit var back: TextView

    private val pages = listOf(
        Page("Pourquoi suivre son sommeil ?", "Comprenez vos habitudes, repérez ce qui perturbe vos nuits et améliorez progressivement votre récupération.", R.drawable.onboarding_sleep, "Un sommeil régulier aide le corps et l’esprit à récupérer."),
        Page("Comment suivre votre sommeil ?", "Choisissez le suivi manuel en ajoutant vos heures de coucher et de réveil, ou le suivi automatique lorsqu’un capteur compatible est disponible.", R.drawable.onboarding_tracking, "Vos nuits sont réunies dans un historique simple à consulter."),
        Page("Rappels & objectifs personnalisés", "Définissez votre durée de sommeil idéale et vos horaires. Recevez des rappels pour garder une routine adaptée à votre quotidien.", R.drawable.onboarding_wakeup, "Avancez à votre rythme, nuit après nuit.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@OnboardingActivity, 22), Ui.dp(this@OnboardingActivity, 22), Ui.dp(this@OnboardingActivity, 22), Ui.dp(this@OnboardingActivity, 20))
            setBackgroundColor(Ui.BG)
        }
        root.addView(Ui.logo(this, Ui.dp(this, 76)).apply {
            layoutParams = LinearLayout.LayoutParams(Ui.dp(this@OnboardingActivity, 76), Ui.dp(this@OnboardingActivity, 76)).apply { gravity = Gravity.CENTER }
        })
        root.addView(Ui.text(this, "SOMMEIL PLUS", 20f, Ui.BLUE, true).apply { gravity = Gravity.CENTER; letterSpacing = .08f })
        root.addView(Ui.text(this, "Prenez soin de vos nuits", 14f, Ui.MUTED).apply { gravity = Gravity.CENTER })
        pager = ViewPager2(this).apply {
            adapter = OnboardingAdapter()
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            offscreenPageLimit = 2
        }
        root.addView(pager)
        dots = Ui.text(this, "●   ○   ○", 18f, Ui.BLUE, true).apply { gravity = Gravity.CENTER; setPadding(0, Ui.dp(this@OnboardingActivity, 8), 0, Ui.dp(this@OnboardingActivity, 8)) }
        root.addView(dots)
        val controls = Ui.row(this)
        back = Ui.text(this, "Précédent", 16f, Ui.MUTED, true).apply {
            gravity = Gravity.CENTER
            setPadding(Ui.dp(this@OnboardingActivity, 12), Ui.dp(this@OnboardingActivity, 14), Ui.dp(this@OnboardingActivity, 12), Ui.dp(this@OnboardingActivity, 14))
            background = Ui.gradient(0xFF102B3A.toInt(), 0xFF0B1D2A.toInt(), Ui.dp(this@OnboardingActivity, 18).toFloat()).apply {
                setStroke(Ui.dp(this@OnboardingActivity, 1), Ui.BORDER)
            }
            setOnClickListener { if (pager.currentItem > 0) pager.currentItem-- }
        }
        controls.addView(back, LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1f))
        next = Ui.button(this, "Suivant") { advance() }
        controls.addView(next, LinearLayout.LayoutParams(0, Ui.dp(this, 54), 2f))
        root.addView(controls)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.text = (0..2).joinToString("   ") { if (it == position) "●" else "○" }
                next.text = Translator.text(this@OnboardingActivity, if (position == 2) "Commencer" else "Suivant")
                back.visibility = if (position == 0) android.view.View.INVISIBLE else android.view.View.VISIBLE
            }
        })
        setContentView(Ui.edgeToEdge(this, root))
    }

    private fun advance() {
        if (pager.currentItem < pages.lastIndex) {
            pager.currentItem++
            return
        }

        // L'onboarding est termine : il ne sera plus affiche apres inscription,
        // connexion ou deconnexion.
        getSharedPreferences("app", MODE_PRIVATE).edit()
            .putBoolean("onboarded", true)
            .putBoolean("loggedIn", false)
            .remove("pendingEmail")
            .apply()

        startActivity(
            Intent(this, LoginActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
    }

    private inner class OnboardingAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingHolder>() {
        override fun getItemCount() = pages.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingHolder {
            val scroll = ScrollView(this@OnboardingActivity).apply {
                // ViewPager2 exige que chaque page remplisse toute sa surface.
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isFillViewport = true
                clipToPadding = false
                overScrollMode = android.view.View.OVER_SCROLL_NEVER
            }
            return OnboardingHolder(scroll)
        }
        override fun onBindViewHolder(holder: OnboardingHolder, position: Int) {
            val p = pages[position]
            val box = LinearLayout(this@OnboardingActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, Ui.dp(context, 20), 0, Ui.dp(context, 24))
            }
            holder.container.removeAllViews()
            holder.container.addView(box)
            val imageCard = FrameLayout(this@OnboardingActivity).apply {
                background = Ui.gradient(0xFF102B3C.toInt(), 0xFF081A29.toInt(), Ui.dp(context, 26).toFloat())
                layoutParams = LinearLayout.LayoutParams(-1, Ui.dp(context, 245))
                clipToOutline = true
            }
            val illustration = ImageView(this@OnboardingActivity).apply {
                setImageResource(p.imageRes)
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = p.title
                // L'image remplit toute la carte, sans marge ni padding intérieur.
                setPadding(0, 0, 0, 0)
                layoutParams = FrameLayout.LayoutParams(-1, -1, Gravity.CENTER)
            }
            imageCard.addView(illustration)
            box.addView(imageCard)
            box.addView(Ui.space(this@OnboardingActivity, Ui.dp(this@OnboardingActivity, 28)))
            box.addView(Ui.text(this@OnboardingActivity, p.title, 25f, Color.WHITE, true).apply { gravity = Gravity.CENTER; setPadding(0, 0, 0, Ui.dp(context, 12)) })
            box.addView(Ui.text(this@OnboardingActivity, p.description, 17f, Ui.MUTED).apply { gravity = Gravity.CENTER; setLineSpacing(Ui.dp(context, 4).toFloat(), 1f) })
            val note = Ui.card(this@OnboardingActivity)
            note.addView(Ui.text(this@OnboardingActivity, "✦  À retenir", 15f, Ui.BLUE, true))
            note.addView(Ui.text(this@OnboardingActivity, p.note, 15f, Color.WHITE))
            val params = note.layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(-1, -2)
            params.topMargin = Ui.dp(this@OnboardingActivity, 22); note.layoutParams = params
            box.addView(note)
        }
    }
    private class OnboardingHolder(val container: ScrollView) : androidx.recyclerview.widget.RecyclerView.ViewHolder(container)
    private data class Page(val title: String, val description: String, val imageRes: Int, val note: String)
}
