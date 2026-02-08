package com.classy.securechat.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classy.securechat.ui.theme.*

@Composable
fun SecureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: @Composable (() -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = SecureTextSecondary) },
        leadingIcon = icon,
        visualTransformation = visualTransformation,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SecureCardBackground,
            unfocusedContainerColor = SecureCardBackground,
            focusedBorderColor = SecureEmerald,
            unfocusedBorderColor = SecureBorder,
            focusedTextColor = SecureTextPrimary,
            unfocusedTextColor = SecureTextPrimary,
            cursorColor = SecureEmerald
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SecureButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean = true
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) SecureEmerald else SecureCardBackground
        ),
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(8.dp),
        border = if (!isPrimary) androidx.compose.foundation.BorderStroke(1.dp, SecureBorder) else null
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPrimary) Color.Black else SecureTextPrimary
        )
    }
}