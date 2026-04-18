package com.amnos.browser.ui.screens.browser.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.AccentBlue
import com.amnos.browser.ui.theme.CardGray
import com.amnos.browser.ui.theme.TextGray

import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import com.amnos.browser.ui.theme.GlassBorder
import com.amnos.browser.ui.theme.GlassWhite
import com.amnos.browser.ui.utils.WindowSize
import com.amnos.browser.ui.utils.rememberWindowSize

import com.amnos.browser.ui.components.keyboard.GhostTextField
import com.amnos.browser.ui.components.keyboard.KeyboardViewModel

@Composable
fun HomeView(viewModel: BrowserViewModel, keyboardViewModel: KeyboardViewModel) {
    val focusManager = LocalFocusManager.current
    val windowSize = rememberWindowSize()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = windowSize == WindowSize.EXPANDED
        val isLandscape = com.amnos.browser.ui.utils.isLandscape()

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = Color.Transparent
        ) {
            when {
                isExpanded -> HomeViewExpanded(viewModel, keyboardViewModel, focusManager)
                isLandscape -> HomeViewLandscapeCompact(viewModel, keyboardViewModel, focusManager)
                else -> HomeViewCompact(viewModel, keyboardViewModel, focusManager)
            }
        }
    }
}

@Composable
fun HomeViewCompact(
    viewModel: BrowserViewModel,
    keyboardViewModel: KeyboardViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppLogoSection()

        Spacer(modifier = Modifier.height(48.dp))

        AddressSearchBar(viewModel, keyboardViewModel, focusManager)
        
        SecurityStatusFooter(viewModel)
    }
}

@Composable
fun HomeViewLandscapeCompact(
    viewModel: BrowserViewModel,
    keyboardViewModel: KeyboardViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Smaller logo for landscape
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.03f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = AccentBlue
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Amnos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Column(
            modifier = Modifier.weight(1.2f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AddressSearchBar(viewModel, keyboardViewModel, focusManager)
            Spacer(modifier = Modifier.height(16.dp))
            SecurityStatusFooter(viewModel)
        }
    }
}

@Composable
fun HomeViewExpanded(
    viewModel: BrowserViewModel,
    keyboardViewModel: KeyboardViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppLogoSection()
        }

        Spacer(modifier = Modifier.width(96.dp))

        Column(
            modifier = Modifier.widthIn(max = 500.dp).weight(1.2f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AddressSearchBar(viewModel, keyboardViewModel, focusManager)
            Spacer(modifier = Modifier.height(48.dp))
            SecurityStatusFooter(viewModel)
        }
    }
}

@Composable
fun AppLogoSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.03f),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = AccentBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Amnos",
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = 2.sp
        )

        Text(
            text = "HARDENED EPHEMERAL BROWSER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AccentBlue,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun AddressSearchBar(
    viewModel: BrowserViewModel,
    keyboardViewModel: KeyboardViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            GhostTextField(
                value = viewModel.urlInput.value,
                onValueChange = { viewModel.urlInput.value = it },
                keyboardViewModel = keyboardViewModel,
                placeholder = "Search or enter secure URL",
                modifier = Modifier.fillMaxWidth(),
                onSearch = {
                    viewModel.navigate(viewModel.urlInput.value)
                    focusManager.clearFocus()
                }
            )
        }
    }
}

@Composable
fun SecurityStatusFooter(viewModel: BrowserViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Active session — Zero durable state",
            fontSize = 13.sp,
            color = TextGray
        )

        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Hardened Engine Active",
                fontSize = 12.sp,
                color = TextGray.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))


    }
}
