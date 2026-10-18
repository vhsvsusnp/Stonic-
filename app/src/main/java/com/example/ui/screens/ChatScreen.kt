package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessageEntity
import com.example.ui.theme.StonicBackground
import com.example.ui.theme.StonicBorderPink
import com.example.ui.theme.StonicCyan
import com.example.ui.theme.StonicDarkPink
import com.example.ui.theme.StonicHotPinkGlow
import com.example.ui.theme.StonicNeonPink
import com.example.ui.theme.StonicSurface
import com.example.ui.theme.StonicSurfaceGlass
import com.example.ui.theme.StonicTextMuted
import com.example.ui.theme.StonicTextPrimary
import com.example.ui.theme.StonicTextSecondary
import com.example.ui.viewmodel.StonicViewModel
import com.example.voice.AssistantState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: StonicViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    val assistantState by viewModel.assistantState.collectAsState()

    val listState = rememberLazyListState()
    var inputQuery by remember { mutableStateOf("") }
    var languageMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val copyToClipboard: (String) -> Unit = { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Stonic AI Response", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StonicBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- TOP APP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StonicSurface)
                    .border(width = 1.dp, color = StonicBorderPink.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Stonic AI Intelligence",
                            color = StonicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Gemini 3.5 Flash • ${uiState.preferredLanguage}",
                            color = StonicNeonPink,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Language Selector Dropdown
                    Box {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, StonicBorderPink.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable { languageMenuOpen = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Color(0x33FF3DAE)
                        ) {
                            Text(
                                text = when (uiState.preferredLanguage) {
                                    "Urdu" -> "اردو"
                                    "Roman Urdu" -> "Roman"
                                    else -> "EN"
                                },
                                color = StonicTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        DropdownMenu(
                            expanded = languageMenuOpen,
                            onDismissRequest = { languageMenuOpen = false },
                            modifier = Modifier.background(StonicSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("English", color = Color.White) },
                                onClick = {
                                    viewModel.setLanguage("English")
                                    languageMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Urdu (اردو)", color = Color.White) },
                                onClick = {
                                    viewModel.setLanguage("Urdu")
                                    languageMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Roman Urdu (Aap kaise hain)", color = Color.White) },
                                onClick = {
                                    viewModel.setLanguage("Roman Urdu")
                                    languageMenuOpen = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Clear Chat
                    IconButton(
                        onClick = { viewModel.clearChatHistory() },
                        modifier = Modifier.testTag("chat_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = StonicTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // --- CHAT MESSAGES LIST ---
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x22FF3DAE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        tint = StonicNeonPink,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Ready to Assist",
                                    color = StonicTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Start asking anything in English, Urdu, or Roman Urdu.\nStonic AI remembers context across turns.",
                                    color = StonicTextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        message = msg,
                        isSpeaking = assistantState == AssistantState.SPEAKING,
                        onCopy = { copyToClipboard(msg.content) },
                        onSpeak = {
                            if (assistantState == AssistantState.SPEAKING) {
                                viewModel.stopSpeaking()
                            } else {
                                viewModel.speakText(msg.content)
                            }
                        },
                        onRetry = {
                            viewModel.submitQuery(msg.content)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (uiState.isProcessing) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StonicSurfaceGlass)
                                    .border(1.dp, StonicBorderPink, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = StonicNeonPink,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Stonic AI is processing...",
                                        color = StonicNeonPink,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- BOTTOM PROMPT INPUT BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StonicSurface)
                    .border(width = 1.dp, color = StonicBorderPink.copy(alpha = 0.25f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    placeholder = {
                        Text(
                            text = "Type your query...",
                            color = StonicTextMuted,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StonicTextPrimary,
                        unfocusedTextColor = StonicTextPrimary,
                        focusedBorderColor = StonicNeonPink,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        cursorColor = StonicNeonPink
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputQuery.isNotBlank()) {
                            val text = inputQuery
                            inputQuery = ""
                            viewModel.submitQuery(text)
                        } else {
                            viewModel.startListening()
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(listOf(StonicNeonPink, StonicHotPinkGlow))
                        )
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = if (inputQuery.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessageEntity,
    isSpeaking: Boolean,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onRetry: () -> Unit
) {
    val isUser = message.role == "user"
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isUser) {
                        Brush.horizontalGradient(
                            listOf(Color(0xFF380829), StonicDarkPink)
                        )
                    } else if (message.hasError) {
                        Brush.verticalGradient(
                            listOf(Color(0xFF3A0D18), Color(0xFF1E070D))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0xFF191926), Color(0xFF12121D))
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) StonicNeonPink.copy(alpha = 0.5f)
                    else if (message.hasError) Color(0xFFFF5370).copy(alpha = 0.6f)
                    else StonicBorderPink.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(14.dp)
        ) {
            Column {
                // Header in bubble
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "YOU" else "STONIC AI",
                        color = if (isUser) StonicCyan else StonicNeonPink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = time,
                        color = StonicTextMuted,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message.content,
                    color = if (message.hasError) Color(0xFFFF8595) else StonicTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bubble action bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isUser && !message.hasError) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read Aloud",
                                tint = StonicNeonPink,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (message.hasError) {
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = Color(0xFFFF5370),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Text",
                            tint = StonicTextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
