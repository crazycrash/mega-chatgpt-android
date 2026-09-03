package com.example.megachatgpt

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 48)
            gravity = Gravity.TOP
        }

        val title = TextView(this).apply {
            text = "MEGA ↔ ChatGPT Test"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        }

        statusText = TextView(this).apply {
            textSize = 17f
            setPadding(0, 28, 0, 28)
        }

        val testButton = Button(this).apply {
            text = "Esegui test locale"
            setOnClickListener { runDiagnostics() }
        }

        val openMegaButton = Button(this).apply {
            text = "Apri MEGA"
            setOnClickListener { openMega() }
        }

        val info = TextView(this).apply {
            text = "Questa build verifica ciò che Android rende visibile a un'app esterna. Se l'account MEGA non compare, significa che la sessione dell'app ufficiale non viene esportata ad altre app: in quel caso il prossimo step sarà autenticare il connettore tramite MEGA SDK invece di leggere dati privati dell'app MEGA."
            textSize = 14f
            setPadding(0, 24, 0, 0)
        }

        root.addView(title)
        root.addView(statusText)
        root.addView(testButton, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(openMegaButton, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(info)

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        runDiagnostics()
    }

    private fun runDiagnostics() {
        val out = StringBuilder()
        out.append("Stato test\n\n")

        var visibleMegaAccount: String? = null
        try {
            val accounts = AccountManager.get(this).accounts
            val mega = accounts.firstOrNull {
                it.type.contains("mega", ignoreCase = true) || it.name.contains("mega", ignoreCase = true)
            }
            visibleMegaAccount = mega?.name
            out.append("Account MEGA visibile ad Android: ")
            out.append(visibleMegaAccount ?: "NO")
            out.append("\n")
        } catch (e: Exception) {
            out.append("Account MEGA visibile ad Android: NON ACCESSIBILE\n")
        }

        val megaIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mega.nz"))
        val resolved = megaIntent.resolveActivity(packageManager)
        out.append("Gestore disponibile per link MEGA: ")
        out.append(if (resolved != null) "SI" else "NO")
        out.append("\n")
        if (resolved != null) {
            out.append("Handler: ${resolved.packageName}\n")
        }

        out.append("\nConnessione ChatGPT ↔ MEGA: ")
        out.append(if (visibleMegaAccount != null) "account locale individuato" else "sessione locale non esportata")

        statusText.text = out.toString()
    }

    private fun openMega() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://mega.nz")))
        } catch (_: Exception) {
            statusText.append("\n\nImpossibile aprire MEGA o un browser compatibile.")
        }
    }
}
