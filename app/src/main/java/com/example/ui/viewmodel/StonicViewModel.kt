package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StonicApplication
import com.example.data.local.ChatMessageEntity
import com.example.data.local.NoteEntity
import com.example.util.JarvisActionExecutor
import com.example.util.JarvisResult
import com.example.voice.AssistantState
import com.example.voice.AvailableVoice
import com.example.voice.StonicWakeWordService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StonicUiState(
    val latestPrompt: String = "",
    val latestResponse: String = "Greetings, Sir. Stonic AI is online and ready for your command.",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val testKeyResult: String? = null,
    val isTestingKey: Boolean = false,
    val preferredLanguage: String = "English",
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 0.95f,
    val selectedVoiceId: String? = null,
    val isMuted: Boolean = false,
    val autoSpeak: Boolean = true,
    val effectiveKeyConfigured: Boolean = false,
    val customApiKeyInput: String = "",
    val wakeWordEnabled: Boolean = true,
    val backgroundListening: Boolean = false,
    val handsFreeConversation: Boolean = true
)

class StonicViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StonicApplication
    private val dao = app.database.stonicDao()
    private val settings = app.settingsRepository
    private val geminiClient = app.geminiClient
    val voiceManager = app.voiceManager
    private val actionExecutor = JarvisActionExecutor(application, dao)

    private val _uiState = MutableStateFlow(StonicUiState())
    val uiState: StateFlow<StonicUiState> = _uiState.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessageEntity>> = dao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMessages: StateFlow<List<ChatMessageEntity>> = dao.getRecentMessages(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assistantState: StateFlow<AssistantState> = voiceManager.assistantState
    val soundLevel: StateFlow<Float> = voiceManager.soundWaveLevel
    val availableVoices: StateFlow<List<AvailableVoice>> = voiceManager.availableVoices

    private var shouldContinueConversation = true

    init {
        loadSettings()

        // Hook voice manager callback for hands-free continuous conversation
        voiceManager.onSpeakingFinishedCallback = {
            if (_uiState.value.handsFreeConversation && !_uiState.value.isMuted && shouldContinueConversation) {
                viewModelScope.launch {
                    delay(600)
                    if (voiceManager.assistantState.value == AssistantState.IDLE) {
                        startListening()
                    }
                }
            }
        }
    }

    private fun loadSettings() {
        val savedKey = settings.getSavedCustomApiKey()
        _uiState.value = _uiState.value.copy(
            customApiKeyInput = savedKey,
            effectiveKeyConfigured = settings.hasValidApiKey(),
            preferredLanguage = settings.getPreferredLanguage(),
            ttsSpeed = settings.getTtsSpeed(),
            ttsPitch = settings.getTtsPitch(),
            selectedVoiceId = settings.getSelectedVoice(),
            isMuted = settings.isVoiceMuted(),
            autoSpeak = settings.isAutoSpeakEnabled(),
            wakeWordEnabled = settings.isWakeWordEnabled(),
            backgroundListening = settings.isBackgroundListeningEnabled(),
            handsFreeConversation = settings.isHandsFreeEnabled()
        )
    }

    private fun isExitPhrase(phrase: String): Boolean {
        val l = phrase.lowercase().trim()
        return l in listOf(
            "goodbye", "good bye", "bye", "stop", "cancel", "exit", "quit",
            "band ho jao", "chup ho jao", "thank you", "thanks", "shukriya"
        )
    }

    private fun extractWakeWordAndCommand(input: String): Pair<Boolean, String> {
        val lower = input.lowercase().trim()
        val wakeWords = listOf(
            "hey stonic", "hello stonic", "hi stonic", "ok stonic", "aey stonic", "suno stonic", "stonic"
        )
        for (w in wakeWords) {
            if (lower == w) {
                return Pair(true, "")
            }
            if (lower.startsWith("$w ") || lower.startsWith("$w,") || lower.startsWith("$w:")) {
                val command = input.substring(w.length).trim().removePrefix(",").removePrefix(":").trim()
                return Pair(true, command)
            }
        }
        return Pair(false, input)
    }

    fun submitQuery(query: String, isVoice: Boolean = false) {
        val rawQuery = query.trim()
        if (rawQuery.isBlank()) return

        // Check for exit phrase to end hands-free loop
        if (isExitPhrase(rawQuery)) {
            shouldContinueConversation = false
            voiceManager.stopListening()
            val ack = "Standing by, Sir. Have a productive day."
            _uiState.value = _uiState.value.copy(
                latestPrompt = rawQuery,
                latestResponse = ack,
                isProcessing = false
            )
            viewModelScope.launch {
                dao.insertMessage(ChatMessageEntity(role = "user", content = rawQuery, isVoice = isVoice))
                dao.insertMessage(ChatMessageEntity(role = "model", content = ack))
            }
            if (_uiState.value.autoSpeak && !_uiState.value.isMuted) {
                voiceManager.speak(ack, _uiState.value.ttsSpeed, _uiState.value.ttsPitch)
            }
            return
        }

        shouldContinueConversation = true

        // Extract wake word
        val (wakeWordDetected, extractedCommand) = extractWakeWordAndCommand(rawQuery)

        // If user only said the wake word ("Stonic" / "Hey Stonic")
        if (wakeWordDetected && extractedCommand.isBlank()) {
            val reply = "Yes Sir, I am listening. How can I assist you?"
            _uiState.value = _uiState.value.copy(
                latestPrompt = rawQuery,
                latestResponse = reply,
                isProcessing = false
            )
            viewModelScope.launch {
                dao.insertMessage(ChatMessageEntity(role = "user", content = rawQuery, isVoice = isVoice))
                dao.insertMessage(ChatMessageEntity(role = "model", content = reply))
            }
            if (_uiState.value.autoSpeak && !_uiState.value.isMuted) {
                voiceManager.speak(reply, _uiState.value.ttsSpeed, _uiState.value.ttsPitch)
            } else {
                startListening()
            }
            return
        }

        // Clean command without wake-word prefix
        val cleanCommand = if (wakeWordDetected && extractedCommand.isNotBlank()) extractedCommand else rawQuery

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                latestPrompt = cleanCommand,
                isProcessing = true,
                errorMessage = null,
                successMessage = null
            )

            voiceManager.setThinkingState()

            // 1. Record user message in DB
            dao.insertMessage(
                ChatMessageEntity(
                    role = "user",
                    content = cleanCommand,
                    isVoice = isVoice,
                    language = _uiState.value.preferredLanguage
                )
            )

            // 2. Check for local device Jarvis actions first
            val actionResult = actionExecutor.tryExecuteAction(cleanCommand)
            when (actionResult) {
                is JarvisResult.Executed -> {
                    dao.insertMessage(
                        ChatMessageEntity(
                            role = "model",
                            content = actionResult.displayFeedback,
                            isVoice = false,
                            language = _uiState.value.preferredLanguage
                        )
                    )

                    _uiState.value = _uiState.value.copy(
                        latestResponse = actionResult.displayFeedback,
                        isProcessing = false
                    )

                    if (_uiState.value.autoSpeak && !_uiState.value.isMuted) {
                        voiceManager.speak(
                            actionResult.spokenFeedback,
                            _uiState.value.ttsSpeed,
                            _uiState.value.ttsPitch
                        )
                    } else {
                        voiceManager.setIdleState()
                    }
                }

                is JarvisResult.ForwardToAi -> {
                    // 3. Send to Gemini 3.5 Flash API
                    val recentTurns = dao.getRecentMessagesList(6).reversed()
                    val result = geminiClient.generateReply(
                        prompt = cleanCommand,
                        recentHistory = recentTurns,
                        languageOverride = _uiState.value.preferredLanguage
                    )

                    result.onSuccess { reply ->
                        dao.insertMessage(
                            ChatMessageEntity(
                                role = "model",
                                content = reply,
                                isVoice = false,
                                language = _uiState.value.preferredLanguage
                            )
                        )

                        _uiState.value = _uiState.value.copy(
                            latestResponse = reply,
                            isProcessing = false
                        )

                        if (_uiState.value.autoSpeak && !_uiState.value.isMuted) {
                            voiceManager.speak(
                                reply,
                                _uiState.value.ttsSpeed,
                                _uiState.value.ttsPitch
                            )
                        } else {
                            voiceManager.setIdleState()
                        }
                    }.onFailure { err ->
                        val errText = err.message ?: "Failed to generate response."
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            errorMessage = errText
                        )
                        voiceManager.setErrorState()

                        dao.insertMessage(
                            ChatMessageEntity(
                                role = "model",
                                content = "Error: $errText",
                                hasError = true,
                                language = _uiState.value.preferredLanguage
                            )
                        )
                    }
                }
            }
        }
    }

    fun startListening() {
        shouldContinueConversation = true
        voiceManager.startListening(
            preferredLanguage = _uiState.value.preferredLanguage,
            onResult = { recognizedText ->
                submitQuery(recognizedText, isVoice = true)
            },
            onError = { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error)
            }
        )
    }

    fun stopListening() {
        shouldContinueConversation = false
        voiceManager.stopListening()
    }

    fun stopSpeaking() {
        shouldContinueConversation = false
        voiceManager.stopSpeaking()
    }

    fun speakText(text: String) {
        if (_uiState.value.isMuted) return
        voiceManager.speak(
            text,
            _uiState.value.ttsSpeed,
            _uiState.value.ttsPitch
        )
    }

    fun toggleMute() {
        val newMuted = !_uiState.value.isMuted
        if (newMuted) {
            voiceManager.stopSpeaking()
        }
        settings.setVoiceMuted(newMuted)
        _uiState.value = _uiState.value.copy(isMuted = newMuted)
    }

    fun setAutoSpeak(enabled: Boolean) {
        settings.setAutoSpeakEnabled(enabled)
        _uiState.value = _uiState.value.copy(autoSpeak = enabled)
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        settings.setWakeWordEnabled(enabled)
        _uiState.value = _uiState.value.copy(wakeWordEnabled = enabled)
        if (!enabled && _uiState.value.backgroundListening) {
            toggleBackgroundListening(false)
        }
    }

    fun toggleBackgroundListening(enabled: Boolean) {
        settings.setBackgroundListeningEnabled(enabled)
        _uiState.value = _uiState.value.copy(backgroundListening = enabled)
        if (enabled) {
            StonicWakeWordService.startService(app)
        } else {
            StonicWakeWordService.stopService(app)
        }
    }

    fun setHandsFreeConversation(enabled: Boolean) {
        settings.setHandsFreeEnabled(enabled)
        _uiState.value = _uiState.value.copy(handsFreeConversation = enabled)
        if (!enabled) {
            shouldContinueConversation = false
        }
    }

    fun setLanguage(lang: String) {
        settings.setPreferredLanguage(lang)
        _uiState.value = _uiState.value.copy(preferredLanguage = lang)
    }

    fun setTtsSpeed(speed: Float) {
        settings.setTtsSpeed(speed)
        _uiState.value = _uiState.value.copy(ttsSpeed = speed)
    }

    fun setTtsPitch(pitch: Float) {
        settings.setTtsPitch(pitch)
        _uiState.value = _uiState.value.copy(ttsPitch = pitch)
    }

    fun setSelectedVoice(voiceName: String?) {
        settings.setSelectedVoice(voiceName)
        voiceManager.setSpecificVoice(voiceName)
        _uiState.value = _uiState.value.copy(selectedVoiceId = voiceName)
    }

    fun updateApiKeyInput(input: String) {
        _uiState.value = _uiState.value.copy(customApiKeyInput = input)
    }

    fun saveApiKey() {
        val key = _uiState.value.customApiKeyInput.trim()
        if (key.isBlank()) {
            settings.clearCustomApiKey()
        } else {
            settings.setCustomApiKey(key)
        }
        _uiState.value = _uiState.value.copy(
            effectiveKeyConfigured = settings.hasValidApiKey(),
            successMessage = "API Key saved successfully, Sir."
        )
    }

    fun clearApiKey() {
        settings.clearCustomApiKey()
        _uiState.value = _uiState.value.copy(
            customApiKeyInput = "",
            effectiveKeyConfigured = settings.hasValidApiKey(),
            successMessage = "Custom API Key cleared, Sir."
        )
    }

    fun testApiKey() {
        val keyToTest = _uiState.value.customApiKeyInput.trim().ifEmpty {
            settings.getEffectiveApiKey()
        }

        if (keyToTest.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter an API key to test, Sir.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTestingKey = true,
                testKeyResult = null,
                errorMessage = null
            )
            val result = geminiClient.testApiKey(keyToTest)
            result.onSuccess { msg ->
                _uiState.value = _uiState.value.copy(
                    isTestingKey = false,
                    testKeyResult = msg
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isTestingKey = false,
                    testKeyResult = "Failed: ${err.message}"
                )
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            dao.clearAllMessages()
            _uiState.value = _uiState.value.copy(
                latestResponse = "Conversation history cleared, Sir.",
                successMessage = "Chat history cleared, Sir."
            )
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            dao.deleteMessage(id)
        }
    }

    fun addNote(title: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val noteTitle = title.trim().ifEmpty {
                if (content.length > 25) content.take(25) + "..." else content
            }
            dao.insertNote(
                NoteEntity(
                    title = noteTitle,
                    content = content.trim(),
                    tag = "Assistant Note"
                )
            )
            _uiState.value = _uiState.value.copy(successMessage = "Note saved, Sir.")
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            dao.deleteNoteById(id)
        }
    }

    fun dismissMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
