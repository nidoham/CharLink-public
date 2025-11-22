package com.nidoham.charlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nidoham.charlink.firebase.MessageHelper
import com.nidoham.charlink.firebase.ai.GeminiApiHandler
import com.nidoham.charlink.firebase.prompt.PromptManager
import com.nidoham.charlink.model.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ChatViewModel with AI integration.
 * Manages chat messages and AI responses.
 */
class ChatViewModel : ViewModel() {

    private val messageHelper = MessageHelper()
    private val geminiHandler = GeminiApiHandler()

    private val _currentCharacterId = MutableStateFlow<String?>(null)
    val currentCharacterId: StateFlow<String?> = _currentCharacterId.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _aiResponseState = MutableStateFlow<GeminiApiHandler.GeminiResult?>(null)
    val aiResponseState: StateFlow<GeminiApiHandler.GeminiResult?> = _aiResponseState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var messageListenerJob: Job? = null

    private lateinit var persona: String

    /**
     * Start listening to messages for a specific character; cancels previous listener if any.
     */
    fun startChatWithCharacter(characterId: String, persona: String) {
        if (characterId.isBlank()) {
            _error.value = "Character ID cannot be empty"
            return
        }

        this.persona = persona

        if (_currentCharacterId.value == characterId) {
            return
        }

        messageListenerJob?.cancel()

        _currentCharacterId.value = characterId
        _messages.value = emptyList()
        _aiResponseState.value = null
        _error.value = null

        messageListenerJob = viewModelScope.launch {
            messageHelper.fetchMessagesFlow(characterId)
                .catch { e -> _error.value = "Failed to load messages: ${e.message}" }
                .collect { list -> _messages.value = list }
        }
    }

    /**
     * Send a user message and optionally trigger AI response.
     */
    fun sendMessage(text: String, characterId: String, triggerAiResponse: Boolean = true) {
        if (text.isBlank()) {
            _error.value = "Message cannot be empty"
            return
        }
        if (characterId.isBlank()) {
            _error.value = "Character ID cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val userMessage = Message.createUserMessage(
                    text = text.trim(),
                    senderId = "", // Set appropriately if known
                    receiverId = characterId
                )
                val success = messageHelper.pushMessage(characterId, userMessage)

                if (success && triggerAiResponse) {
                    generateAiResponse(text, characterId)
                } else if (!success) {
                    _error.value = "Failed to send message"
                }
            } catch (e: Exception) {
                _error.value = "Error sending message: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Generate AI response as a coroutine flow.
     */
    private fun generateAiResponse(userMessage: String, characterId: String) {
        viewModelScope.launch {
            val conversationContext = buildConversationContext(userMessage)
            geminiHandler.generateContentFlow(conversationContext)
                .collect { result ->
                    _aiResponseState.value = result
                    when (result) {
                        is GeminiApiHandler.GeminiResult.Success -> saveAiMessage(result.text, characterId)
                        is GeminiApiHandler.GeminiResult.Error -> _error.value = "AI Error: ${result.message}"
                        else -> {}
                    }
                }
        }
    }

    /**
     * Generate streaming AI response for real-time display.
     */
    fun generateStreamingAiResponse(userMessage: String, characterId: String) {
        viewModelScope.launch {
            val conversationContext = buildConversationContext(userMessage)
            geminiHandler.generateContentStream(conversationContext)
                .collect { result ->
                    _aiResponseState.value = result
                    if (result is GeminiApiHandler.GeminiResult.Success) {
                        saveAiMessage(result.text, characterId)
                    }
                }
        }
    }

    /**
     * Build conversation context for AI prompt, including persona.
     */
    private fun buildConversationContext(currentMessage: String): String {
        val recentMessages = _messages.value.takeLast(10)
        val formattedHistory = recentMessages.map { msg ->
            val prefix = if (msg.sentByUser) "User" else "Char"
            "$prefix: ${msg.text}"
        }

        val fullSystemInstruction = PromptManager.buildSystemInstruction(persona)

        val promptManager = PromptManager(
            systemInstruction = fullSystemInstruction,
            currentQuery = currentMessage,
            persona = persona,
            memories = emptyList() // Add memories if applicable
        )

        return promptManager.buildPrompt()
    }

    /**
     * Save AI-generated message to database.
     */
    private suspend fun saveAiMessage(text: String, characterId: String) {
        try {
            val aiMessage = Message.createReceivedMessage(
                text = text,
                senderId = characterId,
                receiverId = "" // Set if known
            )
            val success = messageHelper.pushMessage(characterId, aiMessage)
            if (!success) _error.value = "Failed to save AI response"
            else _aiResponseState.value = null
        } catch (e: Exception) {
            _error.value = "Error saving AI message: ${e.message}"
        }
    }

    /**
     * Delete message, optionally for everyone.
     */
    fun deleteMessage(characterId: String, messageId: String, deleteForEveryone: Boolean = false) {
        if (characterId.isBlank()) {
            _error.value = "Character ID cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                if (!messageHelper.removeMessage(characterId, messageId, deleteForEveryone)) {
                    _error.value = "Failed to delete message"
                }
            } catch (e: Exception) {
                _error.value = "Error deleting message: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Send quick reply (emoji or predefined).
     */
    fun sendQuickReply(emoji: String, characterId: String) {
        if (characterId.isBlank()) {
            _error.value = "Character ID cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                val quickReply = Message.createQuickReply(
                    emoji = emoji,
                    senderId = "",
                    receiverId = characterId
                )
                messageHelper.pushMessage(characterId, quickReply)
            } catch (e: Exception) {
                _error.value = "Error sending quick reply: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearAiResponse() {
        _aiResponseState.value = null
    }

    override fun onCleared() {
        super.onCleared()
        messageListenerJob?.cancel()
    }
}
