package com.privacy.browser.ui.screens.browser.views

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.privacy.browser.ui.components.ScaledIcon
import com.privacy.browser.ui.components.SecurityDashboard
import com.privacy.browser.ui.screens.browser.BrowserViewModel
import com.privacy.browser.ui.theme.AccentBlue
import com.privacy.browser.ui.theme.KillRed
import com.privacy.browser.ui.theme.SurfaceGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowsingView(viewModel: BrowserViewModel) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                Column(
                    modifier = Modifier
                        .background(SurfaceGray.copy(alpha = 0.95f))
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kill Button
                        IconButton(onClick = { viewModel.killSwitch() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Kill", tint = KillRed)
                        }

                        // URL Field
                        TextField(
                            value = viewModel.urlInput.value,
                            onValueChange = { viewModel.urlInput.value = it },
                            modifier = Modifier.weight(1f).height(48.dp),
                            placeholder = { Text("Search...", fontSize = 14.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                viewModel.navigate(viewModel.urlInput.value)
                                focusManager.clearFocus()
                            }),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.reload() }) {
                                    ScaledIcon(Icons.Default.Refresh, contentDescription = "Reload", size = 16.dp, tint = Color.White)
                                }
                            }
                        )

                        // Tracker Badge / Dashboard Trigger
                        TrackerBadge(viewModel)
                    }
                }
                
                // Progress Indicator
                if (viewModel.loadingProgress.value in 1..99) {
                    LinearProgressIndicator(
                        progress = viewModel.loadingProgress.value / 100f,
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = AccentBlue,
                        trackColor = Color.Transparent,
                    )
                }
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = SurfaceGray.copy(alpha = 0.95f),
                modifier = Modifier.height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.goBack() }, enabled = viewModel.canGoBack.value) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = if (viewModel.canGoBack.value) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { viewModel.goForward() }, enabled = viewModel.canGoForward.value) {
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "Forward", tint = if (viewModel.canGoForward.value) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { viewModel.goHome() }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.showSecurityDashboard.value = true }) {
                        Icon(Icons.Default.Shield, contentDescription = "Security", tint = AccentBlue)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            viewModel.currentTab.value?.let { tab ->
                AndroidView(
                    factory = {
                        tab.webView.apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (viewModel.showSecurityDashboard.value) {
        SecurityDashboard(viewModel)
    }
}

@Composable
fun TrackerBadge(viewModel: BrowserViewModel) {
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .height(32.dp)
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable { viewModel.showSecurityDashboard.value = true }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = viewModel.blockedTrackersCount.value.toString(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
