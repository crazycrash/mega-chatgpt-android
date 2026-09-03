package com.example.megachatgpt.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class MegaAccountRecord(
    val accountRef: String,
    val displayName: String,
    val email: String?,
    val session: String
)

class SecureAccountStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun createAccount(displayName: String, email: String?, session: String): String {
        val accountRef = "acc_${UUID.randomUUID()}"
        saveAccount(accountRef, displayName, email, session)
        if (currentAccountRef() == null) setCurrentAccount(accountRef)
        return accountRef
    }

    fun saveAccount(accountRef: String, displayName: String, email: String?, session: String) {
        require(accountRef.startsWith("acc_")) { "Invalid account reference" }
        val payload = JSONObject()
            .put("accountRef", accountRef)
            .put("displayName", displayName)
            .put("email", email)
            .put("session", session)
            .toString()

        val refs = accountRefs().toMutableSet().apply { add(accountRef) }
        prefs.edit()
            .putString("account.$accountRef", encrypt(payload))
            .putStringSet(KEY_REFS, refs)
            .apply()
    }

    fun loadAccount(accountRef: String): MegaAccountRecord? {
        val encrypted = prefs.getString("account.$accountRef", null) ?: return null
        val payload = JSONObject(decrypt(encrypted))
        return MegaAccountRecord(
            accountRef = payload.getString("accountRef"),
            displayName = payload.getString("displayName"),
            email = payload.optString("email").takeIf { it.isNotBlank() && it != "null" },
            session = payload.getString("session")
        )
    }

    fun accountRefs(): Set<String> = prefs.getStringSet(KEY_REFS, emptySet())?.toSet() ?: emptySet()

    fun currentAccountRef(): String? = prefs.getString(KEY_CURRENT_REF, null)

    fun setCurrentAccount(accountRef: String) {
        // Decrypt before switching so corruption/tampering is detected by AES-GCM.
        requireNotNull(loadAccount(accountRef)) { "Unknown account reference" }
        prefs.edit().putString(KEY_CURRENT_REF, accountRef).apply()
    }

    fun deleteAccount(accountRef: String) {
        val refs = accountRefs().toMutableSet().apply { remove(accountRef) }
        val edit = prefs.edit()
            .remove("account.$accountRef")
            .putStringSet(KEY_REFS, refs)
        if (currentAccountRef() == accountRef) {
            edit.putString(KEY_CURRENT_REF, refs.firstOrNull())
        }
        edit.apply()
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return JSONObject()
            .put("v", 1)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .toString()
    }

    private fun decrypt(serialized: String): String {
        val record = JSONObject(serialized)
        require(record.getInt("v") == 1) { "Unsupported encrypted record version" }
        val iv = Base64.decode(record.getString("iv"), Base64.NO_WRAP)
        val ciphertext = Base64.decode(record.getString("ciphertext"), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val PREFS_NAME = "mega_chatgpt_secure_accounts"
        private const val KEY_REFS = "account_refs"
        private const val KEY_CURRENT_REF = "current_account_ref"
        private const val KEY_ALIAS = "mega_chatgpt_account_vault_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
