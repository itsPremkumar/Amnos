package com.amnos.browser.ui.components.browser

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.GlassBorder
import com.amnos.browser.ui.theme.SurfaceGray
import com.amnos.browser.ui.components.keyboard.KeyboardViewModel

@Composable
fun TopBrowsingBar(
    viewModel: BrowserViewModel,
    keyboardViewModel: KeyboardViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
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
        val isLandscape = com.amnos.browser.ui.utils.isLandscape()
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp, 
                vertical = if (isLandscape) 4.dp else 8.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackerBadge(viewModel)
                Spacer(Modifier.width(8.dp))
                AddressField(viewModel, keyboardViewModel, focusManager, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                BurnSessionButton(viewModel)
            }
        }
    }
}
