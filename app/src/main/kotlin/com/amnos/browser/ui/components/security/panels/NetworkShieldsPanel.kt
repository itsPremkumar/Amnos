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
import com.amnos.browser.ui.components.security.FirewallLevelSelector
import com.amnos.browser.ui.components.security.SecurityToggle
import com.amnos.browser.core.security.FirewallLevel

@Composable
fun NetworkShieldsPanel(viewModel: BrowserViewModel) {
    val policy = viewModel.privacyPolicy.value
    
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text("NETWORK & FILTER ENGINE", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(16.dp))

        FirewallLevelSelector(
            selectedLevel = viewModel.firewallLevel.value,
            onLevelSelected = viewModel::setFirewallLevel
        )

        Spacer(Modifier.height(12.dp))

        SecurityToggle(
            title = "HTTPS Enforcement",
            description = "Forces encrypted connections for all traffic.",
            checked = policy.networkHttpsOnly,
            onCheckedChange = viewModel::toggleHttpsOnly
        )

        SecurityToggle(
            title = "Tracker Suppression",
            description = "Blocks known analytics and advertising pathogens.",
            checked = policy.filterBlockTrackers,
            onCheckedChange = { /* Connected to PrivacyPolicy in ViewModel */ }
        )

        SecurityToggle(
            title = "First-Party Isolation",
            description = "Siloes all storage to current domain context.",
            checked = policy.filterStrictFirstPartyIsolation,
            onCheckedChange = viewModel::toggleStrictFirstPartyIsolation
        )
        
        SecurityToggle(
            title = "Third-Party Requests",
            description = "EXPERIMENTAL: Blocks all cross-domain assets.",
            checked = policy.filterBlockThirdPartyRequests,
            onCheckedChange = viewModel::toggleThirdPartyBlocking
        )
    }
}
