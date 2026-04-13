package com.amnos.browser.ui.components.browser

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.AccentBlue
import com.amnos.browser.ui.theme.GlassWhite
import com.amnos.browser.ui.theme.KillRed
import com.amnos.browser.ui.theme.TextGray

@Composable
fun AddressField(
    viewModel: BrowserViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    TextField(
        value = viewModel.urlInput.value,
        onValueChange = { viewModel.urlInput.value = it },
        modifier = modifier.height(50.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        placeholder = { Text("Search or type URL", fontSize = 14.sp, color = TextGray) },
        singleLine = true,
        shape = RoundedCornerShape(25.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = GlassWhite,
            unfocusedContainerColor = GlassWhite,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White.copy(alpha = 0.8f),
            cursorColor = AccentBlue
        ),
        leadingIcon = {
            val isHttps = viewModel.urlInput.value.startsWith("https")
            Icon(
                imageVector = if (isHttps) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Security",
                tint = if (isHttps) AccentBlue else KillRed,
                modifier = Modifier.size(16.dp)
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            viewModel.navigate(viewModel.urlInput.value)
            focusManager.clearFocus()
        })
    )
}
