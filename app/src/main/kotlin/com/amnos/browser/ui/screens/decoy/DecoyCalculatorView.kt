package com.amnos.browser.ui.screens.decoy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DecoyCalculatorView(
    secretPin: String,
    onUnlock: () -> Unit
) {
    var display by remember { mutableStateOf("0") }
    var pinBuffer by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1C)) // Sleek Dark Calculator
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Display Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = display,
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light
            )
        }

        // Button Grid
        val buttons = listOf(
            listOf("AC", "+/-", "%", "/"),
            listOf("7", "8", "9", "*"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )

        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { label ->
                    val isOrange = label in listOf("/", "*", "-", "+", "=")
                    val isGray = label in listOf("AC", "+/-", "%")
                    
                    CalculatorButton(
                        label = label,
                        color = when {
                            isOrange -> Color(0xFFFF9F0A)
                            isGray -> Color(0xFFA5A5A5)
                            else -> Color(0xFF333333)
                        },
                        textColor = if (isGray) Color.Black else Color.White,
                        modifier = Modifier.weight(if (label == "0") 2f else 1f)
                    ) {
                        // FUNCTIONALITY
                        if (label in "0".."9") {
                            display = if (display == "0") label else display + label
                            pinBuffer += label
                            if (pinBuffer == secretPin) {
                                onUnlock()
                            }
                        } else if (label == "AC") {
                            display = "0"
                            pinBuffer = ""
                        } else {
                            // Dummy calculations for decoy
                            pinBuffer = "" 
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    label: String,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.aspectRatio(if (label == "0") 2f else 1f)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
