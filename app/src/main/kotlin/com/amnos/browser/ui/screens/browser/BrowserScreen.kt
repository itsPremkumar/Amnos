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

        // Overlay keyboard at the bottom
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            GhostKeyboard(keyboardViewModel)
        }
    }
}
