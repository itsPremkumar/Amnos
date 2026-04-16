package com.amnos.browser.ui.components.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
        
        // Bottom Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassWhite)
                    .clickable { viewModel.setLayout(GhostKeyboardLayout.ALPHA) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = "ABC", color = Color.White)
            }
            
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { viewModel.handleBackspace() }
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(text = "⌫", color = Color.White, fontSize = 20.sp)
            }
        }
    }
}
