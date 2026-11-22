package com.example.sommeilplus

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment

class AdviceFragment : Fragment() {
    private data class Advice(val title: String, val description: String, val actions: List<String>)

    private val problemAdvice = linkedMapOf(
        "Difficulté à m'endormir" to Advice(
            "Vous avez du mal à trouver le sommeil",
            "L'objectif est d'aider votre corps à ralentir progressivement avant le coucher.",
            listOf("Gardez une heure de coucher régulière, même le week-end.", "Évitez café, thé et boissons énergisantes dans les 6 heures avant le coucher.", "Éteignez les écrans 30 à 60 minutes avant de dormir.", "Essayez une respiration lente ou une lecture calme. Si vous ne dormez pas après un moment, levez-vous pour une activité tranquille puis revenez au lit quand la somnolence arrive.")
        ),
        "Réveils fréquents pendant la nuit" to Advice(
            "Vous vous réveillez plusieurs fois",
            "Essayez d'identifier ce qui perturbe votre nuit et de rendre votre environnement plus confortable.",
            listOf("Gardez la chambre sombre, calme et plutôt fraîche.", "Évitez les repas très lourds et l'alcool avant le coucher.", "Limitez les liquides juste avant de dormir si les réveils sont liés au besoin d'uriner.", "Notez l'heure des réveils et ce qui les précède pendant une à deux semaines.")
        ),
        "Réveil trop tôt" to Advice(
            "Vous vous réveillez trop tôt",
            "La régularité et la lumière jouent un rôle important dans le rythme veille-sommeil.",
            listOf("Levez-vous à une heure stable chaque jour.", "Exposez-vous à la lumière naturelle le matin.", "Évitez de regarder l'heure à répétition pendant la nuit.", "Évitez les longues siestes ou les siestes tardives.")
        ),
        "Sommeil non réparateur / fatigue" to Advice(
            "Vous vous sentez fatigué malgré le sommeil",
            "La durée ne suffit pas toujours : la régularité et la qualité du sommeil comptent aussi.",
            listOf("Prévoyez une durée de sommeil suffisante et régulière.", "Évitez la caféine en fin de journée.", "Bougez régulièrement pendant la journée, sans sport intense juste avant le coucher.", "Si vous ronflez fortement, avez des pauses respiratoires observées ou une somnolence importante le jour, parlez-en à un professionnel de santé.")
        ),
        "Horaires de sommeil irréguliers" to Advice(
            "Vos horaires changent beaucoup",
            "Un rythme stable aide l'horloge biologique à anticiper le sommeil.",
            listOf("Choisissez une heure de réveil réaliste et gardez-la chaque jour.", "Décalez progressivement votre coucher de 15 à 30 minutes si nécessaire.", "Prenez la lumière du jour le matin et réduisez la lumière forte le soir.", "Évitez les grasses matinées qui décalent fortement le rythme.")
        )
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val c = requireContext()
        val root = Ui.page(c, "Conseils sommeil")
        val email = c.getSharedPreferences("app", 0).getString("email", "") ?: ""
        val sessions = if (email.isBlank()) emptyList() else SleepDatabase(c).sessions(email)
        val recent = sessions.take(7)
        val avgHours = if (recent.isEmpty()) 0f else recent.map { it.hours }.average().toFloat()
        val avgQuality = if (recent.isEmpty()) 0 else recent.map { it.quality }.average().toInt()

        root.addView(Ui.text(c, "Des conseils simples pour mieux dormir", 21f, Color.WHITE, true))
        root.addView(Ui.text(c, "Des habitudes utiles à tous, puis des recommandations adaptées à vos nuits enregistrées et à la difficulté que vous choisissez.", 15f, Ui.MUTED))

        root.addView(Ui.card(c).apply {
            addView(Ui.text(c, "🌙  Conseils généraux", 19f, Ui.BLUE, true))
            val general = listOf(
                "Gardez des heures de coucher et de réveil aussi régulières que possible.",
                "Faites de votre chambre un espace sombre, calme et confortable.",
                "Évitez la caféine en fin de journée et les repas très lourds juste avant le coucher.",
                "Réduisez les écrans et la lumière forte avant de dormir.",
                "Pratiquez une activité physique régulièrement, de préférence en journée.",
                "Réservez un moment calme pour vous détendre avant le coucher."
            )
            general.forEachIndexed { index, tip ->
                addView(Ui.text(c, "${index + 1}.  $tip", 15f, Ui.NAVY))
            }
        })

        root.addView(Ui.text(c, "Votre sommeil enregistré", 19f, Color.WHITE, true))
        root.addView(Ui.card(c).apply {
            if (recent.isEmpty()) {
                addView(Ui.text(c, "Vous n'avez pas encore enregistré de nuit. Ajoutez quelques sessions dans Accueil pour recevoir une analyse personnalisée.", 15f, Ui.MUTED))
            } else {
                addView(Ui.text(c, "Sur vos ${recent.size} dernière(s) session(s)", 14f, Ui.MUTED, true))
                addView(Ui.text(c, "Durée moyenne : ${"%.1f".format(avgHours)} h", 18f, if (avgHours < 7f) Ui.ORANGE else Ui.GREEN, true))
                addView(Ui.text(c, "Qualité moyenne : $avgQuality %", 17f, if (avgQuality < 60) Ui.ORANGE else Ui.GREEN, true))
                val observation = when {
                    avgHours < 7f -> "Votre durée moyenne est inférieure à 7 heures. Essayez d'augmenter progressivement le temps réservé au sommeil."
                    avgQuality < 60 -> "Votre score de qualité moyen est bas. Observez les réveils nocturnes, le stress, l'environnement et vos habitudes du soir."
                    else -> "Vos dernières données sont plutôt encourageantes. Continuez à garder un rythme régulier."
                }
                addView(Ui.text(c, observation, 15f, Ui.NAVY))
            }
            addView(Ui.text(c, "Ces indicateurs donnent des pistes, pas un diagnostic médical.", 13f, Ui.MUTED))
        })

        root.addView(Ui.text(c, "Quel problème rencontrez-vous ?", 19f, Color.WHITE, true))
        root.addView(Ui.text(c, "Choisissez la difficulté qui vous ressemble le plus pour afficher des conseils ciblés.", 14f, Ui.MUTED))
        val options = problemAdvice.keys.toList()
        val spinner = Spinner(c).apply {
            adapter = ArrayAdapter(c, android.R.layout.simple_spinner_dropdown_item, options.map { Translator.text(c, it) }).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            background = Ui.gradient(0xFF102330.toInt(), 0xFF0C1D29.toInt(), Ui.dp(c, 10).toFloat())
            setPadding(Ui.dp(c, 10), Ui.dp(c, 6), Ui.dp(c, 10), Ui.dp(c, 6))
        }
        root.addView(spinner, LinearLayout.LayoutParams(-1, Ui.dp(c, 54)).apply { setMargins(0, Ui.dp(c, 8), 0, Ui.dp(c, 8)) })
        val resultCard = Ui.card(c)
        root.addView(resultCard)
        fun showAdvice(key: String) {
            resultCard.removeAllViews()
            val advice = problemAdvice[key] ?: return
            resultCard.addView(Ui.text(c, "✦  ${advice.title}", 18f, Ui.BLUE, true))
            resultCard.addView(Ui.text(c, advice.description, 15f, Ui.MUTED))
            advice.actions.forEachIndexed { i, action -> resultCard.addView(Ui.text(c, "${i + 1}.  $action", 15f, Ui.NAVY)) }
            resultCard.addView(Ui.text(c, "Si le problème persiste, s'aggrave ou affecte votre quotidien, demandez conseil à un professionnel de santé.", 14f, Ui.ORANGE, true))
        }
        showAdvice(options.first())
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { showAdvice(options[position]) }
        }

        root.addView(Ui.button(c, "Partager un conseil") {
            val selected = problemAdvice[options.getOrNull(spinner.selectedItemPosition)]?.actions?.firstOrNull() ?: "Gardez des horaires de sommeil réguliers."
            val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, Translator.text(c, selected)) }
            startActivity(Intent.createChooser(send, Translator.text(c, "Partager un conseil")))
        })
        return Ui.scroll(c, root)
    }
}
