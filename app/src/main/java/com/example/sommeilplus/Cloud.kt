package com.example.sommeilplus

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Integration Firebase (Authentication + Cloud Firestore).
 *
 * Structure Firestore :
 *   users/{uid}                      -> name, email, birth
 *   users/{uid}/sessions/{cloudId}   -> date, hours, quality
 *   users/{uid}/settings/reminders   -> bed, wake, bedHour, bedMinute, wakeHour, wakeMinute
 *
 * Si app/google-services.json est absent, ready() renvoie false et l'application
 * continue de fonctionner en mode local (SQLite) comme avant.
 */
object Cloud {
    private const val TAG = "SommeilPlusCloud"

    fun ready(context: Context): Boolean = try {
        FirebaseApp.getApps(context.applicationContext).isNotEmpty()
    } catch (e: Exception) {
        false
    }

    private fun auth() = FirebaseAuth.getInstance()
    private fun firestore() = FirebaseFirestore.getInstance()
    private fun uid(): String? = auth().currentUser?.uid

    // ---------------- Authentification ----------------

    fun register(
        context: Context, name: String, email: String, password: String, birth: String,
        callback: (ok: Boolean, error: String?) -> Unit
    ) {
        val app = context.applicationContext
        auth().createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val id = task.result?.user?.uid
                if (id != null) {
                    firestore().collection("users").document(id).set(
                        mapOf(
                            "name" to name, "email" to email.lowercase(), "birth" to birth,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).addOnFailureListener { android.util.Log.w(TAG, "Profil non sauvegarde", it) }
                }
                SleepDatabase(app).upsertProfile(name, email, birth)
                callback(true, null)
            } else {
                callback(false, errorMessage(task.exception))
            }
        }
    }

    fun login(email: String, password: String, callback: (ok: Boolean, error: String?) -> Unit) {
        auth().signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) callback(true, null) else callback(false, errorMessage(task.exception))
        }
    }

    fun resetPassword(email: String, callback: (ok: Boolean, error: String?) -> Unit) {
        auth().sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) callback(true, null) else callback(
                false,
                if (task.exception is FirebaseAuthInvalidUserException) "Aucun compte trouvé avec cet e-mail"
                else errorMessage(task.exception)
            )
        }
    }

    fun signOut(context: Context) {
        if (ready(context)) try { auth().signOut() } catch (_: Exception) {}
    }

    private fun errorMessage(e: Exception?): String = when (e) {
        is FirebaseAuthUserCollisionException -> "Cet email existe déjà ou les données sont invalides"
        is FirebaseAuthWeakPasswordException -> "Remplissez tous les champs (mot de passe : 6 caractères minimum)"
        is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> "Email ou mot de passe incorrect"
        is FirebaseNetworkException -> "Pas de connexion Internet"
        else -> "Erreur Firebase : ${e?.localizedMessage ?: "inconnue"}"
    }

    // ---------------- Donnees (Firestore) ----------------

    fun pushSession(context: Context, cloudId: String, date: String, hours: Float, quality: Int) {
        if (!ready(context)) return
        val id = uid() ?: return
        firestore().collection("users").document(id).collection("sessions").document(cloudId)
            .set(mapOf("date" to date, "hours" to hours.toDouble(), "quality" to quality))
            .addOnFailureListener { android.util.Log.w(TAG, "Session non sauvegardee", it) }
    }

    fun pushReminders(context: Context) {
        if (!ready(context)) return
        val id = uid() ?: return
        val p = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        firestore().collection("users").document(id).collection("settings").document("reminders").set(
            mapOf(
                "bed" to p.getBoolean("bed", false), "wake" to p.getBoolean("wake", false),
                "bedHour" to p.getInt("bedHour", 22), "bedMinute" to p.getInt("bedMinute", 0),
                "wakeHour" to p.getInt("wakeHour", 7), "wakeMinute" to p.getInt("wakeMinute", 0)
            )
        ).addOnFailureListener { android.util.Log.w(TAG, "Alarmes non sauvegardees", it) }
    }

    /**
     * Apres connexion : profil, sessions (fusion dans les deux sens) et alarmes.
     * [onDone] est appele une seule fois, meme en cas d'erreur / hors ligne.
     */
    fun syncAfterLogin(context: Context, email: String, onDone: () -> Unit) {
        val app = context.applicationContext
        val id = uid()
        if (!ready(app) || id == null) { onDone(); return }
        val db = SleepDatabase(app)
        var finished = false
        fun finish() { if (!finished) { finished = true; onDone() } }

        val userDoc = firestore().collection("users").document(id)
        // 1) envoie les sessions locales qui ne sont pas encore dans le cloud
        try {
            db.cloudSessions(email).forEach { s ->
                userDoc.collection("sessions").document(s.cloudId)
                    .set(mapOf("date" to s.date, "hours" to s.hours.toDouble(), "quality" to s.quality))
            }
        } catch (e: Exception) { android.util.Log.w(TAG, "Envoi des sessions impossible", e) }

        // 2) profil -> 3) sessions -> 4) alarmes
        userDoc.get().addOnCompleteListener { profileTask ->
            val doc = if (profileTask.isSuccessful) profileTask.result else null
            if (profileTask.isSuccessful && doc != null && doc.exists()) {
                db.upsertProfile(doc.getString("name") ?: "", email, doc.getString("birth") ?: "")
            } else {
                db.upsertProfile("", email, "", onlyIfMissing = true)
            }
            userDoc.collection("sessions").get().addOnCompleteListener { sessionsTask ->
                val snap = if (sessionsTask.isSuccessful) sessionsTask.result else null
                if (sessionsTask.isSuccessful && snap != null) {
                    val remote = snap.documents.mapNotNull { d ->
                        val date = d.getString("date") ?: return@mapNotNull null
                        val hours = d.getDouble("hours") ?: return@mapNotNull null
                        val quality = d.getLong("quality") ?: return@mapNotNull null
                        SleepDatabase.CloudSession(d.id, date, hours.toFloat(), quality.toInt())
                    }
                    db.mergeRemoteSessions(email, remote)
                }
                userDoc.collection("settings").document("reminders").get().addOnCompleteListener { remTask ->
                    val r = if (remTask.isSuccessful) remTask.result else null
                    if (remTask.isSuccessful && r != null && r.exists()) {
                        app.getSharedPreferences("reminders", Context.MODE_PRIVATE).edit()
                            .putBoolean("bed", r.getBoolean("bed") ?: false)
                            .putBoolean("wake", r.getBoolean("wake") ?: false)
                            .putInt("bedHour", (r.getLong("bedHour") ?: 22L).toInt())
                            .putInt("bedMinute", (r.getLong("bedMinute") ?: 0L).toInt())
                            .putInt("wakeHour", (r.getLong("wakeHour") ?: 7L).toInt())
                            .putInt("wakeMinute", (r.getLong("wakeMinute") ?: 0L).toInt())
                            .apply()
                        try { AlarmScheduler.rescheduleSaved(app) } catch (e: Exception) {
                            android.util.Log.w(TAG, "Reprogrammation impossible", e)
                        }
                    }
                    finish()
                }
            }
        }
    }
}
