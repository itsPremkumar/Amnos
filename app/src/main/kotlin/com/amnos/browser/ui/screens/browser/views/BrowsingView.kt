package com.amnos.browser.ui.screens.browser.views

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.amnos.browser.ui.components.SecurityDashboard
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.AccentBlue
import com.amnos.browser.ui.utils.WindowSize
import com.amnos.browser.ui.utils.rememberWindowSize
import com.amnos.browser.ui.components.browser.BurnOverlay
import com.amnos.browser.ui.components.browser.TopBrowsingBar
import com.amnos.browser.ui.components.keyboard.KeyboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowsingView(viewModel: BrowserViewModel, keyboardViewModel: KeyboardViewModel) {
    val focusManager = LocalFocusManager.current
    val windowSize = rememberWindowSize()
    val isCompact = windowSize == WindowSize.COMPACT
    val webKeyboardRequested by viewModel.webKeyboardRequested

    LaunchedEffect(webKeyboardRequested) {
        if (webKeyboardRequested) {
            keyboardViewModel.show(
                onInput = { viewModel.injectWebInput(it) },
                onBackspace = { viewModel.injectWebBackspace() },
                onClearAll = { /* Optional: Implement web-clear if needed */ },
                onSearch = { viewModel.injectWebSearch() }
            )
        } else {
            // Only hide if the keyboard was actually triggered by web focus
            // (prevents hiding when address bar focus triggers it)
            if (keyboardViewModel.isVisible.value) {
                keyboardViewModel.hide()
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopBrowsingBar(viewModel, keyboardViewModel, focusManager)
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(Color.Black)
        ) {
            viewModel.currentTab.value?.let { tab ->
                AndroidView(
                    factory = { ctx ->
                        tab.webView.asView().apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Floating Progress Indicator
            if (viewModel.loadingProgress.intValue in 1..99) {
                LinearProgressIndicator(
                    progress = viewModel.loadingProgress.intValue / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(if (isCompact) Alignment.BottomCenter else Alignment.TopCenter),
                    color = AccentBlue,
                    trackColor = Color.Transparent,
                )
            }
            
            BurnOverlay(viewModel)
        }
    }

    if (viewModel.showSecurityDashboard.value) {
        SecurityDashboard(viewModel)
    }
}
