package com.example.sommeilplus

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*

class SplashActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val loops = mutableListOf<Animator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val frame = FrameLayout(this).apply {
            background = Ui.gradient(0xFF07131D.toInt(), 0xFF125A83.toInt(), 0f)
        }

        // Etoiles qui scintillent (ambiance nuit)
        val stars = List(16) {
            TextView(this).apply {
                text = "✦"
                setTextColor(Color.WHITE)
                textSize = (8 + (it % 4) * 3).toFloat()
                alpha = 0f
            }
        }
        stars.forEach { frame.addView(it, FrameLayout.LayoutParams(-2, -2)) }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(Ui.dp(this@SplashActivity, 24), Ui.dp(this@SplashActivity, 24), Ui.dp(this@SplashActivity, 24), Ui.dp(this@SplashActivity, 24))
        }
        val logo = Ui.logo(this, Ui.dp(this, 164)).apply {
            layoutParams = LinearLayout.LayoutParams(Ui.dp(this@SplashActivity, 164), Ui.dp(this@SplashActivity, 164)).apply { gravity = Gravity.CENTER }
        }
        val title = TextView(this).apply {
            text = "Sommeil Plus"
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val tagline = TextView(this).apply {
            text = Translator.text(this@SplashActivity, "Dormez mieux,\nVivez mieux")
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 28)
        }
        val progress = ProgressBar(this).apply {
            indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
        }
        column.addView(logo); column.addView(title); column.addView(tagline); column.addView(progress)
        frame.addView(column, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)

        // Etat de depart des animations
        logo.alpha = 0f; logo.scaleX = 0.4f; logo.scaleY = 0.4f; logo.rotation = -12f
        title.alpha = 0f; title.translationY = Ui.dp(this, 36).toFloat()
        tagline.alpha = 0f; tagline.translationY = Ui.dp(this, 24).toFloat()
        progress.alpha = 0f

        // 1) Le logo apparait en grossissant avec un petit rebond, puis "respire" doucement
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(0f)
            .setDuration(900).setInterpolator(OvershootInterpolator(1.3f))
            .withEndAction {
                val pulse = ObjectAnimator.ofPropertyValuesHolder(
                    logo,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.06f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.06f)
                ).apply {
                    duration = 1300
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                }
                loops.add(pulse); pulse.start()
            }.start()

        // 2) Titre puis slogan glissent vers le haut en fondu
        title.animate().alpha(1f).translationY(0f).setStartDelay(450).setDuration(700)
            .setInterpolator(DecelerateInterpolator()).start()
        tagline.animate().alpha(1f).translationY(0f).setStartDelay(800).setDuration(700)
            .setInterpolator(DecelerateInterpolator()).start()
        progress.animate().alpha(1f).setStartDelay(1100).setDuration(500).start()

        // 3) Les etoiles scintillent a des positions aleatoires
        frame.post {
            val rnd = java.util.Random()
            val w = (frame.width - Ui.dp(this, 30)).coerceAtLeast(1)
            val h = (frame.height - Ui.dp(this, 30)).coerceAtLeast(1)
            stars.forEach { star ->
                star.x = rnd.nextInt(w).toFloat()
                star.y = rnd.nextInt(h).toFloat()
                val twinkle = ObjectAnimator.ofFloat(star, View.ALPHA, 0f, 0.9f).apply {
                    duration = 1100L + rnd.nextInt(1300)
                    startDelay = rnd.nextInt(1000).toLong()
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                }
                loops.add(twinkle); twinkle.start()
            }
        }

        handler.postDelayed({
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            // Ne jamais confondre une adresse enregistree avec une session ouverte.
            val next = if (prefs.getBoolean("loggedIn", false) && prefs.getString("email", null) != null) {
                MainActivity::class.java
            } else if (!prefs.getBoolean("onboarded", false)) {
                // Premiere ouverture uniquement : Splash > Onboarding > Login.
                OnboardingActivity::class.java
            } else {
                LoginActivity::class.java
            }
            startActivity(Intent(this, next))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2800)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        loops.forEach { it.cancel() }
        loops.clear()
        super.onDestroy()
    }
}
