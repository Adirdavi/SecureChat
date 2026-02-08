package com.classy.securechat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classy.securechat.data.CryptoManager
import com.classy.securechat.data.UserSharedPreferences
import com.classy.securechat.model.Message // הנה הייבוא של המודל שלך!
import com.classy.securechat.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(contactName: String, onBackClick: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    var isSecretMode by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<Message>() }
    val db = FirebaseFirestore.getInstance()
    val myName = UserSharedPreferences.myName ?: "Unknown"

    val chatId = listOf(myName, contactName).sorted().joinToString("_")

    DisposableEffect(chatId) {
        val registration = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    messages.clear()
                    val now = System.currentTimeMillis()
                    for (doc in snapshot.documents) {
                        // המרה למודל הקיים שלך
                        var msg = doc.toObject(Message::class.java)

                        if (msg != null) {
                            // בדיקת תוקף הודעה
                            if (msg.expiresAt != null && msg.expiresAt <= now) {
                                doc.reference.delete()
                                continue
                            }

                            // --- פענוח (Decrypt) ---
                            // שימוש ב-ID של ההודעה (שנשמר בתוך האובייקט) לפענוח
                            val decryptedText = CryptoManager.decrypt(msg.text, msg.id)

                            // מעדכנים את הטקסט למשהו קריא רק עבור התצוגה המקומית
                            messages.add(msg.copy(text = decryptedText))
                        }
                    }
                }
            }
        onDispose { registration.remove() }
    }

    Scaffold(
        containerColor = SecureDarkBackground,
        topBar = {
            Column(modifier = Modifier.background(SecureCardBackground)) {
                TopAppBar(
                    title = { Text(contactName, color = SecureTextPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = SecureTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SecureCardBackground)
                )
                EncryptionBanner()
            }
        },
        bottomBar = {
            SecureChatInput(
                text = messageText,
                onTextChange = { messageText = it },
                isSecretMode = isSecretMode,
                onSecretModeChange = { isSecretMode = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        // 1. יצירת רפרנס ריק כדי לקבל ID מראש
                        val docRef = db.collection("chats").document(chatId).collection("messages").document()
                        val newId = docRef.id

                        // 2. הצפנה (Encrypt) באמצעות ה-ID וה-CryptoManager
                        // זה הקסם! הטקסט המוצפן תלוי ב-ID של ההודעה
                        val encryptedText = CryptoManager.encrypt(messageText, newId)

                        val expiration = if (isSecretMode) System.currentTimeMillis() + 10000 else null // 10 שניות

                        // שימוש במודל שלך עם השדות הנכונים
                        val newMessage = Message(
                            id = newId,
                            text = encryptedText, // שולחים את הטקסט המוצפן!
                            senderId = myName,     // שים לב: senderId ולא sender
                            timestamp = System.currentTimeMillis(),
                            isEncrypted = true,
                            expiresAt = expiration
                        )

                        // 3. שמירה ב-Firestore
                        docRef.set(newMessage)

                        messageText = ""
                        isSecretMode = false
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(messages) { msg ->
                // שים לב: שימוש ב-senderId
                MessageBubble(msg, isMe = msg.senderId == myName)
            }
        }
    }
}

@Composable
fun EncryptionBanner() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = SecureEmerald, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Messages are encrypted end-to-end", color = SecureEmerald, fontSize = 12.sp)
    }
}

@Composable
fun MessageBubble(message: Message, isMe: Boolean) {
    val isExpired = message.expiresAt != null
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isExpired) Color(0xFF550000) else if (isMe) SecureEmerald else SecureCardBackground
    val textColor = if (isMe && !isExpired) Color.Black else SecureTextPrimary

    var timeLeft by remember { mutableLongStateOf(0L) }
    if (isExpired) {
        LaunchedEffect(message.expiresAt) {
            while (System.currentTimeMillis() < message.expiresAt!!) {
                timeLeft = (message.expiresAt - System.currentTimeMillis()) / 1000
                delay(500)
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp).copy(
                    bottomEnd = if (isMe) CornerSize(0.dp) else CornerSize(12.dp),
                    bottomStart = if (isMe) CornerSize(12.dp) else CornerSize(0.dp)
                ))
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(text = message.text, color = textColor, fontSize = 16.sp)
            if (isExpired) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💥 Self-destruct in: ", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${timeLeft}s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun SecureChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    isSecretMode: Boolean,
    onSecretModeChange: (Boolean) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SecureCardBackground).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onSecretModeChange(!isSecretMode) }) {
            Icon(
                imageVector = if (isSecretMode) Icons.Default.Shield else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isSecretMode) Color.Red else SecureTextSecondary
            )
        }

        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = {
                Text(
                    if (isSecretMode) "Self-destruct (10s)..." else "Message...",
                    color = if (isSecretMode) Color.Red else SecureTextSecondary
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = if (isSecretMode) Color.Red else SecureTextPrimary,
                unfocusedTextColor = SecureTextPrimary
            ),
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onSend,
            modifier = Modifier.clip(CircleShape).background(if (isSecretMode) Color.Red else SecureEmerald)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
        }
    }
}