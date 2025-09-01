package uy.com.abitab.iddigitalsdk.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class PinSecurityManager() {
    private val KEY_ALIAS = "biometric_pin_key_alias"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val AES_MODE = "AES/GCM/NoPadding"

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getSecretKey(): SecretKey {
        return try {
            (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
                ?: createNewKey()
        } catch (e: Exception) {
            throw SecurityException("Error al obtener o crear la clave segura", e)
        }
    }

    private fun createNewKey(): SecretKey {
        try {
            val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            return keyGenerator.generateKey()
        } catch (e: NoSuchAlgorithmException) {
            throw SecurityException("Algoritmo AES no soportado", e)
        } catch (e: NoSuchProviderException) {
            throw SecurityException("Proveedor AndroidKeyStore no encontrado", e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw SecurityException("Parámetros de generación de clave inválidos", e)
        }
    }

    fun encryptPin(pin: String): String {
        try {
            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))

            val combined = iv + encryptedBytes
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            throw SecurityException("Error al cifrar el PIN", e)
        }
    }

    fun decryptPin(encryptedPinData: String): String {
        try {
            val combined = Base64.decode(encryptedPinData, Base64.DEFAULT)

            val ivLength = 12
            if (combined.size < ivLength) {
                throw IllegalArgumentException("Datos cifrados incompletos o corruptos (IV faltante)")
            }

            val iv = combined.copyOfRange(0, ivLength)
            val encryptedBytes = combined.copyOfRange(ivLength, combined.size)

            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(AES_MODE)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException(
                "Error al descifrar el PIN. Puede que los datos estén corruptos o la clave haya cambiado.",
                e
            )
        }
    }
}