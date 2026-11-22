package com.example.sommeilplus

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import java.io.OutputStreamWriter

class ExportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = Ui.page(this, "Exporter mes données", showBack = true)
        root.addView(Ui.text(this, "Exportez vos sessions de sommeil au format CSV."))
        root.addView(Ui.button(this, "Exporter CSV") {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "text/csv"; putExtra(Intent.EXTRA_TITLE, "sommeil_plus.csv")
            }
            create.launch(intent)
        })
        setContentView(Ui.edgeToEdge(this, Ui.withBottomNavigation(this, Ui.scroll(this, root), 4)))
    }
    private val create = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            try {
                contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                        writer.write("date,hours,quality\n")
                        SleepDatabase(this).sessions(getSharedPreferences("app", MODE_PRIVATE).getString("email", "") ?: "").forEach { writer.write("${it.date},${it.hours},${it.quality}\n") }
                    }
                }
                Toast.makeText(this, Translator.text(this, "Export réussi"), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(this, Translator.text(this, "Erreur export : ${e.message}"), Toast.LENGTH_LONG).show() }
        }
    }
}
