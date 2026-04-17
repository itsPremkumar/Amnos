package com.amnos.browser.ui.screens.browser.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.*
import com.amnos.browser.core.security.AmnosSandboxMode
import com.amnos.browser.core.model.RequestEntry
import com.amnos.browser.core.model.RequestDisposition
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Composable
fun FirewallDashboard(viewModel: BrowserViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGray)
            .padding(16.dp)
    ) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::closeFirewall) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "GHOST FIREWALL",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = if (viewModel.sandboxMode.value == AmnosSandboxMode.PARANOID) KillRed else AccentBlue,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    viewModel.sandboxMode.value.name,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // QUICK CONTROLS
        Text("SANDBOX MODE", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FirewallModeButton("OPEN", viewModel.sandboxMode.value == AmnosSandboxMode.OPEN) {
                viewModel.setSandboxMode(AmnosSandboxMode.OPEN)
            }
            FirewallModeButton("BALANCED", viewModel.sandboxMode.value == AmnosSandboxMode.BALANCED) {
                viewModel.setSandboxMode(AmnosSandboxMode.BALANCED)
            }
            FirewallModeButton("PARANOID", viewModel.sandboxMode.value == AmnosSandboxMode.PARANOID) {
                viewModel.setSandboxMode(AmnosSandboxMode.PARANOID)
            }
        }

        Spacer(Modifier.height(32.dp))

        Spacer(Modifier.height(32.dp))
        
        // TAB SWITCHER
        var selectedTab by remember { mutableStateOf(0) }
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = AccentBlue,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AccentBlue
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("TRAFFIC", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("CONSOLE", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(Modifier.height(16.dp))

        if (selectedTab == 0) {
            // TRAFFIC LOGS
            val logs = viewModel.requestLog
            if (logs.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No network activity captured.", color = TextGray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs.reversed()) { log ->
                        RequestLogItem(log, onBlock = { host -> 
                            viewModel.addFirewallRule(host, false)
                        }, onAllow = { host ->
                            viewModel.addFirewallRule(host, true)
                        })
                    }
                }
            }
        } else {
            // SYSTEM CONSOLE LOGS
            val internalLogs = viewModel.internalLogs
            if (internalLogs.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Console is empty.", color = TextGray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(internalLogs) { entry ->
                        InternalLogItem(entry)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        
        // ACTIVE RULES SUMMARY
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceGray,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("ACTIVE RULES", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${viewModel.firewallAllowedDomains.size} Allowed • ${viewModel.firewallBlockedDomains.size} Blocked",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun InternalLogItem(entry: com.amnos.browser.core.model.InternalLogEntry) {
    val color = when (entry.level) {
        "ERROR" -> KillRed
        "WARN" -> Color(0xffff9800)
        "DEBUG" -> TextGray
        else -> Color.White
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row {
            Text(
                "[${entry.tag}]",
                color = AccentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(80.dp)
            )
            Text(
                entry.message,
                color = color,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
fun FirewallModeButton(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.height(36.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        color = if (active) AccentBlue.copy(alpha = 0.2f) else SurfaceGray,
        border = if (active) BorderStroke(1.dp, AccentBlue) else null
    ) {
        Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (active) AccentBlue else TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RequestLogItem(log: RequestEntry, onBlock: (String) -> Unit, onAllow: (String) -> Unit) {
    val host = try { log.url.toHttpUrlOrNull()?.host ?: log.url } catch (_: Exception) { log.url }
    
    Surface(
        color = SurfaceGray,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isBlocked = log.disposition == RequestDisposition.BLOCKED
                val icon = when {
                    !isBlocked -> Icons.Default.CheckCircle
                    log.reason == "firewall_rule" -> Icons.Default.Security
                    log.reason == "tracker" || log.reason == "third_party" -> Icons.Default.Block
                    else -> Icons.Default.Warning
                }
                val color = when {
                    !isBlocked -> Color(0xFF4CAF50)
                    log.reason == "firewall_rule" -> AccentBlue
                    else -> KillRed
                }
                
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    host,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    log.type.name,
                    color = TextGray,
                    fontSize = 10.sp,
                    modifier = Modifier.background(DarkGray, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp)
                )
            }
            
            if (log.disposition == RequestDisposition.BLOCKED) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Blocked: ${log.reason ?: "Security Policy"}",
                    color = KillRed,
                    fontSize = 11.sp
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAllow(host) },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.1f), contentColor = AccentBlue),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("ALLOW", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onBlock(host) },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KillRed.copy(alpha = 0.1f), contentColor = KillRed),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("BLOCK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
