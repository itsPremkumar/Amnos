package com.amnos.browser.ui.components.security

import androidx.compose.foundation.layout.*
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
import com.amnos.browser.ui.theme.AccentBlue

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
fun JavaScriptModeSelector(
    selectedMode: JavaScriptMode,
    onModeSelected: (JavaScriptMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text("JavaScript Mode", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Text("Control scripts surface area.", color = Color.Gray, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JavaScriptMode.values().forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(mode.name, fontSize = 12.sp) },
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
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text("Fingerprint Protection", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Text("Normalization and noise level.", color = Color.Gray, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FingerprintProtectionLevel.values().forEach { level ->
                FilterChip(
                    selected = selectedLevel == level,
                    onClick = { onLevelSelected(level) },
                    label = { Text(level.name, fontSize = 12.sp) },
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
fun FirewallLevelSelector(
    selectedLevel: FirewallLevel,
    onLevelSelected: (FirewallLevel) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text("Firewall Policy", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Text("Strictness of external navigation.", color = Color.Gray, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FirewallLevel.values().forEach { level ->
                FilterChip(
                    selected = selectedLevel == level,
                    onClick = { onLevelSelected(level) },
                    label = { Text(level.name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}
