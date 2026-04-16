package com.amnos.browser.ui.screens.browser

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.amnos.browser.ui.screens.browser.views.BrowsingView
import com.amnos.browser.ui.screens.browser.views.HomeView
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import com.amnos.browser.ui.theme.DarkGray
import com.amnos.browser.ui.theme.DeepSpaceGradient
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amnos.browser.ui.components.keyboard.GhostKeyboard
import com.amnos.browser.ui.components.keyboard.KeyboardViewModel
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Warning
import com.amnos.browser.ui.theme.KillRed
import com.amnos.browser.ui.theme.SurfaceGray
import com.amnos.browser.ui.theme.TextGray
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val keyboardViewModel: KeyboardViewModel = viewModel()
    
    // Handle system back button
    BackHandler(enabled = viewModel.uiState.value == BrowserUIState.BROWSING) {
        viewModel.goBack()
    }

    // Hide keyboard when switching screens
    LaunchedEffect(viewModel.uiState.value) {
        keyboardViewModel.hide()
    }

    val keyboardHeight by keyboardViewModel.keyboardHeight

    // Gated Navigation Safety Dialog
    if (viewModel.blockedNavigationUrl.value != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelBlockedNavigation() },
            title = { Text("Security Checkpoint", fontWeight = FontWeight.Bold) },
            text = { 
                Text("Amnos has intercepted an attempt to leave the secure sandbox for an external application. Do you trust this destination?\n\nURL: ${viewModel.blockedNavigationUrl.value}") 
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmBlockedNavigation() },
                    colors = ButtonDefaults.buttonColors(containerColor = KillRed)
                ) {
                    Text("Trust & Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelBlockedNavigation() }) {
                    Text("Stay in Sandbox")
                }
            },
            containerColor = SurfaceGray,
            titleContentColor = Color.White,
            textContentColor = TextGray
        )
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(DeepSpaceGradient))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = viewModel.uiState.value,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(700))
                    }
                ) { state ->
                    when (state) {
                        BrowserUIState.HOME -> HomeView(viewModel, keyboardViewModel)
                        BrowserUIState.BROWSING -> BrowsingView(viewModel, keyboardViewModel)
                    }
                }
            }
            
            // Push content up by keyboard height
            Spacer(modifier = Modifier.height(keyboardHeight))
        }

        // Accessibility Threat Banner
        if (viewModel.showAccessibilityWarning.value) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 12.dp, end = 12.dp),
                color = KillRed.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Warning, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "THREAT DETECTED: Active Accessibility Scrapers. Screen privacy is compromised.",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Overlay keyboard at the bottom
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            GhostKeyboard(keyboardViewModel)
        }
    }
}
