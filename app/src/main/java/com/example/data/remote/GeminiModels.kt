package com.example.data.remote

import com.squareup.moshi.Json

data class GeminiRequest(
    @param:Json(name = "contents") val contents: List<ContentDto>,
    @param:Json(name = "systemInstruction") val systemInstruction: ContentDto? = null,
    @param:Json(name = "generationConfig") val generationConfig: GenerationConfigDto? = null
)

data class ContentDto(
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "parts") val parts: List<PartDto>
)

data class PartDto(
    @param:Json(name = "text") val text: String
)

data class GenerationConfigDto(
    @param:Json(name = "temperature") val temperature: Float = 0.7f,
    @param:Json(name = "topP") val topP: Float = 0.95f,
    @param:Json(name = "topK") val topK: Int = 40,
    @param:Json(name = "maxOutputTokens") val maxOutputTokens: Int = 2048
)

data class GeminiResponse(
    @param:Json(name = "candidates") val candidates: List<CandidateDto>? = null,
    @param:Json(name = "error") val error: ApiErrorDto? = null
)

data class CandidateDto(
    @param:Json(name = "content") val content: ContentDto? = null,
    @param:Json(name = "finishReason") val finishReason: String? = null
)

data class ApiErrorDto(
    @param:Json(name = "code") val code: Int? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "status") val status: String? = null
)
