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
import androidx.compose.ui.graphics.Brush
import com.privacy.browser.ui.theme.DarkGray
import com.privacy.browser.ui.theme.DeepSpaceGradient

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    
    // Handle system back button
    BackHandler(enabled = viewModel.uiState.value == BrowserUIState.BROWSING) {
        viewModel.goBack()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(DeepSpaceGradient))
    ) {
        AnimatedContent(
            targetState = viewModel.uiState.value,
            transitionSpec = {
                fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(700))
            }
        ) { state ->
            when (state) {
                BrowserUIState.HOME -> HomeView(viewModel)
                BrowserUIState.BROWSING -> BrowsingView(viewModel)
            }
        }
    }
}
