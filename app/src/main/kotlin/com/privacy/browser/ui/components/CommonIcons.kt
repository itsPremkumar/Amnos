package com.privacy.browser.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

@Composable
fun ScaledIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    size: Dp,
    tint: Color
) {
    Icon(
        imageVector,
        contentDescription,
        modifier = Modifier.size(size),
        tint = tint
    )
}
