package com.classy.securechat.data

import android.os.Handler
import android.os.Looper
import com.classy.securechat.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object ChatRepository {
    private val database = Firebase.database("https://chat-29be2-default-rtdb.europe-west1.firebasedatabase.app")
    private val myRef = database.getReference("secure_messages")


    fun sendMessage(text: String, isSecret: Boolean, receiverPublicKey: String, myPublicKey: String) {
        val messageId = myRef.push().key ?: return
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val myName = UserSharedPreferences.myName ?: "Me"


        val encryptedForReceiver = if (receiverPublicKey.isNotEmpty()) RSAKeyManager.encrypt(text, receiverPublicKey) else "NoKey"
        val encryptedForMe = if (myPublicKey.isNotEmpty()) RSAKeyManager.encrypt(text, myPublicKey) else "NoKey"

        val expirationTime = if (isSecret) System.currentTimeMillis() + 10000 else null

        val message = Message(
            id = messageId,
            text = encryptedForReceiver,
            textForSender = encryptedForMe,
            senderId = myName,
            senderUid = myUid,
            timestamp = System.currentTimeMillis(),
            isEncrypted = true,
            isSecret = isSecret,
            expiresAt = expirationTime,
            isRead = false
        )
        myRef.child(messageId).setValue(message)
    }

    fun listenForMessages(onMessagesChanged: (List<Message>) -> Unit) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()
                val currentTime = System.currentTimeMillis()

                for (child in snapshot.children) {
                    val msg = child.getValue(Message::class.java)

                    if (msg != null) {

                        if (msg.expiresAt != null) {
                            if (currentTime > msg.expiresAt) {
                                child.ref.removeValue()
                                continue
                            } else {
                                val timeToWait = msg.expiresAt - currentTime
                                Handler(Looper.getMainLooper()).postDelayed({
                                    child.ref.removeValue()
                                }, timeToWait)
                            }
                        }


                        val isMe = msg.senderUid == myUid
                        val textToDecrypt = if (isMe && msg.textForSender.isNotEmpty()) msg.textForSender else msg.text

                        val decryptedText = try {
                            RSAKeyManager.decrypt(textToDecrypt)
                        } catch (e: Exception) {
                            "Error Decrypting"
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