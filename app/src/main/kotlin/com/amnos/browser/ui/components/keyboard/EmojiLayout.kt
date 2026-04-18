package com.amnos.browser.ui.components.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amnos.browser.ui.theme.GlassWhite

private val EMOJI_LIST = listOf(
    "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
    "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
    "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
    "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
    "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬",
    "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗",
    "🤔", "🤭", "🤫", "🤥", "😶", "😐", "😑", "😬", "🙄", "😯",
    "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "🤐",
    "🥴", "🤢", "🤮", "🤧", "🤨", "🤫", "🤥", "😶", "😐", "😑",
    "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
    "🔥", "✨", "🌟", "⭐", "🌈", "⚡", "☀️", "💧", "❄️", "🍀",
    "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙", "🤚", "✋"
)

@Composable
fun EmojiLayout(viewModel: KeyboardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            modifier = Modifier.weight(1.0f),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(EMOJI_LIST) { emoji ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.handleInput(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 28.sp)
                }
            }
        }
        
        // Bottom Control Row - Balanced and functional
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GhostKey(
                text = "ABC",
                modifier = Modifier.weight(1.5f),
                containerColor = GlassWhite.copy(alpha = 0.2f)
            ) {
                viewModel.setLayout(GhostKeyboardLayout.ALPHA)
            }
            
            GhostKey(
                text = "space",
                modifier = Modifier.weight(4f),
                containerColor = GlassWhite
            ) {
                viewModel.handleInput(" ")
            }
            
            GhostKey(
                icon = Icons.Default.Backspace,
                modifier = Modifier.weight(1.5f),
                containerColor = GlassWhite
            ) {
                viewModel.handleBackspace()
            }
        }
    }
}
