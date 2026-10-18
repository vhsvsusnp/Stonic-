package com.example

import android.app.Application
import com.example.data.local.StonicDatabase
import com.example.data.preferences.SettingsRepository
import com.example.data.remote.GeminiClient
import com.example.voice.VoiceManager

class StonicApplication : Application() {

    lateinit var database: StonicDatabase
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var geminiClient: GeminiClient
        private set

    lateinit var voiceManager: VoiceManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = StonicDatabase.getInstance(this)
        settingsRepository = SettingsRepository(this)
        geminiClient = GeminiClient(settingsRepository)
        voiceManager = VoiceManager(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        voiceManager.shutdown()
    }

    companion object {
        lateinit var instance: StonicApplication
            private set
    }
}
