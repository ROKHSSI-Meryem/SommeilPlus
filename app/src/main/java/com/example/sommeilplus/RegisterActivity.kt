package com.example.sommeilplus

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import android.view.WindowManager
import java.util.Calendar

class RegisterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val root = Ui.page(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, Ui.dp(this@RegisterActivity, 8), 0, Ui.dp(this@RegisterActivity, 8)) }
        body.addView(Ui.space(this, 12))
        body.addView(Ui.logo(this, Ui.dp(this, 100)).apply {
            layoutParams = LinearLayout.LayoutParams(Ui.dp(this@RegisterActivity, 100), Ui.dp(this@RegisterActivity, 100)).apply { gravity = android.view.Gravity.CENTER }
        })
        body.addView(Ui.text(this, "Sommeil Plus", 27f, Ui.BLUE, true).apply { gravity = android.view.Gravity.CENTER })
        body.addView(Ui.text(this, "Créez votre espace personnel", 14f, Ui.MUTED).apply {
            gravity = android.view.Gravity.CENTER
        })

        val form = Ui.card(this)
        val name = Ui.field(this, "Nom d'utilisateur")
        val email = Ui.field(this, "Adresse e-mail", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val (passwordBox, pass) = Ui.passwordField(this, "Mot de passe (6 caractères minimum)")
        val birth = Ui.field(this, "Date de naissance (AAAA-MM-JJ)")
        birth.isFocusable = false
        birth.setOnClickListener {
            // La date maximale selectable est exactement le jour des 18 ans.
            val limit = Calendar.getInstance().apply {
                add(Calendar.YEAR, -18)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            DatePickerDialog(this, { _, y, m, d ->
                birth.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
            }, limit.get(Calendar.YEAR), limit.get(Calendar.MONTH), limit.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.maxDate = limit.timeInMillis
            }.show()
        }
        form.addView(name); form.addView(email); form.addView(Ui.emailShortcuts(this, email)); form.addView(passwordBox); form.addView(birth)
        form.addView(Ui.space(this, 12))
        body.addView(form)
        root.addView(body)
        var busy = false
        root.addView(Ui.button(this, "S'inscrire") {
            if (busy) return@button
            val n = name.text.toString().trim()
            val e = email.text.toString().trim()
            val p = pass.text.toString()
            val b = birth.text.toString()
            if (n.isBlank() || e.isBlank() || p.length < 6 || b.isBlank()) {
                Toast.makeText(this, Translator.text(this, "Remplissez tous les champs (mot de passe : 6 caractères minimum)"), Toast.LENGTH_LONG).show()
                return@button
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
                Toast.makeText(this, Translator.text(this, "Adresse e-mail invalide"), Toast.LENGTH_LONG).show()
                return@button
            }
            if (!isAdult(b)) {
                Toast.makeText(this, Translator.text(this, "Vous devez avoir au moins 18 ans"), Toast.LENGTH_LONG).show()
                return@button
            }
            val proceed = {
                getSharedPreferences("app", MODE_PRIVATE).edit()
                    .putString("email", e.lowercase())
                    .remove("pendingEmail")
                    .putBoolean("loggedIn", true)
                    .putBoolean("onboarded", true)
                    .apply()
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
            if (Cloud.ready(this)) {
                busy = true
                Cloud.register(this, n, e, p, b) { ok, error ->
                    if (ok) proceed() else {
                        busy = false
                        Toast.makeText(this, Translator.text(this, error ?: "Erreur"), Toast.LENGTH_LONG).show()
                    }
                }
            } else if (SleepDatabase(this).register(n, e, p, b)) {
                proceed()
            } else Toast.makeText(this, Translator.text(this, "Cet email existe déjà ou les données sont invalides"), Toast.LENGTH_LONG).show()
        })
        root.addView(Ui.text(this, "Déjà un compte ? Se connecter", 14f, Ui.BLUE, true).apply {
            gravity = android.view.Gravity.CENTER
            setPadding(0, Ui.dp(context, 10), 0, Ui.dp(context, 8))
            setOnClickListener { startActivity(Intent(this@RegisterActivity, LoginActivity::class.java)); finish() }
        })
        val scrollView = Ui.scroll(this, root).apply {
            isFillViewport = true
            descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
            clipToPadding = false
        }
        setContentView(Ui.edgeToEdge(this, scrollView))
    }

    private fun isAdult(value: String): Boolean {
        return try {
            val parts = value.split("-").map { it.toInt() }
            if (parts.size != 3) {
                false
            } else {
                val dob = Calendar.getInstance().apply {
                    isLenient = false
                    clear()
                    set(parts[0], parts[1] - 1, parts[2], 0, 0, 0)
                }
                // Comparaison par date civile (pas par heure) : 18 ans le jour meme = accepte.
                val limit = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -18)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                !dob.after(limit)
            }
        } catch (_: Exception) {
            false
        }
    }
}
