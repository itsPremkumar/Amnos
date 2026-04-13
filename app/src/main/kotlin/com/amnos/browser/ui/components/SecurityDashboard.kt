package com.amnos.browser.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.AccentBlue
import com.amnos.browser.ui.theme.SurfaceGray
import com.amnos.browser.ui.theme.GlassBorder
import com.amnos.browser.ui.utils.WindowSize
import com.amnos.browser.ui.utils.rememberWindowSize
import com.amnos.browser.ui.components.security.DashboardHeader
import com.amnos.browser.ui.components.security.ShieldsTab
import com.amnos.browser.ui.components.security.InspectorTab
import com.amnos.browser.ui.components.security.IdentityTab

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SecurityDashboard(viewModel: BrowserViewModel) {
    val windowSize = rememberWindowSize()
    val isExpanded = windowSize == WindowSize.EXPANDED

    ModalBottomSheet(
        onDismissRequest = { viewModel.showSecurityDashboard.value = false },
        containerColor = SurfaceGray.copy(alpha = 0.95f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = GlassBorder) },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 64.dp)
        ) {
            DashboardHeader(viewModel)

            val tabs = listOf("SHIELDS", "INSPECTOR", "IDENTITY")
            var selectedTab by remember { mutableIntStateOf(0) }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AccentBlue,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            when (selectedTab) {
                0 -> ShieldsTab(viewModel, isExpanded)
                1 -> InspectorTab(viewModel)
                2 -> IdentityTab(viewModel)
            }
        }
    }
}
