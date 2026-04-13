package com.amnos.browser.ui.components.keyboard

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel

enum class GhostKeyboardLayout {
    ALPHA, SYMBOLS
}

enum class GhostShiftState {
    OFF, ONCE, CAPS
}

class KeyboardViewModel : ViewModel() {
    private val _layoutState = mutableStateOf(GhostKeyboardLayout.ALPHA)
    val layoutState: State<GhostKeyboardLayout> = _layoutState

    private val _shiftState = mutableStateOf(GhostShiftState.OFF)
    val shiftState: State<GhostShiftState> = _shiftState

    private val _isVisible = mutableStateOf(false)
    val isVisible: State<Boolean> = _isVisible

    private val _keyboardHeight = mutableStateOf(0.dp)
    val keyboardHeight: State<Dp> = _keyboardHeight

    // The callback to communicate with the integrated TextField
    private var inputCallback: ((String) -> Unit)? = null
    private var backspaceCallback: (() -> Unit)? = null
    private var searchCallback: (() -> Unit)? = null

    fun show(
        onInput: (String) -> Unit,
        onBackspace: () -> Unit,
        onSearch: () -> Unit
    ) {
        inputCallback = onInput
        backspaceCallback = onBackspace
        searchCallback = onSearch
        _isVisible.value = true
    }

    fun hide() {
        _isVisible.value = false
        _keyboardHeight.value = 0.dp
        inputCallback = null
        backspaceCallback = null
        searchCallback = null
    }

    fun updateHeight(height: Dp) {
        if (_isVisible.value) {
            _keyboardHeight.value = height
        }
    }

    fun toggleLayout() {
        _layoutState.value = if (_layoutState.value == GhostKeyboardLayout.ALPHA) {
            GhostKeyboardLayout.SYMBOLS
        } else {
            GhostKeyboardLayout.ALPHA
        }
    }

    fun toggleShift() {
        _shiftState.value = when (_shiftState.value) {
            GhostShiftState.OFF -> GhostShiftState.ONCE
            GhostShiftState.ONCE -> GhostShiftState.CAPS
            GhostShiftState.CAPS -> GhostShiftState.OFF
        }
    }

    fun handleInput(char: String) {
        val processedChar = if (_shiftState.value != GhostShiftState.OFF) {
            char.uppercase()
        } else {
            char.lowercase()
        }
        
        inputCallback?.invoke(processedChar)
        
        if (_shiftState.value == GhostShiftState.ONCE) {
            _shiftState.value = GhostShiftState.OFF
        }
    }

    fun handleBackspace() {
        backspaceCallback?.invoke()
    }

    fun handleSearch() {
        searchCallback?.invoke()
        hide()
    }
}
