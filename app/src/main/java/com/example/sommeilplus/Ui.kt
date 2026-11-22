package com.example.sommeilplus

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object Ui {
    const val BLUE = 0xFF16B9F2.toInt()
    const val BLUE_LIGHT = 0xFF087FB8.toInt()
    const val NAVY = 0xFFEAF5FC.toInt()
    const val BG = 0xFF07131D.toInt()
    const val GREEN = 0xFF39D98A.toInt()
    const val MUTED = 0xFFB0C3D0.toInt()
    const val PURPLE = 0xFF8BA8FF.toInt()
    const val BORDER = 0xFF294657.toInt()
    const val CARD = 0xFF102330.toInt()
    const val ORANGE = 0xFFFF8A00.toInt()

    fun dp(c: Context, value: Int): Int = (value * c.resources.displayMetrics.density + 0.5f).toInt()

    fun page(c: Context, title: String? = null, showBack: Boolean = false): LinearLayout {
        val root = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 22))
            setBackgroundColor(BG)
            layoutDirection = if (c.getSharedPreferences("app", Context.MODE_PRIVATE).getString("language", "fr") == "ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }
        if (title != null) {
            val header = LinearLayout(c).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(c, 15), dp(c, 12), dp(c, 15), dp(c, 12))
                background = gradient(0xFF142D3D.toInt(), 0xFF0C1D2A.toInt(), dp(c, 13).toFloat())
                minimumHeight = dp(c, 54)
            }
            // Le logo reste uniquement dans la barre principale, à côté de « Sommeil Plus ».
            // Les en-têtes des pages secondaires affichent seulement le nom de la page.
            header.addView(text(c, Translator.text(c, title), 19f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            header.addView(text(c, "✦", 17f, BLUE))
            root.addView(header, LinearLayout.LayoutParams(-1, -2))
            root.addView(space(c, dp(c, 11)))
        }
        // Les écrans secondaires affichent leur propre bouton de retour.
        // finish() ramène exactement à l'écran d'origine (Alarme, Profil, etc.).
        if (showBack) {
            // Même style que le bouton « Précédent » de l'onboarding.
            val returnButton = TextView(c).apply {
                text = Translator.text(c, "Précédent")
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(BLUE)
                gravity = Gravity.CENTER
                setPadding(dp(c, 18), dp(c, 10), dp(c, 18), dp(c, 10))
                background = gradient(0xFF102B3A.toInt(), 0xFF0B1D2A.toInt(), dp(c, 18).toFloat()).apply {
                    setStroke(dp(c, 1), BORDER)
                }
                elevation = dp(c, 2).toFloat()
                contentDescription = Translator.text(c, "Précédent")
                setOnClickListener { if (c is Activity) c.finish() }
                layoutParams = LinearLayout.LayoutParams(-1, dp(c, 48)).apply {
                    setMargins(0, 0, 0, dp(c, 10))
                }
                tag = "sommeilplus_fixed_back_button"
            }
            root.addView(returnButton)
        }
        return root
    }

    // Bouton de retour commun, identique au style « Précédent » de l'onboarding.
    fun backArrow(c: Context): TextView = TextView(c).apply {
        text = Translator.text(c, "Précédent")
        textSize = 15f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(BLUE)
        gravity = Gravity.CENTER
        setPadding(dp(c, 18), dp(c, 10), dp(c, 18), dp(c, 10))
        background = gradient(0xFF102B3A.toInt(), 0xFF0B1D2A.toInt(), dp(c, 18).toFloat()).apply {
            setStroke(dp(c, 1), BORDER)
        }
        elevation = dp(c, 2).toFloat()
        setOnClickListener { if (c is Activity) c.finish() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(c, 48)).apply {
            setMargins(0, 0, 0, dp(c, 10))
        }
    }

    fun logo(c: Context, size: Int): ImageView = ImageView(c).apply {
        setImageResource(R.drawable.sommeil_plus_logo)
        scaleType = ImageView.ScaleType.FIT_CENTER
        layoutParams = LinearLayout.LayoutParams(size, size).apply {
            marginEnd = dp(c, 8)
        }
        contentDescription = "Logo Sommeil Plus"
    }

    fun text(c: Context, s: String, size: Float = 17f, color: Int = NAVY, bold: Boolean = false): TextView =
        TextView(c).apply {
            text = Translator.text(c, s)
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
            setPadding(dp(c, 5), dp(c, 5), dp(c, 5), dp(c, 5))
            includeFontPadding = true
            letterSpacing = 0.005f
        }

    // Tous les boutons communs s'adaptent à la longueur du texte et à la langue.
    // La hauteur est WRAP_CONTENT : les libellés longs passent sur plusieurs lignes.
    private fun Button.fitLabel(c: Context, size: Float, horizontalPadding: Int = 12) {
        isAllCaps = false
        textSize = size
        letterSpacing = 0f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        textAlignment = View.TEXT_ALIGNMENT_CENTER
        includeFontPadding = true
        setSingleLine(false)
        maxLines = 4
        setHorizontallyScrolling(false)
        ellipsize = null
        minWidth = 0
        minimumWidth = 0
        minHeight = dp(c, 50)
        minimumHeight = dp(c, 50)
        setPadding(dp(c, horizontalPadding), dp(c, 11), dp(c, horizontalPadding), dp(c, 11))
    }

    fun button(c: Context, s: String, action: () -> Unit): Button =
        Button(c).apply {
            text = Translator.text(c, s)
            fitLabel(c, 16f, 12)
            setTextColor(Color.WHITE)
            background = gradient(0xFF087FB8.toInt(), 0xFF16B9F2.toInt(), dp(c, 20).toFloat()).apply {
                setStroke(dp(c, 1), 0xFF49C9F5.toInt())
            }
            elevation = dp(c, 2).toFloat()
            stateListAnimator = null
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, dp(c, 7), 0, dp(c, 7))
            }
        }

    fun orangeButton(c: Context, s: String, action: () -> Unit): Button =
        Button(c).apply {
            text = Translator.text(c, s)
            fitLabel(c, 16f, 12)
            setTextColor(Color.WHITE)
            background = gradient(0xFF087FB8.toInt(), 0xFF16B9F2.toInt(), dp(c, 20).toFloat()).apply {
                setStroke(dp(c, 1), 0xFF49C9F5.toInt())
            }
            elevation = dp(c, 2).toFloat()
            stateListAnimator = null
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, dp(c, 7), 0, dp(c, 7))
            }
        }

    fun secondaryButton(c: Context, s: String, action: () -> Unit): Button = Button(c).apply {
        text = Translator.text(c, s)
        fitLabel(c, 15f, 12)
        setTextColor(BLUE)
        background = gradient(0xFF102B3A.toInt(), 0xFF0B1D2A.toInt(), dp(c, 18).toFloat()).apply {
            setStroke(dp(c, 1), BORDER)
        }
        stateListAnimator = null
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, dp(c, 6), 0, dp(c, 6))
        }
    }

    fun styleDialogButtons(dialog: android.app.AlertDialog) {
        val positive = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
        positive?.apply {
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            isAllCaps = false
            setSingleLine(false)
            maxLines = 2
            setHorizontallyScrolling(false)
            minWidth = 0
            minimumWidth = 0
            textSize = 13f
            gravity = Gravity.CENTER
            background = gradient(0xFF0A78B0.toInt(), 0xFF16B9F2.toInt(), dp(context, 14).toFloat())
            setPadding(dp(context, 9), dp(context, 7), dp(context, 9), dp(context, 7))
        }
        val negative = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
        negative?.apply {
            val label = text?.toString().orEmpty()
            val isCancel = label.contains("Annuler", ignoreCase = true) ||
                label.contains("Cancel", ignoreCase = true) ||
                label.contains("إلغاء", ignoreCase = true)
            val isContinueTracking = label.contains("Continuer le suivi", ignoreCase = true) ||
                label.contains("Continue tracking", ignoreCase = true) ||
                label.contains("متابعة التتبع", ignoreCase = true)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            isAllCaps = false
            setSingleLine(false)
            maxLines = 2
            setHorizontallyScrolling(false)
            minWidth = 0
            minimumWidth = 0
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(if (isCancel || isContinueTracking) Color.WHITE else BLUE)
            background = when {
                isCancel -> gradient(0xFFB42318.toInt(), 0xFFDC3545.toInt(), dp(context, 14).toFloat())
                isContinueTracking -> gradient(0xFF087FB8.toInt(), 0xFF16B9F2.toInt(), dp(context, 14).toFloat()).apply {
                    setStroke(dp(context, 1), 0xFF49C9F5.toInt())
                }
                else -> gradient(0xFF102B3A.toInt(), 0xFF0B1D2A.toInt(), dp(context, 14).toFloat()).apply {
                    setStroke(dp(context, 1), BORDER)
                }
            }
            setPadding(dp(context, 12), dp(context, 9), dp(context, 12), dp(context, 9))
        }
    }

    // Champ mot de passe avec bouton œil : masqué par défaut, affichage/masquage au toucher.
    fun passwordField(c: Context, hint: String): Pair<LinearLayout, EditText> {
        val password = field(
            c,
            hint,
            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        )
        val wrapper = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = gradient(0xFF0B1B27.toInt(), 0xFF0B1B27.toInt(), dp(c, 11).toFloat()).apply {
                setStroke(dp(c, 1), BORDER)
            }
            layoutParams = LinearLayout.LayoutParams(-1, dp(c, 58)).apply {
                setMargins(0, dp(c, 6), 0, dp(c, 8))
            }
        }
        password.background = null
        password.layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
        password.setPadding(dp(c, 14), dp(c, 10), dp(c, 6), dp(c, 10))
        wrapper.addView(password)

        val eye = ImageButton(c).apply {
            setImageResource(R.drawable.ic_visibility_off)
            setColorFilter(MUTED)
            background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            scaleType = ImageView.ScaleType.CENTER
            contentDescription = Translator.text(c, "Afficher le mot de passe")
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dp(c, 48), -1)
            setPadding(dp(c, 10), dp(c, 10), dp(c, 10), dp(c, 10))
        }
        var visible = false
        eye.setOnClickListener {
            val cursor = password.selectionStart.coerceAtLeast(0)
            visible = !visible
            password.inputType = if (visible) {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            password.setSelection(cursor.coerceAtMost(password.text.length))
            eye.setImageResource(if (visible) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
            eye.contentDescription = Translator.text(c, if (visible) "Masquer le mot de passe" else "Afficher le mot de passe")
        }
        wrapper.addView(eye)
        return Pair(wrapper, password)
    }

    fun field(c: Context, hint: String, type: Int = android.text.InputType.TYPE_CLASS_TEXT): EditText =
        EditText(c).apply {
            this.hint = Translator.text(c, hint)
            inputType = type
            setSingleLine(true)
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(MUTED)
            setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10))
            background = gradient(0xFF0B1B27.toInt(), 0xFF0B1B27.toInt(), dp(c, 11).toFloat()).apply { setStroke(dp(c, 1), BORDER) }
            minHeight = dp(c, 52)
            layoutParams = LinearLayout.LayoutParams(-1, dp(c, 54)).apply { setMargins(0, dp(c, 6), 0, dp(c, 8)) }
        }

    fun card(c: Context): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12))
        background = gradient(CARD, 0xFF0C1D29.toInt(), dp(c, 12).toFloat()).apply { setStroke(dp(c, 1), BORDER) }
        elevation = dp(c, 1).toFloat()
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(c, 6), 0, dp(c, 7)) }
    }

    fun space(c: Context, h: Int = dp(c, 10)): View = View(c).apply { layoutParams = LinearLayout.LayoutParams(1, h) }
    fun scroll(c: Context, child: View): ScrollView = ScrollView(c).apply {
        setBackgroundColor(BG)
        addView(child)
        isFillViewport = true
        clipToPadding = false
        overScrollMode = View.OVER_SCROLL_NEVER
    }
    fun row(c: Context) = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

    fun withBottomNavigation(c: Context, content: View, selected: Int = -1): LinearLayout {
        val screen = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
        }
        // Le bouton « Précédent » est extrait de la page défilante et placé
        // dans une zone fixe, juste au-dessus de la barre de navigation.
        fun extractBackButton(view: View): View? {
            if (view.tag == "sommeilplus_fixed_back_button") {
                (view.parent as? android.view.ViewGroup)?.removeView(view)
                return view
            }
            if (view is android.view.ViewGroup) {
                for (i in 0 until view.childCount) {
                    val found = extractBackButton(view.getChildAt(i))
                    if (found != null) return found
                }
            }
            return null
        }
        val backButton = extractBackButton(content)
        screen.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        if (backButton != null) {
            val backArea = LinearLayout(c).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(c, 16), dp(c, 5), dp(c, 16), dp(c, 5))
                setBackgroundColor(BG)
            }
            backButton.layoutParams = LinearLayout.LayoutParams(-1, dp(c, 48)).apply {
                setMargins(0, 0, 0, 0)
            }
            backArea.addView(backButton)
            screen.addView(backArea, LinearLayout.LayoutParams(-1, -2))
        }
        val nav = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF0B1B27.toInt())
            elevation = dp(c, 8).toFloat()
            setPadding(dp(c, 3), dp(c, 4), dp(c, 3), dp(c, 4))
        }
        val labels = listOf("Accueil", "Alarme", "Buts", "Conseils", "Profil", "Réglages")
        val icons = listOf("⌂", "◷", "♡", "✿", "♙", "⚙")
        labels.forEachIndexed { index, label ->
            val item = TextView(c).apply {
                text = icons[index] + "\n" + Translator.text(c, label)
                textSize = 9f
                maxLines = 2
                gravity = Gravity.CENTER
                includeFontPadding = false
                setLineSpacing(0f, 1f)
                setTextColor(if (index == selected) BLUE else MUTED)
                setPadding(dp(c, 1), dp(c, 3), dp(c, 1), dp(c, 3))
                background = if (index == selected)
                    gradient(0xFF12384C.toInt(), 0xFF0D2636.toInt(), dp(c, 14).toFloat()).apply { setStroke(dp(c, 1), 0xFF1D5872.toInt()) }
                else null
                layoutParams = LinearLayout.LayoutParams(0, dp(c, 58), 1f)
                setOnClickListener {
                    val intent = android.content.Intent(c, MainActivity::class.java)
                        .putExtra("selected_tab", index)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    c.startActivity(intent)
                    if (c is android.app.Activity && c !is MainActivity) c.finish()
                }
            }
            nav.addView(item)
        }
        screen.addView(nav)
        return screen
    }

    /**
     * Ecran "edge-to-edge" sur : les barres systeme ET le clavier sont pris en compte.
     * Le contenu (ScrollView) rapetisse quand le clavier s'ouvre, donc tout reste atteignable
     * en defilant, et le champ en cours de saisie est ramene a l'ecran.
     * [onImeChanged] est appele quand le clavier apparait / disparait.
     */
    fun edgeToEdge(activity: Activity, content: View, onImeChanged: ((Boolean) -> Unit)? = null): FrameLayout {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        val frame = FrameLayout(activity).apply {
            setBackgroundColor(BG)
            addView(content, FrameLayout.LayoutParams(-1, -1))
        }
        try {
            // activity.window.decorView installe la fenetre avant de demander le controleur (sinon NullPointerException).
            val decor = activity.window.decorView
            WindowCompat.getInsetsController(activity.window, decor).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        } catch (e: Exception) {
            android.util.Log.w("SommeilPlus", "Controle des barres systeme indisponible", e)
        }
        var imeShown = false
        ViewCompat.setOnApplyWindowInsetsListener(frame) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            val visible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (visible != imeShown) {
                imeShown = visible
                onImeChanged?.invoke(visible)
                if (visible) v.post {
                    activity.currentFocus?.let { f -> f.requestRectangleOnScreen(android.graphics.Rect(0, 0, f.width, f.height)) }
                }
            }
            insets
        }
        ViewCompat.requestApplyInsets(frame)
        return frame
    }

    /** Boutons rapides sous un champ e-mail : @ , @gmail.com ... (utile si la touche @ du clavier ne marche pas). */
    fun emailShortcuts(c: Context, field: EditText): View {
        val row = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("@", "@gmail.com", "@hotmail.com", "@yahoo.com").forEach { suffix ->
            row.addView(TextView(c).apply {
                text = suffix
                textSize = 14f
                setTextColor(BLUE)
                gravity = Gravity.CENTER
                isFocusable = false
                isFocusableInTouchMode = false
                background = gradient(0xFF0B1B27.toInt(), 0xFF0B1B27.toInt(), dp(c, 16).toFloat()).apply { setStroke(dp(c, 1), BORDER) }
                setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8))
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(c, 8) }
                setOnClickListener {
                    val e = field.text
                    val at = e.indexOf('@')
                    if (suffix == "@") {
                        if (at < 0) e.append("@")
                    } else {
                        if (at >= 0) e.delete(at, e.length)
                        e.append(suffix)
                    }
                    field.setSelection(field.text.length)
                    field.requestFocus()
                }
            })
        }
        return HorizontalScrollView(c).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(c, 8)) }
        }
    }

    fun gradient(start: Int, end: Int, radius: Float): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, end)).apply { cornerRadius = radius }
}
