package com.example.megachatgpt

import android.accounts.AccountManager
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var receivedText: TextView

    companion object {
        private const val MEGA_PACKAGE = "mega.privacy.android.app"
        private const val PICK_FILE_REQUEST = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 48)
            gravity = Gravity.TOP
        }

        val title = TextView(this).apply {
            text = "MEGA ↔ ChatGPT Test v0.2"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        }

        statusText = TextView(this).apply {
            textSize = 16f
            setPadding(0, 28, 0, 24)
        }

        receivedText = TextView(this).apply {
            text = "Nessun link ricevuto."
            textSize = 15f
            setPadding(0, 22, 0, 22)
        }

        val testButton = Button(this).apply {
            text = "Esegui test MEGA"
            setOnClickListener { runDiagnostics() }
        }

        val openMegaButton = Button(this).apply {
            text = "Apri app MEGA"
            setOnClickListener { openMegaApp() }
        }

        val clipboardButton = Button(this).apply {
            text = "Incolla link MEGA dagli appunti"
            setOnClickListener { readClipboard() }
        }

        val pickerButton = Button(this).apply {
            text = "Apri selettore file Android"
            setOnClickListener { openSystemPicker() }
        }

        val info = TextView(this).apply {
            text = "Test v0.2: verifica se Android rende visibile l'app MEGA dopo aver dichiarato correttamente la package visibility, cerca handler per mega.nz, elenca eventuali ContentProvider esportati da MEGA e può ricevere un link condiviso da MEGA. Per provare Corsi ITA, aprilo in MEGA e usa Condividi/Copia link, poi scegli questa app o premi Incolla link."
            textSize = 14f
            setPadding(0, 24, 0, 0)
        }

        root.addView(title)
        root.addView(statusText)
        root.addView(testButton, fullWidth())
        root.addView(openMegaButton, fullWidth())
        root.addView(clipboardButton, fullWidth())
        root.addView(pickerButton, fullWidth())
        root.addView(receivedText)
        root.addView(info)

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        handleIncomingIntent(intent)
        runDiagnostics()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun fullWidth() = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun runDiagnostics() {
        val out = StringBuilder()
        out.append("Stato test\n\n")

        val megaInstalled = try {
            packageManager.getPackageInfo(MEGA_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
        out.append("App MEGA installata/visibile: ${if (megaInstalled) "SI" else "NO"}\n")

        var visibleMegaAccount: String? = null
        try {
            val accounts = AccountManager.get(this).accounts
            visibleMegaAccount = accounts.firstOrNull {
                it.type.contains("mega", ignoreCase = true) || it.name.contains("mega", ignoreCase = true)
            }?.name
            out.append("Account MEGA esportato ad AccountManager: ${visibleMegaAccount ?: "NO"}\n")
        } catch (_: Exception) {
            out.append("Account MEGA esportato ad AccountManager: NON ACCESSIBILE\n")
        }

        val megaIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mega.nz"))
        val handlers = packageManager.queryIntentActivities(megaIntent, PackageManager.MATCH_DEFAULT_ONLY)
        out.append("Handler visibili per https://mega.nz: ${handlers.size}\n")
        handlers.take(5).forEach { info ->
            out.append(" - ${info.activityInfo.packageName}/${info.activityInfo.name}\n")
        }

        if (megaInstalled) {
            try {
                val packageInfo = packageManager.getPackageInfo(MEGA_PACKAGE, PackageManager.GET_PROVIDERS)
                val exportedProviders = packageInfo.providers.orEmpty().filter { it.exported }
                out.append("ContentProvider MEGA esportati: ${exportedProviders.size}\n")
                exportedProviders.take(10).forEach { provider ->
                    out.append(" - ${provider.name} | authority=${provider.authority ?: "?"}\n")
                }
            } catch (e: Exception) {
                out.append("ContentProvider MEGA esportati: errore ${e.javaClass.simpleName}\n")
            }
        }

        out.append("\nSessione dell'app ufficiale riutilizzabile direttamente: ")
        out.append(if (visibleMegaAccount != null) "POSSIBILE" else "NO, serve MEGA SDK/login separato o un canale pubblico esportato")

        statusText.text = out.toString()
    }

    private fun openMegaApp() {
        val direct = packageManager.getLaunchIntentForPackage(MEGA_PACKAGE)
        try {
            if (direct != null) startActivity(direct)
            else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://mega.nz")))
        } catch (_: Exception) {
            receivedText.text = "Impossibile aprire MEGA."
        }
    }

    private fun readClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData? = clipboard.primaryClip
        val text = clip?.getItemAt(0)?.coerceToText(this)?.toString()
        if (text.isNullOrBlank()) {
            receivedText.text = "Appunti vuoti. Copia prima il link da MEGA."
        } else {
            showReceivedText(text, "Appunti")
        }
    }

    private fun openSystemPicker() {
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        try {
            startActivityForResult(picker, PICK_FILE_REQUEST)
        } catch (_: Exception) {
            receivedText.text = "Selettore file Android non disponibile."
        }
    }

    @Deprecated("Deprecated in Android API but kept for this minimal diagnostic app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            var displayName: String? = null
            try {
                val cursor: Cursor? = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) displayName = it.getString(0)
                }
            } catch (_: Exception) { }
            receivedText.text = "File ricevuto dal provider Android:\n${displayName ?: "senza nome"}\nURI: $uri"
        }
    }

    private fun handleIncomingIntent(incoming: Intent?) {
        if (incoming == null) return
        when (incoming.action) {
            Intent.ACTION_SEND -> {
                val text = incoming.getStringExtra(Intent.EXTRA_TEXT)
                if (!text.isNullOrBlank()) showReceivedText(text, "Condivisione Android")
            }
            Intent.ACTION_VIEW -> {
                incoming.data?.toString()?.let { showReceivedText(it, "Deep link") }
            }
        }
    }

    private fun showReceivedText(text: String, source: String) {
        val megaLinks = Regex("https?://(?:www\\.)?mega\\.nz/\\S+", RegexOption.IGNORE_CASE)
            .findAll(text)
            .map { it.value.trimEnd('.', ',', ')', ']', '}') }
            .toList()

        receivedText.text = buildString {
            append("Sorgente: $source\n")
            if (megaLinks.isEmpty()) {
                append("Nessun URL mega.nz riconosciuto.\n\n")
                append(text.take(1200))
            } else {
                append("Link MEGA catturati: ${megaLinks.size}\n")
                megaLinks.forEachIndexed { index, link -> append("${index + 1}. $link\n") }
            }
        }
    }
}
