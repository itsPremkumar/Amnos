package com.amnos.browser.ui.components.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.AccentBlue
import com.amnos.browser.ui.theme.SurfaceGray
import com.amnos.browser.ui.theme.TextGray

@Composable
fun FirewallTab(viewModel: BrowserViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "FIREWALL STATUS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextGray
        )
        
        Spacer(Modifier.height(16.dp))
        
        Surface(
            color = SurfaceGray.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Ghost Engine Active",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    "The Ghost Firewall is actively intercepting and analyzing all network traffic at the packet level. No data leaves this sandbox without explicit policy clearance.",
                    color = TextGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                
                Spacer(Modifier.height(20.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FirewallStatItem("Allowed", viewModel.firewallAllowedDomains.size.toString())
                    FirewallStatItem("Blocked", viewModel.firewallBlockedDomains.size.toString())
                    FirewallStatItem("Requests", viewModel.requestLog.size.toString())
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = { 
                viewModel.showSecurityDashboard.value = false
                viewModel.openFirewall() 
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("OPEN FULL FIREWALL", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun FirewallStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
