package com.amnos.browser.ui.components.keyboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.amnos.browser.ui.theme.TextGray
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GhostTextField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardViewModel: KeyboardViewModel,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Search,
    onSearch: () -> Unit = {},
    textColor: Color = Color.White,
    cursorColor: Color = Color.White
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Internal state to track selection
    var textFieldValue by remember { 
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length))) 
    }
    
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isFocused by remember { mutableStateOf(false) }

    // Cursor blinking animation
    val infiniteTransition = rememberInfiniteTransition()
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 499
                0f at 500
                0f at 999
            },
            repeatMode = RepeatMode.Restart
        )
    )
    
    // Sync with external value changes
    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    val onInput: (String) -> Unit = { input ->
        val currentText = textFieldValue.text
        val selection = textFieldValue.selection
        val newText = StringBuilder(currentText)
            .replace(selection.min, selection.max, input)
            .toString()
        val newCursorPos = selection.min + input.length
        
        val newValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPos)
        )
        textFieldValue = newValue
        onValueChange(newText)
    }

    val onBackspace: () -> Unit = {
        val currentText = textFieldValue.text
        val selection = textFieldValue.selection
        
        if (selection.length > 0) {
            val newText = StringBuilder(currentText)
                .delete(selection.min, selection.max)
                .toString()
            val newValue = TextFieldValue(
                text = newText,
                selection = TextRange(selection.min)
            )
            textFieldValue = newValue
            onValueChange(newText)
        } else if (selection.min > 0) {
            val newText = StringBuilder(currentText)
                .delete(selection.min - 1, selection.min)
                .toString()
            val newValue = TextFieldValue(
                text = newText,
                selection = TextRange(selection.min - 1)
            )
            textFieldValue = newValue
            onValueChange(newText)
        }
    }

    val onClearAll: () -> Unit = {
        textFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)
        onValueChange("")
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { 
            textFieldValue = it
            onValueChange(it.text)
        },
        modifier = modifier.onFocusChanged { focusState ->
            isFocused = focusState.isFocused
            if (focusState.isFocused) {
                com.amnos.browser.core.session.AmnosLog.d("GhostTextField", "Focused! Suppressing Gboard and showing Ghost Keyboard.")
                keyboardController?.hide()
                keyboardViewModel.show(
                    onInput = onInput,
                    onBackspace = onBackspace,
                    onClearAll = onClearAll,
                    onSearch = onSearch
                )
            }
        },
        onTextLayout = { textLayoutResult = it },
        readOnly = true, // SUPPRESS SYSTEM KEYBOARD
        singleLine = singleLine,
        textStyle = LocalTextStyle.current.copy(color = textColor),
        cursorBrush = SolidColor(cursorColor),
        keyboardOptions = KeyboardOptions(
            imeAction = imeAction,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
            autoCorrect = false
        ),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (textFieldValue.text.isEmpty() && placeholder.isNotEmpty()) {
                    Text(text = placeholder, color = TextGray)
                }
                innerTextField()
                
                // Custom Cursor Implementation
                if (isFocused) {
                    textLayoutResult?.let { layout ->
                        val cursorOffset = textFieldValue.selection.max
                        if (cursorOffset <= layout.layoutInput.text.length) {
                            val cursorRect = layout.getCursorRect(cursorOffset)
                            val density = LocalDensity.current
                            val xOffset = with(density) { cursorRect.left.toDp() }
                            val yOffset = with(density) { cursorRect.top.toDp() }
                            val cursorHeight = with(density) { cursorRect.height.toDp() }
                            
                            Box(
                                modifier = Modifier
                                    .padding(start = xOffset, top = yOffset)
                                    .size(2.dp, cursorHeight)
                                    .alpha(cursorAlpha)
                                    .background(cursorColor)
                            )
                        }
                    }
                }
            }
        }
    )
}
