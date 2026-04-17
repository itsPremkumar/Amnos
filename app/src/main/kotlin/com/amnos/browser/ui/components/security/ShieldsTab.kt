package com.amnos.browser.ui.components.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.amnos.browser.ui.theme.GlassBorder
import com.amnos.browser.ui.theme.KillRed
import com.amnos.browser.ui.components.security.panels.NetworkShieldsPanel
import com.amnos.browser.ui.components.security.panels.IdentityHardeningPanel
import com.amnos.browser.ui.components.security.panels.AdvancedHardwarePanel
import com.amnos.browser.ui.components.security.panels.DebugLockdownPanel

@Composable
fun ShieldsTab(viewModel: BrowserViewModel, isWide: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ActiveDefenseHeader(viewModel)
        
        Spacer(Modifier.height(24.dp))

        if (isWide) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    NetworkShieldsPanel(viewModel)
                    IdentityHardeningPanel(viewModel)
                    StatusMatrixCard(viewModel)
                }
                Column(modifier = Modifier.weight(1f)) {
                    AdvancedHardwarePanel(viewModel)
                    DebugLockdownPanel(viewModel)
                }
            }
        } else {
            Column {
                NetworkShieldsPanel(viewModel)
                IdentityHardeningPanel(viewModel)
                AdvancedHardwarePanel(viewModel)
                DebugLockdownPanel(viewModel)
                StatusMatrixCard(viewModel)
            }
        }
    }
}

@Composable
fun ActiveDefenseHeader(viewModel: BrowserViewModel) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("SHIELD ACTIVITY", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(
                    "${viewModel.blockedTrackersCount.intValue} Pathogens Blocked",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Box(
                modifier = Modifier
                    .background(KillRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("GHOST", color = KillRed, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun StatusMatrixCard(viewModel: BrowserViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("INFRASTRUCTURE STATUS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(16.dp))
            
            StatusLine("Proxy Tunnel", viewModel.proxyStatus.value)
            StatusLine("DNS-over-HTTPS", viewModel.dohStatus.value)
            StatusLine("WebRTC Shield", viewModel.webRtcStatus.value)
            StatusLine("WebSocket Shield", viewModel.webSocketStatus.value)
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label: ", color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
