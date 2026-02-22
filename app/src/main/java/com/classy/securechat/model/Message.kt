package com.classy.securechat.model

import com.google.firebase.firestore.PropertyName

data class Message(
    val id: String = "",
    val text: String = "",
    val textForSender: String = "",
    val senderId: String = "",
    val senderUid: String = "",
    val timestamp: Long = 0,

    @get:PropertyName("isEncrypted") @set:PropertyName("isEncrypted")
    var isEncrypted: Boolean = false,

    @get:PropertyName("isSecret") @set:PropertyName("isSecret")
    var isSecret: Boolean = false,

    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,

    val expiresAt: Long? = null
)