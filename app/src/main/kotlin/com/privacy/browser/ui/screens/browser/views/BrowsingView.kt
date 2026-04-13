package com.privacy.browser.ui.screens.browser.views

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import com.privacy.browser.ui.theme.TextGray

import androidx.compose.foundation.border
import com.privacy.browser.ui.theme.GlassBorder
import com.privacy.browser.ui.theme.GlassWhite
import com.privacy.browser.ui.utils.WindowSize
import com.privacy.browser.ui.utils.rememberWindowSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowsingView(viewModel: BrowserViewModel) {
    val focusManager = LocalFocusManager.current
    val windowSize = rememberWindowSize()
    val isCompact = windowSize == WindowSize.COMPACT

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopBrowsingBar(viewModel, focusManager)
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(Color.Black)
        ) {
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
        }
    }

    if (viewModel.showSecurityDashboard.value) {
        SecurityDashboard(viewModel)
    }
}

@Composable
fun TopBrowsingBar(viewModel: BrowserViewModel, focusManager: androidx.compose.ui.focus.FocusManager) {
    Surface(
        color = SurfaceGray.copy(alpha = 0.9f),
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = GlassBorder,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppKillButton(viewModel)
                Spacer(Modifier.width(12.dp))
                AddressField(viewModel, focusManager, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                TrackerBadge(viewModel)
            }
        }
    }
}



@Composable
fun AddressField(
    viewModel: BrowserViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    TextField(
        value = viewModel.urlInput.value,
        onValueChange = { viewModel.urlInput.value = it },
        modifier = modifier.height(50.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        placeholder = { Text("Search or type URL", fontSize = 14.sp, color = TextGray) },
        singleLine = true,
        shape = RoundedCornerShape(25.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = GlassWhite,
            unfocusedContainerColor = GlassWhite,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White.copy(alpha = 0.8f),
            cursorColor = AccentBlue
        ),
        leadingIcon = {
            val isHttps = viewModel.urlInput.value.startsWith("https")
            Icon(
                imageVector = if (isHttps) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Security",
                tint = if (isHttps) AccentBlue else KillRed,
                modifier = Modifier.size(16.dp)
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            viewModel.navigate(viewModel.urlInput.value)
            focusManager.clearFocus()
        })
    )
}

@Composable
fun AppKillButton(viewModel: BrowserViewModel) {
    IconButton(
        onClick = { viewModel.killSwitch() },
        modifier = Modifier
            .size(40.dp)
            .background(KillRed.copy(alpha = 0.1f), CircleShape)
            .border(1.dp, KillRed.copy(alpha = 0.3f), CircleShape)
    ) {
        Icon(Icons.Default.Delete, contentDescription = "Kill Session", tint = KillRed, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun TrackerBadge(viewModel: BrowserViewModel) {
    Surface(
        onClick = { viewModel.showSecurityDashboard.value = true },
        color = AccentBlue.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f)),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = viewModel.blockedTrackersCount.intValue.toString(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
