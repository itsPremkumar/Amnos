package com.amnos.browser.ui.components.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import com.amnos.browser.core.model.*
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.AccentBlue
import com.amnos.browser.ui.theme.KillRed

@Composable
fun InspectorTab(viewModel: BrowserViewModel) {
    var activeTab by remember { mutableStateOf("Requests") }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                TabButton("Requests", activeTab == "Requests") { activeTab = "Requests" }
                Spacer(Modifier.width(16.dp))
                TabButton("Console", activeTab == "Console") { activeTab = "Console" }
            }
        }
        
        Spacer(Modifier.height(16.dp))

        if (activeTab == "Requests") {
            RequestInspectorList(viewModel.requestLog)
        } else {
            SecurityConsoleList(viewModel.internalLogs)
        }
    }
}

@Composable
fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.Gray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .height(2.dp)
                    .width(20.dp)
                    .background(AccentBlue)
            )
        }
    }
}

@Composable
fun SecurityConsoleList(logs: List<InternalLogEntry>) {
    Column(modifier = Modifier.height(250.dp)) {
        Text(
            "Security Forensics (Last 10,000 Events)",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No security events recorded.", color = Color.DarkGray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs) { entry ->
                    ConsoleItem(entry)
                }
            }
        }
    }
}

@Composable
fun ConsoleItem(entry: InternalLogEntry) {
    val levelColor = when {
        entry.tag == "FingerprintShield" -> Color(0xFFBB86FC) // Purple for Spoofing
        entry.level == "ERROR" -> KillRed
        entry.level == "WARN" -> Color(0xFFFFC857)
        entry.level == "DEBUG" -> Color(0xFF888888)
        else -> AccentBlue
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (entry.tag == "FingerprintShield") "SPOOF" else entry.level,
                color = levelColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(45.dp)
                    .background(levelColor.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = entry.tag,
                color = if (entry.tag == "FingerprintShield") levelColor else Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(entry.timestamp)),
                color = Color.DarkGray,
                fontSize = 8.sp
            )
        }
        Text(
            text = entry.message,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
        )
    }
}

@Composable
fun RequestInspectorList(logs: List<RequestEntry>) {
    Column(modifier = Modifier.height(250.dp)) {
        Text(
            "Volatile Request Log (Last 10,000)",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No active requests recorded.", color = Color.DarkGray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs.reversed()) { entry ->
                    RequestItem(entry)
                }
            }
        }
    }
}

@Composable
fun RequestItem(entry: RequestEntry) {
    val color = when (entry.disposition) {
        RequestDisposition.BLOCKED -> KillRed
        RequestDisposition.PASSTHROUGH -> Color(0xFFFFC857)
        RequestDisposition.ALLOWED -> AccentBlue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.type.name,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(70.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .padding(2.dp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.url,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(entry.method)
                    if (entry.thirdParty) append(" - 3P")
                    entry.reason?.let {
                        append(" - ")
                        append(it)
                    }
                },
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}
