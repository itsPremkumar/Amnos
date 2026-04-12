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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SecurityDashboard(viewModel: BrowserViewModel) {
    val policy = viewModel.privacyPolicy.value

    ModalBottomSheet(
        onDismissRequest = { viewModel.showSecurityDashboard.value = false },
        containerColor = SurfaceGray,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Amnos Security Cockpit", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Session ${viewModel.sessionLabel.value}", color = Color.Gray, fontSize = 11.sp)
                }
            }

            val tabs = listOf("SHIELDS", "INSPECTOR", "LOGS")
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
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    Column {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Active Defense", color = Color.Gray, fontSize = 12.sp)
                                    Text(
                                        "${viewModel.blockedTrackersCount.value} Trackers Blocked",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("${viewModel.activeConnections.size} Active Connections", color = Color.Gray, fontSize = 11.sp)
                                    Text("${viewModel.requestLog.size} Volatile Events", color = Color.Gray, fontSize = 11.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(KillRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("GHOST", color = KillRed, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        StatusCard(
                            title = "Transport",
                            lines = listOf(
                                "Proxy: ${viewModel.proxyStatus.value}",
                                "DoH: ${viewModel.dohStatus.value}",
                                "WebRTC: ${viewModel.webRtcStatus.value} (${viewModel.webRtcAttemptCount.value} events)",
                                "WebSocket: ${viewModel.webSocketStatus.value} (${viewModel.webSocketAttemptCount.value} events)"
                            )
                        )

                        JavaScriptModeSelector(
                            selectedMode = viewModel.javaScriptMode.value,
                            onModeSelected = viewModel::setJavaScriptMode
                        )

                        FingerprintLevelSelector(
                            selectedLevel = viewModel.fingerprintProtectionLevel.value,
                            onLevelSelected = viewModel::setFingerprintProtectionLevel
                        )

                        SecurityToggle(
                            title = "HTTPS-Only",
                            description = "Upgrade cleartext links and block insecure loads.",
                            checked = policy.httpsOnlyEnabled,
                            onCheckedChange = viewModel::toggleHttpsOnly
                        )

                        SecurityToggle(
                            title = "Third-Party Blocking",
                            description = "Blocks third-party requests and remote scripts.",
                            checked = policy.blockThirdPartyRequests,
                            onCheckedChange = viewModel::toggleThirdPartyBlocking
                        )

                        SecurityToggle(
                            title = "Inline Script Shield",
                            description = "Applies CSP and blocks dynamic code paths in restricted mode.",
                            checked = policy.blockInlineScripts,
                            onCheckedChange = viewModel::toggleInlineScriptBlocking
                        )

                        SecurityToggle(
                            title = "WebSocket Shield",
                            description = "Blocks live socket channels that can leak identifiers.",
                            checked = policy.blockWebSockets,
                            onCheckedChange = viewModel::toggleWebSockets
                        )

                        SecurityToggle(
                            title = "First-Party Isolation",
                            description = "Rebuild the browsing silo when top-level site identity changes.",
                            checked = policy.strictFirstPartyIsolation,
                            onCheckedChange = viewModel::toggleStrictFirstPartyIsolation
                        )

                        SecurityToggle(
                            title = "WebGL Spoofing",
                            description = "Disable or spoof GPU surfaces to reduce fingerprint entropy.",
                            checked = viewModel.isWebGLEnabled.value,
                            onCheckedChange = viewModel::toggleWebGL
                        )

                        SecurityToggle(
                            title = "Identity Reset On Refresh",
                            description = "Rebuilds the current tab with a fresh tab UUID and profile.",
                            checked = policy.resetIdentityOnRefresh,
                            onCheckedChange = viewModel::toggleResetIdentityOnRefresh
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            viewModel.privacyWarning.value,
                            color = Color(0xFFFFD166),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Amnos v1.2.0 - Loopback privacy controls active",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
                        )
                    }
                }
                1 -> {
                    RequestInspectorList(viewModel.requestLog)
                }
                2 -> {
                    Column {
                        Text("System Diagnostics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Sub-surface process execution logs.", color = Color.Gray, fontSize = 12.sp)
                        
                        Spacer(Modifier.height(12.dp))
                        
                        // Internal Logs List
                        val internalLogs = viewModel.internalLogs
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            items(internalLogs) { entry ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (entry.level) {
                                                        "ERROR" -> KillRed
                                                        "WARN" -> Color(0xFFFFD166)
                                                        "DEBUG" -> AccentBlue
                                                        else -> Color.Gray
                                                    }
                                                )
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "[${entry.tag}]",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        entry.message,
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        SecurityToggle(
                            title = "Remote WebView Debugging",
                            description = "Enables USB debugging via chrome://inspect on a PC. WARNING: REDUCES PRIVACY.",
                            checked = viewModel.enableRemoteDebugging.value,
                            onCheckedChange = viewModel::toggleRemoteDebugging
                        )

                        SecurityToggle(
                            title = "Relax Security for Diagnostics",
                            description = "Master bypass for CSP, trackers, and proxy policies to isolate engine issues.",
                            checked = viewModel.forceRelaxSecurityForDebug.value,
                            onCheckedChange = viewModel::toggleForceRelaxSecurity
                        )
                    }
                }
            }
        }
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
