package com.classy.securechat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classy.securechat.data.UserSharedPreferences
import com.classy.securechat.ui.theme.*

@Composable
fun ChatListScreen(onChatClick: (String) -> Unit, onLogout: () -> Unit) {
    val contacts = listOf("Alice", "Bob", "Team Leader", "David")

    Scaffold(
        containerColor = SecureDarkBackground,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Chats", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SecureTextPrimary)
                    Text("Hello, ${UserSharedPreferences.myName}", fontSize = 14.sp, color = SecureEmerald)
                }
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = SecureTextSecondary
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(contacts) { name ->
                ChatListItem(name, onClick = { onChatClick(name) })
            }
        }
    }
}

@Composable
fun ChatListItem(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(SecureCardBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1), color = SecureEmerald, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(name, color = SecureTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
    HorizontalDivider(thickness = 0.5.dp, color = SecureBorder)
}