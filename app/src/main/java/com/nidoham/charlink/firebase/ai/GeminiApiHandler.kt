package com.nidoham.charlink.firebase.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.nidoham.charlink.firebase.model.GeminiApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Handler class for Gemini API interactions
 * Optimized for Jetpack Compose with Flow-based reactive patterns
 */
class GeminiApiHandler {

    companion object {
        private const val TAG = "GeminiApiHandler"
        private val MODEL_NAME = GeminiApi.GEMINI_API_FREE_TEXT
    }

    // Sealed class for handling different result states
    sealed class GeminiResult {
        data object Loading : GeminiResult()
        data class Success(val text: String) : GeminiResult()
        data class Error(val message: String, val exception: Exception? = null) : GeminiResult()
    }

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(MODEL_NAME)
    }

    /**
     * Generate content as Flow (recommended for Compose)
     * Automatically emits Loading -> Success/Error states
     *
     * Usage in ViewModel:
     * ```
     * viewModelScope.launch {
     *     geminiHandler.generateContentFlow(prompt)
     *         .collect { result ->
     *             _uiState.value = result
     *         }
     * }
     * ```
     */
    fun generateContentFlow(prompt: String): Flow<GeminiResult> = flow {
        emit(GeminiResult.Loading)

        try {
            val response = model.generateContent(prompt)
            val responseText = response.text

            if (responseText.isNullOrBlank()) {
                emit(GeminiResult.Error(
                    message = "Empty response received",
                    exception = Exception("No text in response")
                ))
            } else {
                Log.d(TAG, "Response received: ${responseText.take(100)}...")
                emit(GeminiResult.Success(responseText))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content", e)
            emit(GeminiResult.Error(
                message = e.message ?: "Unknown error occurred",
                exception = e
            ))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Generate content with suspend function (alternative approach)
     * Returns result directly without Flow
     *
     * Usage in ViewModel:
     * ```
     * viewModelScope.launch {
     *     _uiState.value = GeminiResult.Loading
     *     _uiState.value = geminiHandler.generateContent(prompt)
     * }
     * ```
     */
    suspend fun generateContent(prompt: String): GeminiResult {
        return withContext(Dispatchers.IO) {
            try {
                val response = model.generateContent(prompt)
                val responseText = response.text

                if (responseText.isNullOrBlank()) {
                    GeminiResult.Error(
                        message = "Empty response received",
                        exception = Exception("No text in response")
                    )
                } else {
                    Log.d(TAG, "Response received: ${responseText.take(100)}...")
                    GeminiResult.Success(responseText)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating content", e)
                GeminiResult.Error(
                    message = e.message ?: "Unknown error occurred",
                    exception = e
                )
            }
        }
    }

    /**
     * Stream content as it's generated (for real-time responses)
     * Emits partial text as it arrives
     */
    fun generateContentStream(prompt: String): Flow<GeminiResult> = flow {
        emit(GeminiResult.Loading)

        try {
            val response = model.generateContentStream(prompt)
            val fullText = StringBuilder()

            response.collect { chunk ->
                chunk.text?.let { text ->
                    fullText.append(text)
                    emit(GeminiResult.Success(fullText.toString()))
                }
            }

            if (fullText.isEmpty()) {
                emit(GeminiResult.Error(
                    message = "Empty response received",
                    exception = Exception("No text in response")
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error streaming content", e)
            emit(GeminiResult.Error(
                message = e.message ?: "Unknown error occurred",
                exception = e
            ))
        }
    }.flowOn(Dispatchers.IO)
}