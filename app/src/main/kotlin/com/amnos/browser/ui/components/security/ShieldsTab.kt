package com.amnos.browser.ui.components.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.core.security.JavaScriptMode
import com.amnos.browser.core.security.FirewallLevel
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.AccentBlue
import com.amnos.browser.ui.theme.GlassBorder
import com.amnos.browser.ui.theme.KillRed

@Composable
fun ShieldsTab(viewModel: BrowserViewModel, isWide: Boolean) {
    if (isWide) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                ActiveDefenseCard(viewModel)
                Spacer(Modifier.height(16.dp))
                StatusCard(
                    title = "Transport Matrix",
                    lines = listOf(
                        "Proxy Tunnel: ${viewModel.proxyStatus.value}",
                        "DNS-over-HTTPS: ${viewModel.dohStatus.value}",
                        "WebRTC Block: ${viewModel.webRtcStatus.value}"
                    )
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                SecurityControlsGroup(viewModel)
            }
        }
    } else {
        Column {
            ActiveDefenseCard(viewModel)
            Spacer(Modifier.height(16.dp))
            SecurityControlsGroup(viewModel)
        }
    }
}

@Composable
fun ActiveDefenseCard(viewModel: BrowserViewModel) {
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
                Text("NETWORK SHIELDS", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(
                    "${viewModel.blockedTrackersCount.intValue} Pathogens Blocked",
                    color = Color.White,
                    fontSize = 18.sp,
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
fun SecurityControlsGroup(viewModel: BrowserViewModel) {
    val policy = viewModel.privacyPolicy.value
    Column {
        FirewallLevelSelector(
            selectedLevel = viewModel.firewallLevel.value,
            onLevelSelected = viewModel::setFirewallLevel
        )

        Spacer(Modifier.height(16.dp))

        JavaScriptModeSelector(
            selectedMode = viewModel.javaScriptMode.value,
            onModeSelected = viewModel::setJavaScriptMode
        )

        Spacer(Modifier.height(8.dp))

        FingerprintLevelSelector(
            selectedLevel = viewModel.fingerprintProtectionLevel.value,
            onLevelSelected = viewModel::setFingerprintProtectionLevel
        )

        Spacer(Modifier.height(16.dp))

        SecurityToggle(
            title = "HTTPS Enforcement",
            description = "Blocks all insecure cleartext traffic.",
            checked = policy.networkHttpsOnly,
            onCheckedChange = viewModel::toggleHttpsOnly
        )

        SecurityToggle(
            title = "Siloed Identifiers",
            description = "Aggressive first-party isolation for all state.",
            checked = policy.filterStrictFirstPartyIsolation,
            onCheckedChange = viewModel::toggleStrictFirstPartyIsolation
        )

        SecurityToggle(
            title = "WebGL Randomization",
            description = "Spoofs GPU surfaces to prevent profiling.",
            checked = viewModel.isWebGLEnabled.value,
            onCheckedChange = viewModel::toggleWebGL
        )
    }
}

@Composable
fun StatusCard(
    title: String,
    lines: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            lines.forEach { line ->
                Text(line, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JavaScriptModeSelector(
    selectedMode: JavaScriptMode,
    onModeSelected: (JavaScriptMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text("JavaScript Mode", color = Color.White, fontWeight = FontWeight.Medium)
        Text("Choose between compatibility and attack-surface reduction.", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JavaScriptMode.values().forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(mode.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FingerprintLevelSelector(
    selectedLevel: FingerprintProtectionLevel,
    onLevelSelected: (FingerprintProtectionLevel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text("Fingerprint Protection", color = Color.White, fontWeight = FontWeight.Medium)
        Text("Balanced favors compatibility. Strict normalizes values and timing more aggressively.", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FingerprintProtectionLevel.values().forEach { level ->
                FilterChip(
                    selected = selectedLevel == level,
                    onClick = { onLevelSelected(level) },
                    label = { Text(level.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun SecurityToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(description, color = Color.Gray, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentBlue,
                checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
            )
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallLevelSelector(
    selectedLevel: FirewallLevel,
    onLevelSelected: (FirewallLevel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text("Firewall Level", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text("Paranoid is Zero-Trust. Balanced allows gated navigation.", color = Color.Gray, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FirewallLevel.values().forEach { level ->
                FilterChip(
                    selected = selectedLevel == level,
                    onClick = { onLevelSelected(level) },
                    label = { Text(level.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}
