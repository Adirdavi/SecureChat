package com.classy.securechat.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.classy.securechat.data.RSAKeyManager
import com.classy.securechat.data.UserSharedPreferences
import com.classy.securechat.model.Message
import com.classy.securechat.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

val ChatAvatarColors = listOf(
    Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
    Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF3B82F6),
    Color(0xFF8B5CF6), Color(0xFFEC4899)
)

fun getChatUserColor(name: String): Color {
    val index = abs(name.hashCode()) % ChatAvatarColors.size
    return ChatAvatarColors[index]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(contactName: String, onBackClick: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    var isSecretMode by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<Message>() }
    val listState = rememberLazyListState()

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    val myName = UserSharedPreferences.myName ?: "Unknown"
    val myUid = auth.currentUser?.uid ?: ""

    val chatId = listOf(myName, contactName).sorted().joinToString("_")
    val contactColor = remember(contactName) { getChatUserColor(contactName) }

    var contactPublicKey by remember { mutableStateOf("") }
    var myPublicKey by remember { mutableStateOf("") }

    LaunchedEffect(contactName) {
        db.collection("users").whereEqualTo("displayName", contactName).get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    contactPublicKey = docs.documents[0].getString("publicKey") ?: ""
                }
            }
        db.collection("users").document(myUid).get()
            .addOnSuccessListener { doc ->
                myPublicKey = doc.getString("publicKey") ?: ""
            }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    DisposableEffect(chatId) {
        val registration = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatScreen", "Listen failed", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    messages.clear()
                    val now = System.currentTimeMillis()

                    for (doc in snapshot.documents) {
                        try {
                            var msg = doc.toObject(Message::class.java)

                            if (msg != null) {
                                if (msg.expiresAt != null && msg.expiresAt <= now) {
                                    doc.reference.delete()
                                    continue
                                }

                                val isMe = msg.senderUid == myUid
                                val isReceiver = msg.senderUid != myUid && msg.senderUid.isNotEmpty()

                                if (isReceiver) {
                                    if (!msg.isRead) {
                                        doc.reference.update("isRead", true)
                                    }

                                    if (msg.isSecret && msg.expiresAt == null) {
                                        val expiryTime = System.currentTimeMillis() + 20000
                                        doc.reference.update("expiresAt", expiryTime)
                                    }
                                }

                                val textToDecrypt = if (isMe && msg.textForSender.isNotEmpty()) msg.textForSender else msg.text
                                val decryptedText = try {
                                    RSAKeyManager.decrypt(textToDecrypt)
                                } catch (e: Exception) {
                                    "Error decrypting"
                                }

                                messages.add(msg.copy(text = decryptedText))
                            }
                        } catch (e: Exception) {
                            Log.e("ChatScreen", "Skipped corrupted message: ${doc.id}", e)
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
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(contactColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contactName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = contactName,
                                color = SecureTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
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
                        val docRef = db.collection("chats").document(chatId).collection("messages").document()
                        val newId = docRef.id

                        val encryptedForReceiver = if (contactPublicKey.isNotEmpty()) {
                            RSAKeyManager.encrypt(messageText, contactPublicKey)
                        } else {
                            "NoKey"
                        }

                        val encryptedForMe = if (myPublicKey.isNotEmpty()) {
                            RSAKeyManager.encrypt(messageText, myPublicKey)
                        } else {
                            "NoKey"
                        }

                        val newMessage = Message(
                            id = newId,
                            text = encryptedForReceiver,
                            textForSender = encryptedForMe,
                            senderId = myName,
                            senderUid = myUid,
                            timestamp = System.currentTimeMillis(),
                            isEncrypted = true,
                            isSecret = isSecretMode,
                            expiresAt = null,
                            isRead = false
                        )
                        docRef.set(newMessage)
                        messageText = ""
                        isSecretMode = false
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp)
            ) {
                items(messages) { msg ->
                    val isMe = if (msg.senderUid.isNotEmpty()) msg.senderUid == myUid else msg.senderId == myName
                    MessageBubble(
                        message = msg,
                        isMe = isMe,
                        onDelete = {
                            db.collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .document(msg.id)
                                .delete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EncryptionBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = SecureEmerald, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Messages are E2E RSA Encrypted", color = SecureEmerald, fontSize = 12.sp)
    }
}

@Composable
fun MessageBubble(message: Message, isMe: Boolean, onDelete: () -> Unit) {
    val isTimerRunning = message.expiresAt != null
    val isPendingSecret = message.isSecret && !isTimerRunning

    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart

    val bubbleColor = when {
        isTimerRunning -> Color(0xFF550000)
        isPendingSecret && isMe -> Color.Black
        isPendingSecret && !isMe -> Color(0xFF550000)
        isMe -> SecureEmerald
        else -> SecureCardBackground
    }

    val borderColor = if (isPendingSecret && isMe) BorderStroke(2.dp, Color.Red) else null
    val textColor = if (isMe && !isPendingSecret && !isTimerRunning) Color.Black else Color.White

    val timeColor = if (isMe && !isPendingSecret && !isTimerRunning) Color.DarkGray else Color.LightGray

    var timeLeft by remember { mutableLongStateOf(0L) }

    val timeString = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    if (isTimerRunning) {
        LaunchedEffect(message.expiresAt) {
            while (System.currentTimeMillis() < message.expiresAt!!) {
                timeLeft = (message.expiresAt - System.currentTimeMillis()) / 1000
                delay(200)
            }
            onDelete()
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
                .then(if (borderColor != null) Modifier.border(borderColor, RoundedCornerShape(12.dp).copy(
                    bottomEnd = if (isMe) CornerSize(0.dp) else CornerSize(12.dp),
                    bottomStart = if (isMe) CornerSize(12.dp) else CornerSize(0.dp)
                )) else Modifier)
                .padding(12.dp)
        ) {
            Text(text = message.text, color = textColor, fontSize = 16.sp)

            Text(
                text = timeString,
                fontSize = 10.sp,
                color = timeColor,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
            )

            if (isTimerRunning) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text("💥 Disappearing in: ", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${timeLeft}s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            } else if (isPendingSecret && isMe) {
                Text("Waiting for read...", color = Color.Red, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .background(SecureCardBackground)
            .padding(16.dp),
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
                    if (isSecretMode) "Self-destruct (20s)..." else "Message...",
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