package com.privacy.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacy.browser.core.session.SecurityController
import com.privacy.browser.ui.screens.browser.BrowserViewModel
import com.privacy.browser.ui.theme.AccentBlue
import com.privacy.browser.ui.theme.KillRed
import com.privacy.browser.ui.theme.SurfaceGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDashboard(viewModel: BrowserViewModel) {
    ModalBottomSheet(
        onDismissRequest = { viewModel.showSecurityDashboard.value = false },
        containerColor = SurfaceGray,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text("Amnos Elite Cockpit", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Tabs for Toggles vs Inspector
            val tabs = listOf("SHIELDS", "INSPECTOR")
            var selectedTab = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }

            TabRow(
                selectedTabIndex = selectedTab.intValue,
                containerColor = Color.Transparent,
                contentColor = AccentBlue,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab.intValue == index,
                        onClick = { selectedTab.intValue = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedTab.intValue == 0) {
                Column {
                    // Stats Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Active Defense", color = Color.Gray, fontSize = 12.sp)
                                Text("${viewModel.blockedTrackersCount.value} Trackers Blocked", color = Color.White, fontWeight = FontWeight.Bold)
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

                    SecurityToggle(
                        title = "JavaScript Engine",
                        description = "Enable for sites, Disable for full ghosting.",
                        checked = viewModel.isJavaScriptEnabled.value,
                        onCheckedChange = { viewModel.toggleJavaScript(it) }
                    )

                    SecurityToggle(
                        title = "WebGL Masking",
                        description = "Spoofs GPU to prevent hardware fingerprints.",
                        checked = viewModel.isWebGLEnabled.value,
                        onCheckedChange = { viewModel.toggleWebGL(it) }
                    )

                    SecurityToggle(
                        title = "Elite Font Shield",
                        description = "Blocks all non-system fonts.",
                        checked = true,
                        onCheckedChange = { },
                        enabled = false
                    )
                }
            } else {
                // Request Inspector UI
                RequestInspectorList(viewModel.requestLog)
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                "Amnos v1.0.0 - Modular Elite Hardening Active",
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun RequestInspectorList(logs: List<SecurityController.RequestEntry>) {
    Column(modifier = Modifier.height(250.dp)) {
        Text("Volatile Request Log (Last 50)", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
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
fun RequestItem(entry: SecurityController.RequestEntry) {
    val color = when(entry.type) {
        SecurityController.RequestType.TRACKER -> KillRed
        SecurityController.RequestType.WEBSOCKET -> Color.Yellow
        else -> Color.Gray
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            entry.type.name,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(2.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.width(8.dp))
        Text(
            entry.url,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
            Text(description, color = Color.Gray, fontSize = 12.sp)
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
