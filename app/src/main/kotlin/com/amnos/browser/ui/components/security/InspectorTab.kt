package com.amnos.browser.ui.components.security

import androidx.compose.foundation.background
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
import com.amnos.browser.core.session.SecurityController
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.AccentBlue
import com.amnos.browser.ui.theme.KillRed
import com.amnos.browser.ui.theme.TextGray

@Composable
fun InspectorTab(viewModel: BrowserViewModel) {
    Column {
        Text("Volatile Request Log", color = Color.White, fontWeight = FontWeight.Bold)
        Text("Real-time visibility into all outgoing network traffic.", color = TextGray, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        RequestInspectorList(viewModel.requestLog)
    }
}

@Composable
fun RequestInspectorList(logs: List<SecurityController.RequestEntry>) {
    Column(modifier = Modifier.height(250.dp)) {
        Text(
            "Volatile Request Log (Last 100)",
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
fun RequestItem(entry: SecurityController.RequestEntry) {
    val color = when (entry.disposition) {
        SecurityController.RequestDisposition.BLOCKED -> KillRed
        SecurityController.RequestDisposition.PASSTHROUGH -> Color(0xFFFFC857)
        SecurityController.RequestDisposition.ALLOWED -> AccentBlue
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
