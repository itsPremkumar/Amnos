package com.amnos.browser.ui.components.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.AccentBlue
import com.amnos.browser.ui.theme.GlassBorder
import com.amnos.browser.ui.theme.KillRed
import com.amnos.browser.ui.theme.TextGray

@Composable
fun IdentityTab(viewModel: BrowserViewModel) {
    Column {
        Text("Digital Persona", color = Color.White, fontWeight = FontWeight.Bold)
        Text("Your ephemeral identity for this session.", color = TextGray, fontSize = 12.sp)
        
        Spacer(Modifier.height(20.dp))

        IdentityMetricsCard(viewModel)
        
        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Connection Masking", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("ACTIVE", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(12.dp))
                IdentityDetailRow("Public IP Status", "Proxy Masked", true)
                IdentityDetailRow("DNS Resolution", "Encrypted (DoH)", true)
                IdentityDetailRow("WebRTC Leak Protection", "Enabled (Refined)", true)
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Digital Fingerprint", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(12.dp))
                IdentityDetailRow("User-Agent Spoofing", "Chrome 122 / Linux (Hardened)", true)
                IdentityDetailRow("Hardware Concurrency", "Normalised (8 Cores)", true)
                IdentityDetailRow("Canvas Fingerprinting", "Noise Injected", true)
                IdentityDetailRow("WebGL Metadata", "Randomized", true)
            }
        }
    }
}

@Composable
fun IdentityMetricsCard(viewModel: BrowserViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("SESSION TOKEN", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(viewModel.sessionLabel.value, color = Color.White, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("SECURITY LEVEL", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(
                text = when(viewModel.fingerprintProtectionLevel.value) {
                    FingerprintProtectionLevel.STRICT -> "MAXIMUM"
                    else -> "BALANCED"
                },
                color = AccentBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun IdentityDetailRow(label: String, value: String, isProtected: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = if (isProtected) Color.White else KillRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        if (isProtected) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.VisibilityOff, null, tint = AccentBlue.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
        }
    }
}
