package com.privacy.browser.ui.screens.browser

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.privacy.browser.ui.screens.browser.views.BrowsingView
import com.privacy.browser.ui.screens.browser.views.HomeView
import com.privacy.browser.ui.theme.DarkGray

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    
    // Handle system back button
    BackHandler(enabled = viewModel.uiState.value == BrowserUIState.BROWSING) {
        viewModel.goBack()
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkGray)) {
        AnimatedContent(
            targetState = viewModel.uiState.value,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
            }
        ) { state ->
            when (state) {
                BrowserUIState.HOME -> HomeView(viewModel)
                BrowserUIState.BROWSING -> BrowsingView(viewModel)
            }
        }
    }
}
