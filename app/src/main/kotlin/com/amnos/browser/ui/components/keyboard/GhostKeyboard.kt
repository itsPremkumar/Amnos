package com.amnos.browser.ui.components.keyboard

import androidx.compose.animation.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amnos.browser.ui.theme.AccentBlue
import com.amnos.browser.ui.theme.DarkGray
import com.amnos.browser.ui.theme.GlassBorder
import com.amnos.browser.ui.theme.GlassWhite


@Composable
fun GhostKeyboard(viewModel: KeyboardViewModel) {
    val isVisible by viewModel.isVisible
    val layoutState by viewModel.layoutState
    
    val density = LocalDensity.current
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                if (isVisible) {
                    val heightInDp = with(density) { coords.size.height.toDp() }
                    viewModel.updateHeight(heightInDp)
                }
            }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = DarkGray.copy(alpha = 0.98f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (layoutState == GhostKeyboardLayout.ALPHA) {
                    AlphaLayout(viewModel)
                } else {
                    SymbolLayout(viewModel)
                }
            }
        }
    }
}

@Composable
fun AlphaLayout(viewModel: KeyboardViewModel) {
    val shiftState by viewModel.shiftState
    
    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")

    KeyboardRow(row1) { viewModel.handleInput(it) }
    KeyboardRow(row2) { viewModel.handleInput(it) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Shift Key
        val shiftIcon = when (shiftState) {
            GhostShiftState.OFF -> Icons.Default.ArrowUpward
            GhostShiftState.ONCE -> Icons.Default.KeyboardArrowUp
            GhostShiftState.CAPS -> Icons.Default.KeyboardDoubleArrowUp
        }
        val shiftColor = if (shiftState != GhostShiftState.OFF) AccentBlue else Color.White
        
        GhostKey(
            icon = shiftIcon,
            modifier = Modifier.weight(1.5f),
            contentColor = shiftColor
        ) {
            viewModel.toggleShift()
        }
        
        row3.forEach { char ->
            GhostKey(
                text = if (shiftState != GhostShiftState.OFF) char.uppercase() else char,
                modifier = Modifier.weight(1f)
            ) {
                viewModel.handleInput(char)
            }
        }
        
        // Backspace Key
        GhostKey(
            icon = Icons.Default.Backspace,
            modifier = Modifier.weight(1.5f)
        ) {
            viewModel.handleBackspace()
        }
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GhostKey(
            text = "?123",
            modifier = Modifier.weight(1.5f)
        ) {
            viewModel.toggleLayout()
        }
        
        GhostKey(
            text = "space",
            modifier = Modifier.weight(4f)
        ) {
            viewModel.handleInput(" ")
        }
        
        GhostKey(
            icon = Icons.Default.Search,
            modifier = Modifier.weight(1.5f),
            containerColor = AccentBlue.copy(alpha = 0.3f),
            contentColor = AccentBlue
        ) {
            viewModel.handleSearch()
        }
    }
}

@Composable
fun SymbolLayout(viewModel: KeyboardViewModel) {
    val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val row2 = listOf("@", "#", "₹", "&", "*", "(", ")", "-", "+")
    val row3 = listOf("!", "\"", "'", ":", ";", "/", "?")

    KeyboardRow(row1) { viewModel.handleInput(it) }
    KeyboardRow(row2) { viewModel.handleInput(it) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GhostKey(
            text = "=",
            modifier = Modifier.weight(1.5f)
        ) {
            viewModel.handleInput("=")
        }
        
        row3.forEach { char ->
            GhostKey(
                text = char,
                modifier = Modifier.weight(1f)
            ) {
                viewModel.handleInput(char)
            }
        }
        
        GhostKey(
            icon = Icons.Default.Backspace,
            modifier = Modifier.weight(1.5f)
        ) {
            viewModel.handleBackspace()
        }
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GhostKey(
            text = "ABC",
            modifier = Modifier.weight(1.5f)
        ) {
            viewModel.toggleLayout()
        }
        
        GhostKey(
            text = "space",
            modifier = Modifier.weight(4f)
        ) {
            viewModel.handleInput(" ")
        }
        
        GhostKey(
            icon = Icons.Default.Search,
            modifier = Modifier.weight(1.5f),
            containerColor = AccentBlue.copy(alpha = 0.3f),
            contentColor = AccentBlue
        ) {
            viewModel.handleSearch()
        }
    }
}

@Composable
fun KeyboardRow(keys: List<String>, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { key ->
            GhostKey(
                text = key,
                modifier = Modifier.weight(1f),
                onClick = { onClick(key) }
            )
        }
    }
}

@Composable
fun GhostKey(
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: ImageVector? = null,
    containerColor: Color = GlassWhite,
    contentColor: Color = Color.White,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(
                text = text,
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
