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
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import com.amnos.browser.ui.components.keyboard.GhostTextField
import com.amnos.browser.ui.components.keyboard.KeyboardViewModel

@Composable
fun AddressField(
    viewModel: BrowserViewModel,
    keyboardViewModel: KeyboardViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        color = GlassWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isHttps = viewModel.urlInput.value.startsWith("https")
            Icon(
                imageVector = if (isHttps) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Security",
                tint = if (isHttps) AccentBlue else KillRed,
                modifier = Modifier.size(14.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            GhostTextField(
                value = viewModel.urlInput.value,
                onValueChange = { viewModel.urlInput.value = it },
                keyboardViewModel = keyboardViewModel,
                modifier = Modifier.weight(1f),
                placeholder = "Search or type URL",
                onSearch = {
                    viewModel.navigate(viewModel.urlInput.value)
                    focusManager.clearFocus()
                }
            )
        }
    }
}
