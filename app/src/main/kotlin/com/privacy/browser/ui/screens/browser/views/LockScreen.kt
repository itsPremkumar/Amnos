package com.privacy.browser.ui.screens.browser.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacy.browser.ui.screens.browser.BrowserViewModel
import com.privacy.browser.ui.theme.AccentBlue
import com.privacy.browser.ui.theme.SurfaceGray

@Composable
fun LockScreen(viewModel: BrowserViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceGray)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Incognito Session Locked",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            "Enter PIN to resume your private session",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(Modifier.height(48.dp))
        
        OutlinedTextField(
            value = viewModel.pinInput.value,
            onValueChange = { 
                viewModel.pinInput.value = it.take(4)
                if (it == viewModel.userPin) {
                    viewModel.isLocked.value = false
                    viewModel.pinInput.value = ""
                }
            },
            label = { Text("Enter 4-Digit PIN") },
            placeholder = { Text("Default: 1111") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier.width(200.dp)
        )
        
        Spacer(Modifier.height(24.dp))
        
        TextButton(onClick = { viewModel.killSwitch() }) {
            Text("Emergency Wipe & Exit", color = Color.Red.copy(alpha = 0.7f))
        }
    }
}
