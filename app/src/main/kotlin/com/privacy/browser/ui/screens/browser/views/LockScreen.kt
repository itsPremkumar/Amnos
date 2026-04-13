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
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.privacy.browser.ui.theme.AccentBlue
import com.privacy.browser.ui.theme.DeepSpaceGradient
import com.privacy.browser.ui.theme.GlassBorder
import com.privacy.browser.ui.theme.GlassWhite
import com.privacy.browser.ui.theme.KillRed
import com.privacy.browser.ui.theme.SurfaceGray

@Composable
fun LockScreen(viewModel: BrowserViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(DeepSpaceGradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = GlassWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "Vault Locked",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            
            Text(
                "EPHEMERAL SESSION ACTIVE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AccentBlue,
                letterSpacing = 2.sp,
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
                modifier = Modifier
                    .width(240.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(GlassWhite),
                placeholder = { Text("Enter 4-Digit PIN", color = Color.Gray) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            
            Spacer(Modifier.height(64.dp))
            
            Button(
                onClick = { viewModel.killSwitch() },
                colors = ButtonDefaults.buttonColors(containerColor = KillRed.copy(alpha = 0.1f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, KillRed.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    "EMERGENCY WIPE & EXIT",
                    color = KillRed,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
