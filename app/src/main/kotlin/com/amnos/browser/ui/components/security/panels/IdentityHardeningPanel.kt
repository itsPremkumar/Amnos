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
import com.amnos.browser.ui.components.security.SecurityToggle

@Composable
fun IdentityHardeningPanel(viewModel: BrowserViewModel) {
    val policy = viewModel.privacyPolicy.value
    
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text("IDENTITY & PURGE CONTROL", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(16.dp))

        Text("Active Profile: ${viewModel.sessionLabel.value}", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))

        SecurityToggle(
            title = "Identity Reset on Refresh",
            description = "Regenerates profile/fingerprint on every page reload.",
            checked = policy.identityResetOnRefresh,
            onCheckedChange = viewModel::toggleResetIdentityOnRefresh
        )

        SecurityToggle(
            title = "Sandbox Isolation",
            description = "Enables intent jail and forensic wipe triggers.",
            checked = viewModel.isSandboxEnabled.value,
            onCheckedChange = viewModel::toggleSandboxEnabled
        )

        SecurityToggle(
            title = "Forensic RAM Scramble",
            description = "Best effort: Wipes memory buffers before process kill.",
            checked = policy.purgeForensicRamScramble,
            onCheckedChange = { /* Connected to PrivacyPolicy */ }
        )
    }
}
