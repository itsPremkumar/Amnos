package com.amnos.browser.ui.components.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.KillRed

@Composable
fun BurnSessionButton(viewModel: BrowserViewModel) {
    IconButton(
        onClick = { viewModel.killSwitch() },
        modifier = Modifier
            .size(40.dp)
            .background(KillRed.copy(alpha = 0.1f), CircleShape)
            .border(1.dp, KillRed.copy(alpha = 0.3f), CircleShape)
    ) {
        Icon(Icons.Default.Whatshot, contentDescription = "Burn Session", tint = KillRed, modifier = Modifier.size(20.dp))
    }
}
