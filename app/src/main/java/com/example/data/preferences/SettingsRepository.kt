package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stonic_ai_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_PREFERRED_LANGUAGE = "preferred_language"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_SELECTED_VOICE = "selected_voice"
        private const val KEY_AUTO_SPEAK = "auto_speak"
        private const val KEY_MUTE_VOICE = "mute_voice"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_BACKGROUND_LISTENING = "background_listening"
        private const val KEY_HANDS_FREE_CONVERSATION = "hands_free_conversation"
    }

    // --- API Key Management ---
    fun getSavedCustomApiKey(): String {
        return prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    fun clearCustomApiKey() {
        prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
    }

    /**
     * Resolves the actual API key to use:
     * 1. User's manually configured key in Settings
     * 2. BuildConfig.GEMINI_API_KEY from AI Studio Secrets
     */
    fun getEffectiveApiKey(): String {
        val userKey = getSavedCustomApiKey()
        if (userKey.isNotBlank()) {
            return userKey
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            buildKey
        } else {
            ""
        }
    }

    fun hasValidApiKey(): Boolean {
        return getEffectiveApiKey().isNotBlank()
    }

    // --- Language Preference ---
    // Options: "English", "Urdu", "Roman Urdu", "Auto"
    fun getPreferredLanguage(): String {
        return prefs.getString(KEY_PREFERRED_LANGUAGE, "English") ?: "English"
    }

    fun setPreferredLanguage(language: String) {
        prefs.edit().putString(KEY_PREFERRED_LANGUAGE, language).apply()
    }

    // --- TTS Voice Controls ---
    fun getTtsSpeed(): Float {
        return prefs.getFloat(KEY_TTS_SPEED, 1.0f)
    }

    fun setTtsSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_TTS_SPEED, speed).apply()
    }

    fun getTtsPitch(): Float {
        return prefs.getFloat(KEY_TTS_PITCH, 0.95f)
    }

    fun setTtsPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_TTS_PITCH, pitch).apply()
    }

    fun getSelectedVoice(): String? {
        return prefs.getString(KEY_SELECTED_VOICE, null)
    }

    fun setSelectedVoice(voiceName: String?) {
        if (voiceName == null) {
            prefs.edit().remove(KEY_SELECTED_VOICE).apply()
        } else {
            prefs.edit().putString(KEY_SELECTED_VOICE, voiceName).apply()
        }
    }

    fun isAutoSpeakEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SPEAK, true)
    }

    fun setAutoSpeakEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SPEAK, enabled).apply()
    }

    fun isVoiceMuted(): Boolean {
        return prefs.getBoolean(KEY_MUTE_VOICE, false)
    }

    fun setVoiceMuted(muted: Boolean) {
        prefs.edit().putBoolean(KEY_MUTE_VOICE, muted).apply()
    }

    // --- Wake Word & Hands-Free Conversation ---
    fun isWakeWordEnabled(): Boolean {
        return prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
    }

    fun isBackgroundListeningEnabled(): Boolean {
        return prefs.getBoolean(KEY_BACKGROUND_LISTENING, false)
    }

    fun setBackgroundListeningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_LISTENING, enabled).apply()
    }

    fun isHandsFreeEnabled(): Boolean {
        return prefs.getBoolean(KEY_HANDS_FREE_CONVERSATION, true)
    }

    fun setHandsFreeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HANDS_FREE_CONVERSATION, enabled).apply()
    }

    // Flow for observing preference changes
    fun observeKeyChanges(): Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null) trySend(key)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
}
