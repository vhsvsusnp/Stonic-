package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.components.StonicAiOrb
import com.example.ui.components.StonicGlassCard
import com.example.ui.components.StonicStatusChip
import com.example.ui.theme.StonicBackground
import com.example.ui.theme.StonicBorderPink
import com.example.ui.theme.StonicCyan
import com.example.ui.theme.StonicHotPinkGlow
import com.example.ui.theme.StonicNeonPink
import com.example.ui.theme.StonicSurface
import com.example.ui.theme.StonicSurfaceGlass
import com.example.ui.theme.StonicTextMuted
import com.example.ui.theme.StonicTextPrimary
import com.example.ui.theme.StonicTextSecondary
import com.example.ui.viewmodel.StonicViewModel
import com.example.voice.AssistantState

@Composable
fun HomeScreen(
    viewModel: StonicViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val assistantState by viewModel.assistantState.collectAsState()
    val soundLevel by viewModel.soundLevel.collectAsState()
    val recentList by viewModel.recentMessages.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    val onMicClicked = {
        when {
            assistantState == AssistantState.LISTENING -> {
                viewModel.stopListening()
            }
            assistantState == AssistantState.SPEAKING -> {
                viewModel.stopSpeaking()
            }
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.startListening()
            }
            else -> {
                showPermissionRationale = true
            }
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = {
                Text(
                    text = "Microphone Access Required",
                    color = StonicTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Stonic AI needs access to your microphone for voice recognition, spoken commands, and hands-free conversational assistance. Your audio is processed locally using Android speech services and never recorded secretly.",
                    color = StonicTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StonicNeonPink)
                ) {
                    Text("Grant Access", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Cancel", color = StonicTextMuted)
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateToSettings() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, StonicNeonPink, CircleShape)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.stonic_logo),
                            contentDescription = "Stonic AI Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "STONIC AI",
                            color = StonicTextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Your Personal AI Assistant",
                            color = StonicNeonPink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mute / Unmute quick toggle
                    IconButton(
                        onClick = { viewModel.toggleMute() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = if (uiState.isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute Voice",
                            tint = if (uiState.isMuted) StonicTextMuted else StonicNeonPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Chip
            StonicStatusChip(state = assistantState)

            Spacer(modifier = Modifier.height(20.dp))

            // --- ANIMATED GLOWING AI ORB ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                StonicAiOrb(
                    state = assistantState,
                    soundLevel = soundLevel,
                    size = 230.dp,
                    onClick = onMicClicked
                )
            }

            // Subtitle state feedback
            Text(
                text = when (assistantState) {
                    AssistantState.IDLE -> "Tap orb or mic to speak with Stonic"
                    AssistantState.LISTENING -> "Listening... speak now"
                    AssistantState.THINKING -> "Synthesizing response with Gemini..."
                    AssistantState.SPEAKING -> "Tap orb to interrupt speech"
                },
                color = when (assistantState) {
                    AssistantState.LISTENING -> StonicCyan
                    AssistantState.THINKING -> StonicNeonPink
                    else -> StonicTextMuted
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Response snippet display
            if (uiState.latestResponse.isNotBlank()) {
                StonicGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (uiState.errorMessage != null) Color(0xFFFF5370) else StonicBorderPink
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.errorMessage != null) "SYSTEM ALERT" else "ASSISTANT TRANSMISSION",
                            color = if (uiState.errorMessage != null) Color(0xFFFF5370) else StonicNeonPink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        if (assistantState == AssistantState.SPEAKING) {
                            Text(
                                text = "Speaking...",
                                color = StonicCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.errorMessage ?: uiState.latestResponse,
                        color = StonicTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- QUICK PROMPT INPUT FIELD ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(StonicSurfaceGlass)
                    .border(1.dp, StonicBorderPink, RoundedCornerShape(28.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = {
                        Text(
                            text = "Ask Stonic anything...",
                            color = StonicTextMuted,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StonicTextPrimary,
                        unfocusedTextColor = StonicTextPrimary,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = StonicNeonPink
                    ),
                    maxLines = 2,
                    singleLine = true
                )

                if (inputPrompt.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val text = inputPrompt
                            inputPrompt = ""
                            viewModel.submitQuery(text)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(StonicNeonPink)
                            .testTag("home_send_button")
                    ) {
                        if (uiState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = onMicClicked,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (assistantState == AssistantState.LISTENING) StonicCyan else StonicNeonPink
                            )
                            .testTag("home_mic_button")
                    ) {
                        Icon(
                            imageVector = if (assistantState == AssistantState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (assistantState == AssistantState.LISTENING) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- QUICK ACTION LAUNCHER CARDS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    title = "AI Chat",
                    subtitle = "Multi-turn",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    iconColor = StonicNeonPink,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToChat
                )
                QuickActionItem(
                    title = "Jarvis Tools",
                    subtitle = "App & Device",
                    icon = Icons.Default.SmartToy,
                    iconColor = StonicCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTools
                )
                QuickActionItem(
                    title = "Notes",
                    subtitle = "Local records",
                    icon = Icons.AutoMirrored.Filled.Note,
                    iconColor = StonicHotPinkGlow,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToNotes
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // --- SUGGESTED QUERIES CHIPS ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SUGGESTED COMMANDS",
                    color = StonicTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val suggestions = listOf(
                    "What time is it?",
                    "Open YouTube",
                    "Take a note: Buy Groceries",
                    "Calculate 450 * 18",
                    "Set timer for 5 minutes",
                    "Tell me a joke in Roman Urdu",
                    "Who are you, Stonic?"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions) { query ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, StonicBorderPink.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .clickable {
                                    viewModel.submitQuery(query)
                                },
                            color = StonicSurfaceGlass
                        ) {
                            Text(
                                text = query,
                                color = StonicTextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- RECENT INTERACTIONS ---
            if (recentList.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT CONVERSATION",
                            color = StonicTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "View All",
                            color = StonicNeonPink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNavigateToChat() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    recentList.take(3).forEach { item ->
                        RecentItemRow(item.role == "user", item.content)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(StonicSurfaceGlass)
            .border(1.dp, StonicBorderPink.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = StonicTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = StonicTextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun RecentItemRow(isUser: Boolean, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x8013131D))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isUser) StonicCyan else StonicNeonPink)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = (if (isUser) "You: " else "Stonic: ") + text,
            color = if (isUser) StonicTextSecondary else StonicTextPrimary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
