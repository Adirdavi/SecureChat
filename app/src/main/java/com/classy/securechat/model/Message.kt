package com.classy.securechat.model

data class Message(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val timestamp: Long = 0,
    val isEncrypted: Boolean = false,
    val expiresAt: Long? = null
)