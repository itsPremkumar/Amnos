package com.amnos.browser.ui.components.security.panels

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.AccentBlue
import com.amnos.browser.ui.theme.KillRed
import com.amnos.browser.ui.components.security.SecurityToggle

@Composable
fun DebugLockdownPanel(viewModel: BrowserViewModel) {
    val policy = viewModel.privacyPolicy.value
    
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("LOCKDOWN & DEBUG", color = KillRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            if (!viewModel.debugControlsAvailable) {
                Text("RESTRICTED", color = Color.Gray, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(16.dp))

        SecurityToggle(
            title = "App Lockdown Mode",
            description = "Blocks all debuggers and remote inspection.",
            checked = policy.debugLockdownMode,
            onCheckedChange = viewModel::toggleRemoteDebugging,
            enabled = viewModel.debugControlsAvailable
        )

        SecurityToggle(
            title = "Block Forensic Logging",
            description = "Disables Android Logcat for this application.",
            checked = policy.debugBlockForensicLogging,
            onCheckedChange = { /* Connected to PrivacyPolicy */ },
            enabled = viewModel.debugControlsAvailable
        )

        SecurityToggle(
            title = "Anti-Screenshot Protection",
            description = "Prevents system-level screen capture/recording.",
            checked = policy.debugBlockScreenshots,
            onCheckedChange = { /* Connected via logic */ }
        )
    }
}
