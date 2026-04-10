package com.privacy.browser.ui.screens.browser.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacy.browser.ui.screens.browser.BrowserViewModel
import com.privacy.browser.ui.theme.AccentBlue
import com.privacy.browser.ui.theme.CardGray
import com.privacy.browser.ui.theme.TextGray

@Composable
fun HomeView(viewModel: BrowserViewModel) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = CardGray,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = AccentBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Privacy Browser",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Single Session Security. Total Anonymity.",
            fontSize = 14.sp,
            color = TextGray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "Strong privacy protections, but not full network anonymity.",
            fontSize = 12.sp,
            color = Color(0xFFFFD166),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = viewModel.urlInput.value,
            onValueChange = { viewModel.urlInput.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            placeholder = { Text("Search or enter URL", color = TextGray) },
            singleLine = true,
            shape = RoundedCornerShape(32.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                viewModel.navigate(viewModel.urlInput.value)
                focusManager.clearFocus()
            }),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextGray)
            }
        )
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}
