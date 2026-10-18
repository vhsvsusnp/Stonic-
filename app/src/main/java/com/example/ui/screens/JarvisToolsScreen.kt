package com.example.ui.screens

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StonicGlassCard
import com.example.ui.theme.StonicBackground
import com.example.ui.theme.StonicBorderPink
import com.example.ui.theme.StonicCyan
import com.example.ui.theme.StonicHotPinkGlow
import com.example.ui.theme.StonicNeonPink
import com.example.ui.theme.StonicPurple
import com.example.ui.theme.StonicSurface
import com.example.ui.theme.StonicSurfaceGlass
import com.example.ui.theme.StonicTextMuted
import com.example.ui.theme.StonicTextPrimary
import com.example.ui.theme.StonicTextSecondary
import com.example.ui.viewmodel.StonicViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JarvisToolItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val command: String
)

@Composable
fun JarvisToolsScreen(
    viewModel: StonicViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    var showWebSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var showCalcDialog by remember { mutableStateOf(false) }
    var calcExpr by remember { mutableStateOf("") }
    var calcResult by remember { mutableStateOf<String?>(null) }

    val toolsList = remember {
        listOf(
            JarvisToolItem(
                title = "Current Time",
                description = "Speaks & shows exact time",
                icon = Icons.Default.AccessTime,
                iconColor = StonicNeonPink,
                command = "what time is it"
            ),
            JarvisToolItem(
                title = "Today's Date",
                description = "Full day, date & calendar info",
                icon = Icons.Default.DateRange,
                iconColor = StonicCyan,
                command = "what is today's date"
            ),
            JarvisToolItem(
                title = "Open YouTube",
                description = "Launch official YouTube app",
                icon = Icons.Default.PlayArrow,
                iconColor = Color(0xFFFF0000),
                command = "open youtube"
            ),
            JarvisToolItem(
                title = "Launch Camera",
                description = "Ready for photos & recording",
                icon = Icons.Default.CameraAlt,
                iconColor = StonicHotPinkGlow,
                command = "open camera"
            ),
            JarvisToolItem(
                title = "System Settings",
                description = "Device configuration hub",
                icon = Icons.Default.Settings,
                iconColor = StonicPurple,
                command = "open settings"
            ),
            JarvisToolItem(
                title = "Set 5 Min Timer",
                description = "Countdown timer intent",
                icon = Icons.Default.Timer,
                iconColor = StonicCyan,
                command = "set timer for 5 minutes"
            ),
            JarvisToolItem(
                title = "Set 7:00 AM Alarm",
                description = "Native alarm clock intent",
                icon = Icons.Default.Alarm,
                iconColor = StonicNeonPink,
                command = "set alarm for 7:00 am"
            ),
            JarvisToolItem(
                title = "Google Maps",
                description = "Navigation & places nearby",
                icon = Icons.Default.Map,
                iconColor = Color(0xFF34A853),
                command = "open maps"
            )
        )
    }

    if (showWebSearchDialog) {
        AlertDialog(
            onDismissRequest = { showWebSearchDialog = false },
            title = {
                Text(
                    text = "Jarvis Web Search",
                    color = StonicTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Enter topic to search...", color = StonicTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StonicTextPrimary,
                        unfocusedTextColor = StonicTextPrimary,
                        focusedBorderColor = StonicNeonPink,
                        unfocusedBorderColor = StonicBorderPink
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                                putExtra(SearchManager.QUERY, searchQuery)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            showWebSearchDialog = false
                            searchQuery = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StonicNeonPink)
                ) {
                    Text("Search", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebSearchDialog = false }) {
                    Text("Cancel", color = StonicTextMuted)
                }
            },
            containerColor = StonicSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showCalcDialog) {
        AlertDialog(
            onDismissRequest = { showCalcDialog = false },
            title = {
                Text(
                    text = "Jarvis Calculator",
                    color = StonicTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = calcExpr,
                        onValueChange = { calcExpr = it },
                        placeholder = { Text("e.g. 240 * 15 or 850 / 4", color = StonicTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StonicTextPrimary,
                            unfocusedTextColor = StonicTextPrimary,
                            focusedBorderColor = StonicNeonPink,
                            unfocusedBorderColor = StonicBorderPink
                        ),
                        singleLine = true
                    )
                    if (calcResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Result: $calcResult",
                            color = StonicCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitQuery("calculate $calcExpr")
                        showCalcDialog = false
                        calcExpr = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StonicNeonPink)
                ) {
                    Text("Calculate", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalcDialog = false }) {
                    Text("Close", color = StonicTextMuted)
                }
            },
            containerColor = StonicSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StonicBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StonicSurface)
                    .border(width = 1.dp, color = StonicBorderPink.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Jarvis Device Protocol",
                        color = StonicTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Native Android Intents & Actions",
                        color = StonicCyan,
                        fontSize = 11.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Banner
                StonicGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(StonicNeonPink.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = StonicNeonPink,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Intelligent Device Automation",
                                color = StonicTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Tap any command below or speak it to the orb.",
                                color = StonicTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Utilities
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(StonicSurfaceGlass)
                            .border(1.dp, StonicCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { showWebSearchDialog = true }
                            .padding(16.dp)
                    ) {
                        Column {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = StonicCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Web Search",
                                color = StonicTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Browse internet via default browser",
                                color = StonicTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(StonicSurfaceGlass)
                            .border(1.dp, StonicNeonPink.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { showCalcDialog = true }
                            .padding(16.dp)
                    ) {
                        Column {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Calc",
                                tint = StonicNeonPink,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Calculator",
                                color = StonicTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Solve math & percentages instantly",
                                color = StonicTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "ONE-TAP EXECUTIONS",
                    color = StonicTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Tool Grid
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    toolsList.forEach { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(StonicSurfaceGlass)
                                .border(1.dp, StonicBorderPink.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .clickable {
                                    viewModel.submitQuery(item.command)
                                    onNavigateBack()
                                }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(item.iconColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = item.iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = StonicTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = item.description,
                                        color = StonicTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "Execute",
                                    color = StonicNeonPink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
