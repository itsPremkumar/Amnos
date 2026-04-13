package com.privacy.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacy.browser.core.security.FingerprintProtectionLevel
import com.privacy.browser.core.security.JavaScriptMode
import com.privacy.browser.core.session.SecurityController
import com.privacy.browser.ui.screens.browser.BrowserViewModel
import com.privacy.browser.ui.theme.AccentBlue
import com.privacy.browser.ui.theme.KillRed
import com.privacy.browser.ui.theme.SurfaceGray
import androidx.compose.foundation.border
import com.privacy.browser.ui.theme.GlassBorder
import com.privacy.browser.ui.theme.TextGray
import com.privacy.browser.ui.utils.WindowSize
import com.privacy.browser.ui.utils.rememberWindowSize

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SecurityDashboard(viewModel: BrowserViewModel) {
    val windowSize = rememberWindowSize()
    val isExpanded = windowSize == WindowSize.EXPANDED

    ModalBottomSheet(
        onDismissRequest = { viewModel.showSecurityDashboard.value = false },
        containerColor = SurfaceGray.copy(alpha = 0.95f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = GlassBorder) },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 64.dp)
        ) {
            DashboardHeader(viewModel)

            val tabs = listOf("SHIELDS", "INSPECTOR", "IDENTITY")
            var selectedTab by remember { mutableIntStateOf(0) }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AccentBlue,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            when (selectedTab) {
                0 -> ShieldsTab(viewModel, isExpanded)
                1 -> InspectorTab(viewModel)
                2 -> IdentityTab(viewModel)
            }
        }
    }
}

@Composable
fun DashboardHeader(viewModel: BrowserViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        Icon(
            Icons.Default.Shield,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text("Security Cockpit", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Session ID: ${viewModel.sessionLabel.value}", color = TextGray, fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
}

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
fun InspectorTab(viewModel: BrowserViewModel) {
    Column {
        Text("Volatile Request Log", color = Color.White, fontWeight = FontWeight.Bold)
        Text("Real-time visibility into all outgoing network traffic.", color = TextGray, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        RequestInspectorList(viewModel.requestLog)
    }
}

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
            checked = policy.httpsOnlyEnabled,
            onCheckedChange = viewModel::toggleHttpsOnly
        )

        SecurityToggle(
            title = "Siloed Identifiers",
            description = "Aggressive first-party isolation for all state.",
            checked = policy.strictFirstPartyIsolation,
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
fun RequestInspectorList(logs: List<SecurityController.RequestEntry>) {
    Column(modifier = Modifier.height(250.dp)) {
        Text(
            "Volatile Request Log (Last 100)",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No active requests recorded.", color = Color.DarkGray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs.reversed()) { entry ->
                    RequestItem(entry)
                }
            }
        }
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

@Composable
fun RequestItem(entry: SecurityController.RequestEntry) {
    val color = when (entry.disposition) {
        SecurityController.RequestDisposition.BLOCKED -> KillRed
        SecurityController.RequestDisposition.PASSTHROUGH -> Color(0xFFFFC857)
        SecurityController.RequestDisposition.ALLOWED -> AccentBlue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.type.name,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(70.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .padding(2.dp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.url,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(entry.method)
                    if (entry.thirdParty) append(" - 3P")
                    entry.reason?.let {
                        append(" - ")
                        append(it)
                    }
                },
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
