package com.privacy.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                Text("Security Cockpit", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Stats Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
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
                        Text("STRICT", color = KillRed, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // Toggles
            SecurityToggle(
                title = "JavaScript",
                description = "Enable for functionality, Disable for maximum security.",
                checked = viewModel.isJavaScriptEnabled.value,
                onCheckedChange = { viewModel.toggleJavaScript(it) }
            )

            SecurityToggle(
                title = "WebGL / 3D Graphics",
                description = "Prevents hardware-based graphics fingerprinting.",
                checked = viewModel.isWebGLEnabled.value,
                onCheckedChange = { viewModel.toggleWebGL(it) }
            )

            SecurityToggle(
                title = "Strict Font Masking",
                description = "Prevents identification via your system fonts.",
                checked = true, // Force on for now
                onCheckedChange = { },
                enabled = false
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                "Architecture v2 - God-Tier Volatility Active",
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
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
