package com.classy.securechat.data

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"

    // זה הסוד שקיים אצל כל מי שמתקין את האפליקציה
    // בזכותו אפשר לראות את ההודעות מכל טלפון!
    private const val MASTER_SECRET = "Adir_Is_The_Best_Developer_2026"

    private fun generateKeyForMessage(messageId: String): SecretKeySpec {
        // מערבבים את הסוד הראשי עם ה-ID הייחודי של ההודעה
        val combined = MASTER_SECRET + messageId
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(combined.toByteArray())
        val keyBytes = bytes.copyOf(16)
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    // ההצפנה דורשת עכשיו את ה-ID
    fun encrypt(message: String, messageId: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val uniqueKey = generateKeyForMessage(messageId)

            cipher.init(Cipher.ENCRYPT_MODE, uniqueKey)
            val encryptedBytes = cipher.doFinal(message.toByteArray())
            val iv = cipher.iv

            val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encryptedString = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

            "$ivString:$encryptedString"
        } catch (e: Exception) {
            e.printStackTrace()
            "Error"
        }
    }

    // הפענוח דורש את ה-ID כדי לשחזר את המפתח
    fun decrypt(encryptedMessage: String, messageId: String): String {
        return try {
            val parts = encryptedMessage.split(":")
            if (parts.size != 2) return "Error"

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val uniqueKey = generateKeyForMessage(messageId)
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.DECRYPT_MODE, uniqueKey, ivSpec)
            val decodedBytes = cipher.doFinal(encryptedBytes)
            String(decodedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            "Error Decrypting"
        }
    }
}