package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StonicGlassCard
import com.example.ui.components.StonicNeonButton
import com.example.ui.theme.StonicBackground
import com.example.ui.theme.StonicBorderPink
import com.example.ui.theme.StonicCyan
import com.example.ui.theme.StonicNeonPink
import com.example.ui.theme.StonicSurface
import com.example.ui.theme.StonicSurfaceGlass
import com.example.ui.theme.StonicTextMuted
import com.example.ui.theme.StonicTextPrimary
import com.example.ui.theme.StonicTextSecondary
import com.example.ui.viewmodel.StonicViewModel

@Composable
fun SettingsScreen(
    viewModel: StonicViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val uiState by viewModel.uiState.collectAsState()
    val voices by viewModel.availableVoices.collectAsState()

    var showKeyText by remember { mutableStateOf(false) }
    var voiceDropdownOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StonicBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StonicSurface)
                    .border(width = 1.dp, color = StonicBorderPink.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Settings & Configuration",
                        color = StonicTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Gemini Key • Voice • Language • Privacy",
                        color = StonicNeonPink,
                        fontSize = 11.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Status Toast/Banner
                if (uiState.successMessage != null || uiState.errorMessage != null) {
                    val isErr = uiState.errorMessage != null
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isErr) Color(0x33FF5370) else Color(0x3300F0FF))
                            .border(1.dp, if (isErr) Color(0xFFFF5370) else StonicCyan, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isErr) Icons.Default.Error else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isErr) Color(0xFFFF5370) else StonicCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = uiState.errorMessage ?: uiState.successMessage.orEmpty(),
                                color = StonicTextPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // --- 1. GEMINI API KEY MANAGEMENT ---
                StonicGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = StonicNeonPink,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gemini API Credentials",
                                color = StonicTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (uiState.effectiveKeyConfigured) "Active & configured" else "No key found",
                                color = if (uiState.effectiveKeyConfigured) StonicCyan else Color(0xFFFF8595),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = uiState.customApiKeyInput,
                        onValueChange = { viewModel.updateApiKeyInput(it) },
                        label = { Text("Gemini API Key", color = StonicTextMuted) },
                        placeholder = { Text("AIzaSy...", color = StonicTextMuted) },
                        visualTransformation = if (showKeyText) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showKeyText = !showKeyText }) {
                                Icon(
                                    imageVector = if (showKeyText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle key visibility",
                                    tint = StonicTextMuted
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StonicTextPrimary,
                            unfocusedTextColor = StonicTextPrimary,
                            focusedBorderColor = StonicNeonPink,
                            unfocusedBorderColor = StonicBorderPink,
                            cursorColor = StonicNeonPink
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveApiKey() },
                            colors = ButtonDefaults.buttonColors(containerColor = StonicNeonPink),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Key", color = Color.White, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { viewModel.testApiKey() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StonicCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isTestingKey) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = StonicCyan,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Test Key", color = StonicCyan, fontSize = 13.sp)
                            }
                        }

                        if (uiState.customApiKeyInput.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.clearApiKey() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FF5370))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear",
                                    tint = Color(0xFFFF5370),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (uiState.testKeyResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22FFFFFF))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = uiState.testKeyResult.orEmpty(),
                                color = if (uiState.testKeyResult?.startsWith("Success") == true) StonicCyan else Color(0xFFFF8595),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tip: You can obtain a free Gemini API key from Google AI Studio (aistudio.google.com).",
                        color = StonicTextMuted,
                        fontSize = 11.sp
                    )
                }

                // --- 2. PREFERRED RESPONSE LANGUAGE ---
                StonicGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = StonicCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Assistant Response Language",
                            color = StonicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val languages = listOf("English", "Urdu", "Roman Urdu")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languages.forEach { lang ->
                            val isSelected = uiState.preferredLanguage.equals(lang, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) StonicNeonPink else Color(0x331E1E2E))
                                    .border(
                                        1.dp,
                                        if (isSelected) StonicNeonPink else StonicBorderPink.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.setLanguage(lang) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (lang) {
                                        "Urdu" -> "اردو"
                                        "Roman Urdu" -> "Roman Urdu"
                                        else -> "English"
                                    },
                                    color = if (isSelected) Color.White else StonicTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // --- 3. MALE VOICE & TEXT-TO-SPEECH CONTROLS ---
                StonicGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = StonicNeonPink,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Voice Synthesis & Persona",
                            color = StonicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto read aloud switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Speak Responses", color = StonicTextPrimary, fontSize = 14.sp)
                            Text("Reads assistant reply aloud automatically", color = StonicTextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = uiState.autoSpeak,
                            onCheckedChange = { viewModel.setAutoSpeak(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StonicNeonPink,
                                uncheckedTrackColor = Color(0xFF2B2B36)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Voice Selection Dropdown
                    Text(
                        text = "Installed Voice Engine Profile",
                        color = StonicTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, StonicBorderPink, RoundedCornerShape(12.dp))
                                .clickable { voiceDropdownOpen = true }
                                .padding(12.dp),
                            color = Color(0x3312121E)
                        ) {
                            val currentVoiceName = voices.firstOrNull { it.id == uiState.selectedVoiceId }?.displayName
                                ?: "Default Male Voice (Optimized)"
                            Text(
                                text = currentVoiceName,
                                color = StonicTextPrimary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        DropdownMenu(
                            expanded = voiceDropdownOpen,
                            onDismissRequest = { voiceDropdownOpen = false },
                            modifier = Modifier
                                .background(StonicSurface)
                                .height(260.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Auto Best Male Voice", color = StonicCyan) },
                                onClick = {
                                    viewModel.setSelectedVoice(null)
                                    voiceDropdownOpen = false
                                }
                            )
                            voices.forEach { v ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = v.displayName,
                                            color = if (v.isMaleCandidate) StonicNeonPink else Color.White,
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSelectedVoice(v.id)
                                        voiceDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Speech Speed Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speech Speed", color = StonicTextSecondary, fontSize = 13.sp)
                        Text("${String.format(java.util.Locale.US, "%.1fx", uiState.ttsSpeed)}", color = StonicNeonPink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = uiState.ttsSpeed,
                        onValueChange = { viewModel.setTtsSpeed(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = StonicNeonPink,
                            activeTrackColor = StonicNeonPink,
                            inactiveTrackColor = Color(0xFF2A2A38)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pitch Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vocal Pitch (Deeper for Male tone)", color = StonicTextSecondary, fontSize = 13.sp)
                        Text("${String.format(java.util.Locale.US, "%.2f", uiState.ttsPitch)}", color = StonicCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = uiState.ttsPitch,
                        onValueChange = { viewModel.setTtsPitch(it) },
                        valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = StonicCyan,
                            activeTrackColor = StonicCyan,
                            inactiveTrackColor = Color(0xFF2A2A38)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.speakText("Greetings. Stonic AI voice synthesis is operational.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF3DAE)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StonicNeonPink),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Vocal Output", color = Color.White, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Note: Voice availability depends on the text-to-speech engine and language packs installed on your Android device.",
                        color = StonicTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                // --- 4. PHONE-FRIENDLY APK BUILD & INSTALLATION GUIDE ---
                StonicGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = StonicCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "How to Install APK on Phone",
                            color = StonicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Built directly for users with only an Android smartphone:\n\n" +
                                "1. AI Studio Build (Cloud): Click the Settings/Export menu in the top bar and select 'Generate APK' or 'Download APK'.\n" +
                                "2. Phone Direct Install: When downloaded onto your Android phone, tap the file in Downloads.\n" +
                                "3. Permission: If prompted 'Allow installation from this source', toggle Allow and tap Install.\n" +
                                "4. No Laptop Required: All source files, Gradle build configs, adaptive icons, and database models are completely packaged for you.",
                        color = StonicTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }

                // --- 5. SECURITY & PRIVACY NOTICE ---
                StonicGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFFFF5370),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Security & Privacy Protocol",
                            color = StonicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Local Storage: Your API key and conversation history are stored exclusively in private on-device app storage.\n" +
                                "• Zero Secret Recording: Audio is only processed when you tap the mic or orb. Stonic never records in the background.\n" +
                                "• APK Security Note: Any API key embedded inside a distributed APK can potentially be decompiled. For public release or multi-user production, always route requests through a secured backend proxy with per-user rate limiting.\n" +
                                "• Clear Data: You can purge all chat history and local preferences anytime using the button below.",
                        color = StonicTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.clearChatHistory() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF5370)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5370)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Purge Chat History", color = Color(0xFFFF5370), fontSize = 13.sp)
                    }
                }

                // --- 6. ABOUT STONIC AI ---
                StonicGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = StonicTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Stonic AI Version 1.0.0", color = StonicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Futuristic Personal Android AI Assistant", color = StonicTextMuted, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
