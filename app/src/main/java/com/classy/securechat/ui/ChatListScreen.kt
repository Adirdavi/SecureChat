package com.classy.securechat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classy.securechat.data.RSAKeyManager
import com.classy.securechat.data.UserRepository
import com.classy.securechat.data.UserSharedPreferences
import com.classy.securechat.model.Message
import com.classy.securechat.model.User
import com.classy.securechat.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

val ListAvatarColors = listOf(
    Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
    Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF3B82F6),
    Color(0xFF8B5CF6), Color(0xFFEC4899)
)

fun getListUserColor(name: String): Color {
    val index = abs(name.hashCode()) % ListAvatarColors.size
    return ListAvatarColors[index]
}

data class UserChatDisplay(
    val user: User,
    val lastMessageTime: Long = 0,
    val lastMessageText: String = "",
    val hasUnread: Boolean = false
)

@Composable
fun ChatListScreen(onChatClick: (String) -> Unit, onLogout: () -> Unit) {
    var allUsers by remember { mutableStateOf(listOf<User>()) }
    var chatDisplays by remember { mutableStateOf(listOf<UserChatDisplay>()) }
    var searchQuery by remember { mutableStateOf("") }

    val myId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val myName = UserSharedPreferences.myName ?: ""
    val db = FirebaseFirestore.getInstance()

    val searchBarColor = Color(0xFF1F2937)
    val accentGreen = Color(0xFF10B981)

    LaunchedEffect(Unit) {
        UserRepository.getAllUsers { users ->
            val otherUsers = users.filter { it.userId != myId }
            allUsers = otherUsers

            if (chatDisplays.isEmpty()) {
                chatDisplays = otherUsers.map { UserChatDisplay(it) }
                    .sortedBy { it.user.displayName }
            }
        }
    }

    DisposableEffect(allUsers) {
        val listeners = mutableListOf<ListenerRegistration>()

        if (allUsers.isNotEmpty()) {
            allUsers.forEach { user ->
                val chatId = listOf(myName, user.displayName).sorted().joinToString("_")

                val registration = db.collection("chats").document(chatId).collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null && !snapshot.isEmpty) {
                            val doc = snapshot.documents[0]
                            val msg = doc.toObject(Message::class.java)
                            if (msg != null) {
                                val unread = !msg.isRead && msg.senderUid != myId
                                val isMe = msg.senderUid == myId

                                val textToDecrypt = if (isMe && msg.textForSender.isNotEmpty()) msg.textForSender else msg.text
                                val previewText = try {
                                    if (msg.isSecret && !unread) "Secret Message 🔒"
                                    else RSAKeyManager.decrypt(textToDecrypt)
                                } catch (e: Exception) {
                                    "Encrypted Message"
                                }

                                chatDisplays = chatDisplays.map { display ->
                                    if (display.user.userId == user.userId) {
                                        display.copy(
                                            lastMessageTime = msg.timestamp,
                                            lastMessageText = previewText,
                                            hasUnread = unread
                                        )
                                    } else display
                                }.sortedWith(
                                    compareByDescending<UserChatDisplay> { it.lastMessageTime > 0 }
                                        .thenByDescending { it.lastMessageTime }
                                        .thenBy { it.user.displayName }
                                )
                            }
                        }
                    }
                listeners.add(registration)
            }
        }

        onDispose {
            listeners.forEach { it.remove() }
        }
    }

    val filteredList = if (searchQuery.isBlank()) {
        chatDisplays
    } else {
        chatDisplays.filter {
            it.user.displayName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = Color(0xFF111827),
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color(0xFF111827))
                    .statusBarsPadding()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = accentGreen, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SecureChat", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.size(40.dp).background(Color(0xFF374151), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color.LightGray)
                    }
                }

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    placeholder = { Text("Search...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = searchBarColor,
                        unfocusedContainerColor = searchBarColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = accentGreen,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                Text(
                    "ALL MESSAGES",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp, top = 16.dp)
                )
            }

            if (filteredList.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No chats found", color = Color.Gray)
                    }
                }
            }

            items(filteredList) { item ->
                val userColor = remember(item.user.displayName) { getListUserColor(item.user.displayName) }
                ChatListItem(item, userColor) { onChatClick(item.user.displayName) }
            }
        }
    }
}

@Composable
fun ChatListItem(item: UserChatDisplay, avatarColor: Color, onClick: () -> Unit) {
    val accentGreen = Color(0xFF10B981)

    val timeString = if (item.lastMessageTime > 0) {
        val diff = System.currentTimeMillis() - item.lastMessageTime
        if (diff < 24 * 60 * 60 * 1000) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.lastMessageTime))
        } else {
            SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(item.lastMessageTime))
        }
    } else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(item.user.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.user.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (item.hasUnread) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.Lock, contentDescription = null, tint = accentGreen, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (item.lastMessageText.isNotBlank()) item.lastMessageText else "Start a new conversation",
                color = if (item.hasUnread) Color.White else Color.Gray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (item.hasUnread) FontWeight.Medium else FontWeight.Normal
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(timeString, color = if (item.hasUnread) accentGreen else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            if (item.hasUnread) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(accentGreen))
            }
        }
    }
}