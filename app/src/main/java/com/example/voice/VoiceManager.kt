package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

data class AvailableVoice(
    val id: String,
    val displayName: String,
    val locale: String,
    val isMaleCandidate: Boolean
)

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private var speechRecognizer: SpeechRecognizer? = null
    private val isSpeechRecognitionAvailable: Boolean by lazy {
        SpeechRecognizer.isRecognitionAvailable(context)
    }

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _soundWaveLevel = MutableStateFlow(0f)
    val soundWaveLevel: StateFlow<Float> = _soundWaveLevel.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<AvailableVoice>>(emptyList())
    val availableVoices: StateFlow<List<AvailableVoice>> = _availableVoices.asStateFlow()

    private var onSpeechResultCallback: ((String) -> Unit)? = null
    private var onSpeechErrorCallback: ((String) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _assistantState.value = AssistantState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    _assistantState.value = AssistantState.IDLE
                    _soundWaveLevel.value = 0f
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _assistantState.value = AssistantState.IDLE
                    _soundWaveLevel.value = 0f
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _assistantState.value = AssistantState.IDLE
                    _soundWaveLevel.value = 0f
                }
            })
            loadAvailableVoices()
            applyBestMaleVoice()
        }
    }

    private fun loadAvailableVoices() {
        val voiceList = mutableListOf<AvailableVoice>()
        try {
            val systemVoices = tts?.voices ?: emptySet()
            for (v in systemVoices) {
                val nameLower = v.name.lowercase(Locale.ROOT)
                // Heuristic for male voices in Google TTS and standard Android engines
                val isMale = nameLower.contains("male") ||
                        nameLower.contains("#male") ||
                        nameLower.contains("en-us-x-sfg") || // Google TTS male
                        nameLower.contains("en-us-x-iob") || // Google TTS male
                        nameLower.contains("en-gb-x-rjs")    // British male

                val label = buildString {
                    append(v.locale.displayName)
                    if (isMale) append(" (Male Candidate)")
                    append(" [${v.name}]")
                }

                voiceList.add(
                    AvailableVoice(
                        id = v.name,
                        displayName = label,
                        locale = v.locale.toString(),
                        isMaleCandidate = isMale
                    )
                )
            }
        } catch (_: Exception) {
            // Some devices may restrict voices query
        }
        _availableVoices.value = voiceList
    }

    /**
     * Attempts to find and set a male-sounding voice if available on the user's device.
     */
    fun applyBestMaleVoice() {
        if (!isTtsInitialized) return
        try {
            val voices = tts?.voices ?: return
            // Look for male voice in English first or current system language
            val maleVoice = voices.firstOrNull { voice ->
                val n = voice.name.lowercase(Locale.ROOT)
                n.contains("male") || n.contains("#male") || n.contains("en-us-x-sfg") || n.contains("en-gb-x-rjs")
            }

            if (maleVoice != null) {
                tts?.voice = maleVoice
            } else {
                // Fall back to default English voice
                tts?.language = Locale.ENGLISH
            }
        } catch (_: Exception) {
            tts?.language = Locale.ENGLISH
        }
    }

    fun setSpecificVoice(voiceName: String?) {
        if (!isTtsInitialized || voiceName.isNullOrBlank()) {
            applyBestMaleVoice()
            return
        }
        try {
            val target = tts?.voices?.firstOrNull { it.name == voiceName }
            if (target != null) {
                tts?.voice = target
            }
        } catch (_: Exception) {
            // Fallback
        }
    }

    fun speak(
        text: String,
        speechRate: Float = 1.0f,
        speechPitch: Float = 0.95f
    ) {
        if (!isTtsInitialized || text.isBlank()) return

        stopSpeaking()

        tts?.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        // Pitch slightly below 1.0 gives a deeper, more masculine Jarvis-like tone
        tts?.setPitch(speechPitch.coerceIn(0.5f, 1.5f))

        val utteranceId = "stonic_utterance_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        // Clean up markdown syntax for natural voice reading
        val cleanText = text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Bold
            .replace(Regex("\\*(.*?)\\*"), "$1")     // Italic
            .replace(Regex("`([^`]+)`"), "$1")       // Code
            .replace(Regex("#+\\s*"), "")            // Headers
            .trim()

        _assistantState.value = AssistantState.SPEAKING
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeaking() {
        if (isTtsInitialized) {
            tts?.stop()
        }
        if (_assistantState.value == AssistantState.SPEAKING) {
            _assistantState.value = AssistantState.IDLE
        }
        _soundWaveLevel.value = 0f
    }

    fun setThinkingState() {
        _assistantState.value = AssistantState.THINKING
    }

    fun setIdleState() {
        _assistantState.value = AssistantState.IDLE
        _soundWaveLevel.value = 0f
    }

    // --- Speech Recognition ---
    fun startListening(
        preferredLanguage: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isSpeechRecognitionAvailable) {
            onError("Speech recognition service is not available on this device. Please use text input.")
            return
        }

        stopSpeaking()
        stopListening()

        onSpeechResultCallback = onResult
        onSpeechErrorCallback = onError

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _assistantState.value = AssistantState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {
                        _assistantState.value = AssistantState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize RMS dB (usually -2 to 10) into 0f..1f range for visualizer
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _soundWaveLevel.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _assistantState.value = AssistantState.THINKING
                        _soundWaveLevel.value = 0f
                    }

                    override fun onError(error: Int) {
                        _assistantState.value = AssistantState.IDLE
                        _soundWaveLevel.value = 0f
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client speech recognition error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Recognition error ($error)"
                        }
                        onSpeechErrorCallback?.invoke(message)
                    }

                    override fun onResults(results: Bundle?) {
                        _assistantState.value = AssistantState.IDLE
                        _soundWaveLevel.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            onSpeechResultCallback?.invoke(text.trim())
                        } else {
                            onSpeechErrorCallback?.invoke("No clear speech recognized.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                val langCode = when (preferredLanguage.lowercase(Locale.ROOT)) {
                    "urdu" -> "ur-PK"
                    "roman urdu" -> "ur-PK"
                    else -> "en-US"
                }
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode)
            }

            speechRecognizer?.startListening(intent)
            _assistantState.value = AssistantState.LISTENING
        } catch (e: Exception) {
            _assistantState.value = AssistantState.IDLE
            onError("Failed to initialize microphone listener: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.IDLE
        }
        _soundWaveLevel.value = 0f
    }

    fun shutdown() {
        stopSpeaking()
        stopListening()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }
}
