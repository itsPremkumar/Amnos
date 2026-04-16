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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
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
                when (layoutState) {
                    GhostKeyboardLayout.ALPHA -> AlphaLayout(viewModel)
                    GhostKeyboardLayout.SYMBOLS -> SymbolLayout(viewModel)
                    GhostKeyboardLayout.EMOJI -> EmojiLayout(viewModel)
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
    val isShifted = shiftState != GhostShiftState.OFF

    // Row 1
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        row1.forEachIndexed { index, char ->
            val number = if (index < 9) (index + 1).toString() else "0"
            GhostKey(
                text = if (isShifted) char.uppercase() else char,
                secondaryText = number,
                modifier = Modifier.weight(1f),
                onLongPress = { viewModel.handleInput(number) }
            ) {
                viewModel.handleInput(char)
            }
        }
    }

    // Row 2 (Staggered - Padded for Gboard feel)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        row2.forEach { char ->
            GhostKey(
                text = if (isShifted) char.uppercase() else char,
                modifier = Modifier.weight(1f)
            ) {
                viewModel.handleInput(char)
            }
        }
    }
    
    // Row 3 (Shift + Keys + Backspace)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Shift Key
        val shiftIcon = when (shiftState) {
            GhostShiftState.OFF -> Icons.Default.ArrowUpward
            GhostShiftState.ONCE -> Icons.Default.KeyboardArrowUp
            GhostShiftState.CAPS -> Icons.Default.KeyboardDoubleArrowUp
        }
        val shiftColor = if (shiftState != GhostShiftState.OFF) AccentBlue else Color.White
        val shiftBg = if (shiftState != GhostShiftState.OFF) GlassWhite.copy(alpha = 0.3f) else GlassWhite
        
        GhostKey(
            icon = shiftIcon,
            modifier = Modifier.weight(1.5f),
            containerColor = shiftBg,
            contentColor = shiftColor
        ) {
            viewModel.toggleShift()
        }
        
        row3.forEach { char ->
            GhostKey(
                text = if (isShifted) char.uppercase() else char,
                modifier = Modifier.weight(1f)
            ) {
                viewModel.handleInput(char)
            }
        }
        
        // Backspace Key
        GhostKey(
            icon = Icons.Default.Backspace,
            modifier = Modifier.weight(1.5f),
            containerColor = GlassWhite
        ) {
            viewModel.handleBackspace()
        }
    }
    
    // Row 4 (Bottom Row)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GhostKey(
            text = "?123",
            modifier = Modifier.weight(1.5f),
            containerColor = GlassWhite.copy(alpha = 0.2f)
        ) {
            viewModel.toggleLayout()
        }
        
        GhostKey(
            text = "😀",
            modifier = Modifier.weight(1f),
            containerColor = GlassWhite.copy(alpha = 0.2f)
        ) {
            viewModel.setLayout(GhostKeyboardLayout.EMOJI)
        }
        
        GhostKey(
            text = "space",
            modifier = Modifier.weight(3f),
            containerColor = GlassWhite
        ) {
            viewModel.handleSpace()
        }
        
        GhostKey(
            icon = Icons.Default.Search,
            modifier = Modifier.weight(1.5f),
            containerColor = AccentBlue.copy(alpha = 0.4f),
            contentColor = Color.White
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
fun KeyboardRow(keys: List<String>, isUppercase: Boolean = false, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { key ->
            GhostKey(
                text = if (isUppercase) key.uppercase() else key,
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
    secondaryText: String? = null,
    icon: ImageVector? = null,
    containerColor: Color = GlassWhite,
    contentColor: Color = Color.White,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) containerColor.copy(alpha = 0.4f) else containerColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        onClick()
                    },
                    onLongPress = {
                        if (onLongPress != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Secondary label (top-right small text)
        if (secondaryText != null) {
            Text(
                text = secondaryText,
                color = contentColor.copy(alpha = 0.4f),
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )
        }

        // The Key Popup (Bubble)
        if (isPressed && text != null && text.length == 1) {
            Box(
                modifier = Modifier
                    .offset(y = (-60).dp)
                    .size(50.dp, 60.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Color.White.copy(alpha = 0.95f))
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color.Black,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (text != null) {
            Text(
                text = text,
                color = contentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
