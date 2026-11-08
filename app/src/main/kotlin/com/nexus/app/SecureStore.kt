package com.nexus.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Small secrets (a Steam Web API key) encrypted with an Android Keystore key. The key never leaves the device. */
object SecureStore {
    private const val ALIAS = "nexus_secrets_v1"
    private const val FILE = "nexus_secrets"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build(),
        )
        return gen.generateKey()
    }

    fun put(ctx: Context, name: String, value: String) {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val ct = c.doFinal(value.toByteArray(Charsets.UTF_8))
        val packed = Base64.encodeToString(c.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(ct, Base64.NO_WRAP)
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(name, packed).apply()
    }

    fun get(ctx: Context, name: String): String? {
        val packed = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(name, null) ?: return null
        return runCatching {
            val (iv, ct) = packed.split(":")
            val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))) }
            String(c.doFinal(Base64.decode(ct, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    fun remove(ctx: Context, name: String) { ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove(name).apply() }
}
