package com.example.data.remote

import com.example.data.local.ChatMessageEntity
import com.example.data.preferences.SettingsRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiClient(
    private val settingsRepository: SettingsRepository
) {
    companion object {
        // Model mandated by Gemini API guidelines for general text & conversation
        const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    /**
     * Sends conversation history and latest prompt to Gemini API.
     */
    suspend fun generateReply(
        prompt: String,
        recentHistory: List<ChatMessageEntity> = emptyList(),
        languageOverride: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = settingsRepository.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is missing. Please configure it in Settings or via Secrets.")
            )
        }

        val preferredLanguage = languageOverride ?: settingsRepository.getPreferredLanguage()
        val systemPrompt = buildSystemInstruction(preferredLanguage)

        // Build contents list from recent conversation turns
        val contentsList = mutableListOf<ContentDto>()

        // Include up to 8 recent turns for context
        val contextTurns = recentHistory.takeLast(8)
        for (item in contextTurns) {
            val role = if (item.role == "user") "user" else "model"
            contentsList.add(
                ContentDto(
                    role = role,
                    parts = listOf(PartDto(text = item.content))
                )
            )
        }

        // Add the current user query
        contentsList.add(
            ContentDto(
                role = "user",
                parts = listOf(PartDto(text = prompt))
            )
        )

        val geminiRequest = GeminiRequest(
            contents = contentsList,
            systemInstruction = ContentDto(
                parts = listOf(PartDto(text = systemPrompt))
            ),
            generationConfig = GenerationConfigDto(
                temperature = 0.7f,
                topP = 0.95f,
                topK = 40,
                maxOutputTokens = 2048
            )
        )

        val jsonBody = requestAdapter.toJson(geminiRequest)
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, responseString)
                return@withContext Result.failure(IOException(errorMsg))
            }

            val parsedResponse = responseAdapter.fromJson(responseString)
            val replyText = parsedResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (!replyText.isNullOrBlank()) {
                Result.success(replyText.trim())
            } else {
                Result.failure(IOException("Empty response received from Stonic AI network."))
            }
        } catch (e: Exception) {
            val readableMessage = when (e) {
                is IOException -> "Network error: Unable to reach Gemini servers. Please check your internet connection."
                else -> e.message ?: "An unexpected error occurred while contacting Stonic AI."
            }
            Result.failure(IOException(readableMessage, e))
        }
    }

    /**
     * Verifies if a user-supplied API key is active and valid.
     */
    suspend fun testApiKey(testKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (testKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be empty."))
        }

        val testRequest = GeminiRequest(
            contents = listOf(
                ContentDto(
                    role = "user",
                    parts = listOf(PartDto(text = "Respond in 5 words confirming your core systems are operational."))
                )
            ),
            generationConfig = GenerationConfigDto(maxOutputTokens = 40)
        )

        val jsonBody = requestAdapter.toJson(testRequest)
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        val url = "$BASE_URL/$MODEL_NAME:generateContent?key=${testKey.trim()}"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val parsed = responseAdapter.fromJson(responseString)
                val reply = parsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Operational"
                Result.success("Success: $reply")
            } else {
                val errorDesc = parseErrorMessage(response.code, responseString)
                Result.failure(IOException(errorDesc))
            }
        } catch (e: Exception) {
            Result.failure(IOException("Connection failed: ${e.message}"))
        }
    }

    private fun parseErrorMessage(code: Int, body: String): String {
        return try {
            val parsed = responseAdapter.fromJson(body)
            val apiMsg = parsed?.error?.message
            when {
                code == 400 -> "Invalid API Key or Bad Request: ${apiMsg ?: "Check your API key"}"
                code == 403 -> "Access Forbidden: ${apiMsg ?: "Check API key permissions"}"
                code == 429 -> "Rate Limit Exceeded: Gemini API quota reached. Please wait or use a different key."
                code in 500..599 -> "Gemini Service Error ($code): Google servers are momentarily busy."
                !apiMsg.isNullOrBlank() -> "Error ($code): $apiMsg"
                else -> "HTTP $code: Request failed."
            }
        } catch (_: Exception) {
            "HTTP $code: Network request failed."
        }
    }

    private fun buildSystemInstruction(preferredLanguage: String): String {
        val languageDirective = when (preferredLanguage.lowercase()) {
            "urdu" -> "CRITICAL LANGUAGE REQUIREMENT: You MUST answer strictly in fluent, natural Urdu (اردو) script."
            "roman urdu" -> "CRITICAL LANGUAGE REQUIREMENT: You MUST answer strictly in natural Roman Urdu (Urdu written in English alphabet, e.g. 'Ji Sir, main aapki poori madad kar sakta hoon. Aaj ka mausam kaisa hai?')."
            "english" -> "CRITICAL LANGUAGE REQUIREMENT: Answer strictly in clear, professional English."
            else -> "Detect the user's language and respond naturally in the same language (English, Urdu, or Roman Urdu)."
        }

        return """
            You are Stonic AI, an advanced futuristic personal AI assistant inspired by JARVIS.
            Your creator and user is on an Android smartphone.
            IMPORTANT: Always address the user politely and respectfully as "Sir" (e.g., "Right away, Sir", "Yes Sir, I am on it", "Sir, here is the answer").
            Personality: Helpful, intelligent, friendly, and professional. Speak with confidence and warmth like a trusted digital confidant.
            $languageDirective
            Formatting: Keep responses natural for a voice assistant. Avoid excessive markdown bullet lists for brief answers unless explaining step-by-step instructions. Keep conversational answers concise and punchy for spoken audio.
        """.trimIndent()
    }
}
