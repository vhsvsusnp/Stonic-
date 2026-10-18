package com.example.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.StonicApplication
import java.util.Locale

class StonicWakeWordService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var shouldKeepListening = true

    companion object {
        const val CHANNEL_ID = "stonic_wake_word_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.action.START_WAKE_WORD"
        const val ACTION_STOP = "com.example.action.STOP_WAKE_WORD"
        const val EXTRA_WAKE_PROMPT = "extra_wake_prompt"

        fun startService(context: Context) {
            val intent = Intent(context, StonicWakeWordService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, StonicWakeWordService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                shouldKeepListening = false
                stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                shouldKeepListening = true
                startAsForeground()
                initSpeechRecognizer()
                startWakeWordListening()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Stonic AI Voice Core",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors for 'Stonic' wake word in background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startAsForeground() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Stonic AI Voice Core Active")
            .setContentText("Say \"Stonic\" or \"Hey Stonic\" anytime to activate")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    // If background listening is desired, restart after a brief delay
                    if (shouldKeepListening) {
                        android.os.Handler(mainLooper).postDelayed({
                            if (shouldKeepListening) {
                                startWakeWordListening()
                            }
                        }, 1200)
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val phrase = matches?.firstOrNull()?.trim() ?: ""

                    if (phrase.isNotBlank()) {
                        handleDetectedSpeech(phrase)
                    }

                    if (shouldKeepListening) {
                        android.os.Handler(mainLooper).postDelayed({
                            if (shouldKeepListening) {
                                startWakeWordListening()
                            }
                        }, 800)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val phrase = matches?.firstOrNull()?.trim() ?: ""
                    if (containsWakeWord(phrase)) {
                        handleDetectedSpeech(phrase)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startWakeWordListening() {
        if (isListening || !shouldKeepListening) return
        val recognizer = speechRecognizer ?: return

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString())
            }
            recognizer.startListening(intent)
        } catch (_: Exception) {
            isListening = false
        }
    }

    private fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isListening = false
    }

    private fun containsWakeWord(text: String): Boolean {
        val lower = text.lowercase(Locale.ENGLISH)
        return lower.contains("stonic") ||
                lower.contains("tonic") ||
                lower.contains("hey stonic") ||
                lower.contains("hi stonic") ||
                lower.contains("hello stonic") ||
                lower.contains("ok stonic")
    }

    private fun handleDetectedSpeech(phrase: String) {
        if (!containsWakeWord(phrase)) return

        // Extract command following wake word, e.g. "Stonic what is the time" -> "what is the time"
        val regex = Regex("^(hey |hello |hi |ok |aey )?s?tonic[,:]?\\s*", RegexOption.IGNORE_CASE)
        val extractedCommand = phrase.replace(regex, "").trim()

        // Bring MainActivity to foreground and trigger assistant
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WAKE_PROMPT, extractedCommand)
        }
        startActivity(launchIntent)
    }

    override fun onDestroy() {
        shouldKeepListening = false
        stopListening()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
