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
import com.amnos.browser.ui.components.security.JavaScriptModeSelector
import com.amnos.browser.ui.components.security.FingerprintLevelSelector
import com.amnos.browser.ui.components.security.SecurityToggle

@Composable
fun AdvancedHardwarePanel(viewModel: BrowserViewModel) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text("HARDWARE MASKING", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(16.dp))

        JavaScriptModeSelector(
            selectedMode = viewModel.javaScriptMode.value,
            onModeSelected = viewModel::setJavaScriptMode
        )

        Spacer(Modifier.height(12.dp))

        FingerprintLevelSelector(
            selectedLevel = viewModel.fingerprintProtectionLevel.value,
            onLevelSelected = viewModel::setFingerprintProtectionLevel
        )

        Spacer(Modifier.height(12.dp))

        SecurityToggle(
            title = "WebGL Randomization",
            description = "Spoofs GPU surfaces to prevent profiling.",
            checked = viewModel.isWebGLEnabled.value,
            onCheckedChange = viewModel::toggleWebGL
        )

        SecurityToggle(
            title = "Inline Script Blocking",
            description = "Blocks scripts embedded directly in HTML.",
            checked = viewModel.privacyPolicy.value.filterBlockInlineScripts,
            onCheckedChange = viewModel::toggleInlineScriptBlocking
        )
    }
}
