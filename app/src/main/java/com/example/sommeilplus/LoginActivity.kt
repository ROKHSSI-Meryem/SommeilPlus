package com.example.sommeilplus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.widget.*

class LoginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val root = Ui.page(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        content.addView(Ui.space(this, Ui.dp(this, 24)))
        content.addView(Ui.logo(this, Ui.dp(this, 112)).apply {
            layoutParams = LinearLayout.LayoutParams(Ui.dp(this@LoginActivity, 112), Ui.dp(this@LoginActivity, 112)).apply { gravity = Gravity.CENTER }
        })
        content.addView(Ui.text(this, "Sommeil Plus", 30f, android.graphics.Color.WHITE, true).apply { gravity = Gravity.CENTER })
        content.addView(Ui.text(this, "Retrouvez le plaisir de bien dormir", 15f, Ui.MUTED).apply { gravity = Gravity.CENTER })
        content.addView(Ui.space(this, Ui.dp(this, 28)))
        val card = Ui.card(this)
        card.addView(Ui.text(this, "Bienvenue", 24f, android.graphics.Color.WHITE, true))
        card.addView(Ui.text(this, "Connectez-vous à votre espace", 15f, Ui.MUTED))
        card.addView(Ui.space(this, Ui.dp(this, 18)))
        val email = Ui.field(this, "Adresse e-mail", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val (passwordBox, pass) = Ui.passwordField(this, "Mot de passe")
        card.addView(Ui.text(this, "E-mail", 14f, Ui.MUTED, true)); card.addView(email); card.addView(Ui.emailShortcuts(this, email))
        card.addView(Ui.text(this, "Mot de passe", 14f, Ui.MUTED, true)); card.addView(passwordBox)
        card.addView(Ui.text(this, "Mot de passe oublié ?", 14f, Ui.BLUE, true).apply {
            gravity = Gravity.END; setPadding(0, Ui.dp(context, 8), 0, Ui.dp(context, 8))
            setOnClickListener { showForgotPassword(email.text.toString().trim()) }
        })
        content.addView(card)
        lateinit var loginButton: Button
        loginButton = Ui.button(this, "Se connecter") {
            doLogin(email.text.toString().trim(), pass.text.toString(), loginButton)
        }
        content.addView(loginButton)
        content.addView(Ui.text(this, "Vous n’avez pas encore de compte ?  Créer un compte", 14f, Ui.BLUE, true).apply {
            gravity = Gravity.CENTER; setPadding(0, Ui.dp(context, 12), 0, Ui.dp(context, 16))
            setOnClickListener { startActivity(Intent(this@LoginActivity, RegisterActivity::class.java)) }
        })
        root.addView(content, LinearLayout.LayoutParams(-1, -2))
        val scrollView = Ui.scroll(this, root).apply {
            isFillViewport = true
            descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
            clipToPadding = false
        }
        setContentView(Ui.edgeToEdge(this, scrollView))
    }

    /** Mot de passe oublie : demande l'e-mail, puis Firebase envoie un VRAI e-mail avec un lien de reinitialisation. */
    private fun showForgotPassword(prefill: String) {
        val input = Ui.field(this, "Adresse e-mail", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            .apply { setText(prefill) }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@LoginActivity, 20), Ui.dp(this@LoginActivity, 8), Ui.dp(this@LoginActivity, 20), 0)
            addView(input)
            addView(Ui.emailShortcuts(this@LoginActivity, input))
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(Translator.text(this, "Mot de passe oublié ?"))
            .setMessage(Translator.text(this, "Entrez votre adresse e-mail. Firebase vous enverra un lien sécurisé contenant un code à usage unique pour réinitialiser votre mot de passe."))
            .setView(box)
            .setPositiveButton(Translator.text(this, "Envoyer")) { _, _ -> sendReset(input.text.toString().trim()) }
            .setNegativeButton(Translator.text(this, "Annuler"), null)
            .show().also { dialog ->
                dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
    }

    private fun sendReset(mail: String) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) { toast("Adresse e-mail invalide"); return }
        if (!Cloud.ready(this)) {
            toast("Firebase n'est pas configuré : ajoutez google-services.json puis synchronisez le projet")
            return
        }
        toast("Envoi en cours…")
        Cloud.resetPassword(mail) { ok, error ->
            if (ok) {
                android.app.AlertDialog.Builder(this)
                    .setTitle(Translator.text(this, "E-mail envoyé ✉️"))
                    .setMessage(
                        Translator.text(this, "Si cette adresse correspond à un compte, un e-mail de réinitialisation vient d'être envoyé à ") + mail +
                            Translator.text(this, ". Ouvrez votre boîte mail (vérifiez aussi les Spams) puis cliquez sur le lien pour choisir un nouveau mot de passe.")
                    )
                    .setPositiveButton("OK", null)
                    .show()
            } else toast(error ?: "Erreur")
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, Translator.text(this, message), Toast.LENGTH_LONG).show()

    private fun openMain(mail: String) {
        getSharedPreferences("app", MODE_PRIVATE).edit()
            .putString("email", mail.trim().lowercase())
            .putBoolean("loggedIn", true)
            .remove("pendingEmail")
            .apply()
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }

    /** Recupere les donnees Firebase puis ouvre l'application (max. 5 s d'attente si le reseau est lent). */
    private fun syncThenOpen(mail: String) {
        var opened = false
        val go = { if (!opened) { opened = true; openMain(mail) } }
        Handler(Looper.getMainLooper()).postDelayed(Runnable { go() }, 5000)
        Cloud.syncAfterLogin(this, mail) { go() }
    }

    private fun doLogin(mail: String, password: String, button: Button) {
        if (!Cloud.ready(this)) {
            // Mode local (Firebase non configure) : comportement d'origine.
            val user = SleepDatabase(this).login(mail, password)
            if (user != null) openMain(user.email) else toast("Email ou mot de passe incorrect")
            return
        }
        if (mail.isBlank() || password.isBlank()) { toast("Remplissez tous les champs (mot de passe : 6 caractères minimum)"); return }
        button.isEnabled = false
        Cloud.login(mail, password) { ok, error ->
            if (ok) { syncThenOpen(mail); return@login }
            // Ancien compte cree avant Firebase : on le migre vers Firebase avec le meme mot de passe.
            val local = SleepDatabase(this).login(mail, password)
            if (local != null) {
                Cloud.register(this, local.name, local.email, password, local.birth) { ok2, error2 ->
                    if (ok2) syncThenOpen(mail) else { button.isEnabled = true; toast(error2 ?: error ?: "Erreur") }
                }
            } else {
                button.isEnabled = true
                toast(error ?: "Email ou mot de passe incorrect")
            }
        }
    }
}
