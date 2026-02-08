package com.classy.securechat.data

import android.os.Handler
import android.os.Looper
import com.classy.securechat.model.Message
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object ChatRepository {
    private val database = Firebase.database("https://chat-29be2-default-rtdb.europe-west1.firebasedatabase.app")
    private val myRef = database.getReference("secure_messages")

    fun sendMessage(text: String, isSecret: Boolean) {
        val messageId = myRef.push().key ?: return
        val encryptedText = CryptoManager.encrypt(text, messageId)

        // אם סודי -> 10 שניות, אחרת -> כלום
        val expirationTime = if (isSecret) System.currentTimeMillis() + 10000 else null

        val message = Message(
            id = messageId,
            text = encryptedText,
            senderId = "Me",
            timestamp = System.currentTimeMillis(),
            isEncrypted = true,
            expiresAt = expirationTime
        )
        myRef.child(messageId).setValue(message)
    }

    fun listenForMessages(onMessagesChanged: (List<Message>) -> Unit) {
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()
                val currentTime = System.currentTimeMillis()

                for (child in snapshot.children) {
                    val msg = child.getValue(Message::class.java)

                    if (msg != null) {
                        // --- מנגנון הפצצה המשופר 💣 ---
                        if (msg.expiresAt != null) {
                            if (currentTime > msg.expiresAt) {
                                // 1. אם הזמן כבר עבר -> תמחק מיד!
                                child.ref.removeValue()
                                continue
                            } else {
                                // 2. אם הזמן עוד לא עבר -> תפעיל טיימר לעתיד
                                val timeToWait = msg.expiresAt - currentTime
                                Handler(Looper.getMainLooper()).postDelayed({
                                    child.ref.removeValue()
                                }, timeToWait)
                            }
                        }
                        // -------------------------------

                        val decryptedText = try {
                            CryptoManager.decrypt(msg.text, msg.id)
                        } catch (e: Exception) {
                            "Error"
                        }
                        messages.add(msg.copy(text = decryptedText))
                    }
                }
                onMessagesChanged(messages)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}