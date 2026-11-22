package com.example.sommeilplus

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

class SleepDatabase(context: Context) : SQLiteOpenHelper(context, "sommeil_plus.db", null, 3) {
    private val appContext: Context = context.applicationContext

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, email TEXT UNIQUE NOT NULL, pass TEXT NOT NULL, birth TEXT NOT NULL)")
        db.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, hours REAL NOT NULL, quality INTEGER NOT NULL, owner_email TEXT NOT NULL DEFAULT '', cloud_id TEXT NOT NULL DEFAULT '')")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration non destructive : les anciennes sessions restent disponibles.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN owner_email TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN cloud_id TEXT NOT NULL DEFAULT ''")
        }
    }
    private fun legacyHash(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun secureHash(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        spec.clearPassword()
        return "pbkdf2$" + Base64.encodeToString(salt, Base64.NO_WRAP) + "$" +
            Base64.encodeToString(derived, Base64.NO_WRAP)
    }

    private fun passwordMatches(stored: String, password: String): Boolean {
        if (!stored.startsWith("pbkdf2$")) return stored == legacyHash(password)
        return try {
            val parts = stored.split("$")
            if (parts.size != 3) return false
            val salt = Base64.decode(parts[1], Base64.NO_WRAP)
            val expected = Base64.decode(parts[2], Base64.NO_WRAP)
            val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
            val actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
            spec.clearPassword()
            MessageDigest.isEqual(expected, actual)
        } catch (_: Exception) { false }
    }

    fun register(name: String, email: String, password: String, birth: String): Boolean {
        val v = ContentValues().apply {
            put("name", name.trim()); put("email", email.trim().lowercase())
            put("pass", secureHash(password)); put("birth", birth)
        }
        return try { writableDatabase.insertOrThrow("users", null, v) > 0 } catch (_: Exception) { false }
    }

    fun login(email: String, password: String): AppUser? {
        var user: AppUser? = null
        var storedPassword: String? = null
        readableDatabase.rawQuery("SELECT name,email,birth,pass FROM users WHERE email=?",
            arrayOf(email.trim().lowercase())).use { c ->
            if (c.moveToFirst()) {
                user = AppUser(c.getString(0), c.getString(1), c.getString(2))
                storedPassword = c.getString(3)
            }
        }
        val account = user ?: return null
        val savedHash = storedPassword ?: return null
        if (!passwordMatches(savedHash, password)) return null
        // Mise à niveau transparente des anciens mots de passe SHA-256 non salés.
        if (!savedHash.startsWith("pbkdf2$")) {
            writableDatabase.update("users", ContentValues().apply { put("pass", secureHash(password)) },
                "email = ?", arrayOf(account.email))
        }
        return account
    }
    fun user(email: String): AppUser? {
        readableDatabase.rawQuery("SELECT name,email,birth FROM users WHERE email=?", arrayOf(email)).use { c ->
            return if (c.moveToFirst()) AppUser(c.getString(0), c.getString(1), c.getString(2)) else null
        }
    }
    fun addSession(hours: Float, quality: Int, ownerEmail: String) {
        val cloudId = java.util.UUID.randomUUID().toString()
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val v = ContentValues().apply {
            put("date", date)
            put("hours", hours)
            put("quality", quality)
            put("owner_email", ownerEmail.trim().lowercase())
            put("cloud_id", cloudId)
        }
        writableDatabase.insert("sessions", null, v)
        // Sauvegarde aussi dans Firebase (si configure et connecte).
        Cloud.pushSession(appContext, cloudId, date, hours, quality)
    }

    // ---------- Synchronisation Firebase ----------

    /** Cree ou met a jour le profil local d'un compte Firebase (le mot de passe reste chez Firebase). */
    fun upsertProfile(name: String, email: String, birth: String, onlyIfMissing: Boolean = false) {
        val mail = email.trim().lowercase()
        val exists = readableDatabase.rawQuery("SELECT 1 FROM users WHERE email=?", arrayOf(mail)).use { it.moveToFirst() }
        if (exists) {
            if (onlyIfMissing) return
            writableDatabase.update("users", ContentValues().apply {
                put("name", name.ifBlank { mail.substringBefore("@") }); put("birth", birth.ifBlank { "—" })
            }, "email=?", arrayOf(mail))
        } else {
            writableDatabase.insert("users", null, ContentValues().apply {
                put("name", name.ifBlank { mail.substringBefore("@") }); put("email", mail)
                put("pass", "firebase"); put("birth", birth.ifBlank { "—" })
            })
        }
    }

    data class CloudSession(val cloudId: String, val date: String, val hours: Float, val quality: Int)

    /** Sessions locales du compte, avec un cloud_id garanti (pour l'envoi vers Firestore). */
    fun cloudSessions(ownerEmail: String): List<CloudSession> {
        sessions(ownerEmail) // rattache d'abord les anciennes sessions sans proprietaire
        val mail = ownerEmail.trim().lowercase()
        val out = mutableListOf<CloudSession>()
        val toFill = mutableListOf<Pair<Long, String>>()
        readableDatabase.rawQuery("SELECT id,date,hours,quality,cloud_id FROM sessions WHERE owner_email=?", arrayOf(mail)).use { c ->
            while (c.moveToNext()) {
                var cid = c.getString(4)
                if (cid.isBlank()) {
                    cid = java.util.UUID.randomUUID().toString()
                    toFill.add(c.getLong(0) to cid)
                }
                out.add(CloudSession(cid, c.getString(1), c.getFloat(2), c.getInt(3)))
            }
        }
        toFill.forEach { (rowId, cid) ->
            writableDatabase.update("sessions", ContentValues().apply { put("cloud_id", cid) }, "id=?", arrayOf(rowId.toString()))
        }
        return out
    }

    /** Ajoute localement les sessions presentes dans Firestore mais absentes de ce telephone. */
    fun mergeRemoteSessions(ownerEmail: String, remote: List<CloudSession>) {
        val mail = ownerEmail.trim().lowercase()
        val known = HashSet<String>()
        readableDatabase.rawQuery("SELECT cloud_id FROM sessions WHERE owner_email=?", arrayOf(mail)).use { c ->
            while (c.moveToNext()) known.add(c.getString(0))
        }
        remote.filter { it.cloudId !in known }.forEach { r ->
            writableDatabase.insert("sessions", null, ContentValues().apply {
                put("date", r.date); put("hours", r.hours); put("quality", r.quality)
                put("owner_email", mail); put("cloud_id", r.cloudId)
            })
        }
    }

    fun sessions(ownerEmail: String): List<SleepSession> {
        val out = mutableListOf<SleepSession>()
        // Rattache les anciennes sessions au premier compte qui les récupère, sans les supprimer.
        val normalizedEmail = ownerEmail.trim().lowercase()
        writableDatabase.update(
            "sessions",
            ContentValues().apply { put("owner_email", normalizedEmail) },
            "owner_email = ''",
            null
        )
        readableDatabase.rawQuery(
            "SELECT id,date,hours,quality FROM sessions WHERE owner_email = ? ORDER BY id DESC",
            arrayOf(normalizedEmail)
        ).use { c ->
            while (c.moveToNext()) out.add(SleepSession(c.getLong(0), c.getString(1), c.getFloat(2), c.getInt(3)))
        }
        return out
    }
}
