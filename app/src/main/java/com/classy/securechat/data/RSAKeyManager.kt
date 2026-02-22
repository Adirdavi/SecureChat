package com.classy.securechat.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher

object RSAKeyManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "MySecureChatKey"

    fun generateKeyPair(): String? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (keyStore.containsAlias(KEY_ALIAS)) {
                return getPublicKeyString()
            }

            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                ANDROID_KEYSTORE
            )

            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .build()

            keyPairGenerator.initialize(parameterSpec)
            val keyPair = keyPairGenerator.generateKeyPair()

            Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getPublicKeyString(): String? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            val publicKey = entry?.certificate?.publicKey
            if (publicKey != null) {
                Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun encrypt(text: String, publicKeyString: String): String {
        return try {
            val publicBytes = Base64.decode(publicKeyString, Base64.NO_WRAP)
            val keySpec = java.security.spec.X509EncodedKeySpec(publicBytes)
            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val publicKey: PublicKey = keyFactory.generatePublic(keySpec)

            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encryptedBytes = cipher.doFinal(text.toByteArray())

            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            "EncryptionError"
        }
    }

    fun decrypt(encryptedText: String): String {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey ?: return "NoPrivateKey"

            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, privateKey)

            val encryptedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            String(decryptedBytes)
        } catch (e: Exception) {
            "DecryptionError"
        }
    }
}