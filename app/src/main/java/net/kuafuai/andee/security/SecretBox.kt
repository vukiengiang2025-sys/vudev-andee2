package net.kuafuai.andee.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * At-rest encryption for the credentials this app stores: AI provider keys, the hub token and the
 * WebSocket token.
 *
 * The key itself lives in the Android Keystore and never leaves it, so a preferences file pulled off a
 * backup, an adb bugreport or a rooted-friend's phone contains AES/GCM ciphertext rather than a working
 * API key. This is the same construction [net.kuafuai.andee.config.Notebook] and Vault use; it is a
 * separate class with a separate alias so that wiping the notebook does not make the providers
 * unreadable and vice versa.
 *
 * Two failure modes are handled by returning null rather than throwing:
 *
 *  * a blob written by another install (restored backup) cannot be decrypted;
 *  * the Keystore was invalidated (lock-screen change, factory reset).
 *
 * In both cases the caller treats the secret as missing and asks the user to type it again, which is
 * exactly what happened. A crash here would make the app unopenable because of a stale key.
 */
object SecretBox {

    private const val TAG = "Secret"
    private const val KEY_ALIAS = "andee_secret_box_v1"
    private const val PREFS_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return gen.generateKey()
    }

    /** Plain text in, "v1:base64(iv || ciphertext)" out. Null when the Keystore refused. */
    fun seal(plain: String): String? = runCatching {
        if (plain.isEmpty()) return ""
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, secretKey())
        val body = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        "v1:" + Base64.encodeToString(c.iv + body, Base64.NO_WRAP)
    }.getOrElse {
        Log.e(TAG, "seal failed: the Keystore refused to encrypt", it)
        null
    }

    /** The inverse of [seal]; null when the blob is not ours or this install cannot read it. */
    fun open(blob: String?): String? {
        if (blob.isNullOrBlank()) return null
        if (!blob.startsWith("v1:")) return null
        return runCatching {
            val raw = Base64.decode(blob.removePrefix("v1:"), Base64.NO_WRAP)
            if (raw.size <= PREFS_IV_BYTES) return null
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(GCM_TAG_BITS, raw, 0, PREFS_IV_BYTES),
            )
            String(c.doFinal(raw, PREFS_IV_BYTES, raw.size - PREFS_IV_BYTES), Charsets.UTF_8)
        }.getOrElse {
            Log.w(TAG, "open failed: the blob belongs to another install or the Keystore was reset")
            null
        }
    }

    /** Mask a key for display: never show more than a recognisable prefix and length. */
    fun mask(key: String): String = when {
        key.isBlank() -> ""
        key.length <= 8 -> "••••"
        else -> key.take(4) + "••••" + key.takeLast(4)
    }
}
