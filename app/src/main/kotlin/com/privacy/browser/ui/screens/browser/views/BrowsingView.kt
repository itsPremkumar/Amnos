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
                        .background(SurfaceGray.copy(alpha = 0.98f))
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kill Button
                        IconButton(
                            onClick = { viewModel.killSwitch() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Kill", tint = KillRed, modifier = Modifier.size(22.dp))
                        }

                        Spacer(Modifier.width(4.dp))

                        // URL Field
                        TextField(
                            value = viewModel.urlInput.value,
                            onValueChange = { viewModel.urlInput.value = it },
                            modifier = Modifier.weight(1f).height(50.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                            placeholder = { Text("Search or type URL", fontSize = 15.sp, color = Color.Gray) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp), // More modern Squircle look
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                                cursorColor = AccentBlue
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = if (viewModel.urlInput.value.startsWith("https")) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Security",
                                    tint = if (viewModel.urlInput.value.startsWith("https")) Color.Gray.copy(alpha = 0.8f) else KillRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { viewModel.reload() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(18.dp), tint = Color.LightGray)
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                viewModel.navigate(viewModel.urlInput.value)
                                focusManager.clearFocus()
                            })
                        )

                        Spacer(Modifier.width(8.dp))

                        // Tracker Badge
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
