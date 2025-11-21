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
 * ChatViewModel with AI integration
 * Manages chat messages and AI responses
 */
class ChatViewModel : ViewModel() {

    private val messageHelper = MessageHelper()
    private val geminiHandler = GeminiApiHandler()

    // Current character ID being chatted with
    private val _currentCharacterId = MutableStateFlow<String?>(null)
    val currentCharacterId: StateFlow<String?> = _currentCharacterId.asStateFlow()

    // Messages list
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // AI response state
    private val _aiResponseState = MutableStateFlow<GeminiApiHandler.GeminiResult?>(null)
    val aiResponseState: StateFlow<GeminiApiHandler.GeminiResult?> = _aiResponseState.asStateFlow()

    // Loading state for message operations
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Job for message listener (to cancel when switching characters)
    private var messageListenerJob: Job? = null

    private lateinit var persona: String

    // --------------------------------------------------------
    // INITIALIZATION
    // --------------------------------------------------------

    /**
     * Start listening to messages for a specific character
     * Cancels previous listener if exists
     */
    fun startChatWithCharacter(characterId: String, persona: String) {
        this.persona = persona

        if (_currentCharacterId.value == characterId) {
            return // Already listening to this character
        }

        // Cancel previous listener
        messageListenerJob?.cancel()

        // Update current character
        _currentCharacterId.value = characterId

        // Clear previous messages and states
        _messages.value = emptyList()
        _aiResponseState.value = null
        _error.value = null

        // Start new listener
        messageListenerJob = viewModelScope.launch {
            messageHelper.fetchMessagesFlow(characterId)
                .catch { e ->
                    _error.value = "Failed to load messages: ${e.message}"
                }
                .collect { list ->
                    _messages.value = list
                }
        }
    }

    // --------------------------------------------------------
    // SEND USER MESSAGE
    // --------------------------------------------------------

    /**
     * Send a user message and optionally trigger AI response
     */
    fun sendMessage(
        text: String,
        characterId: String,
        triggerAiResponse: Boolean = true
    ) {
        if (text.isBlank()) {
            _error.value = "Message cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Create and send user message
                val userMessage = Message.createUserMessage(
                    text = text.trim(),
                    senderId = "", // Will be set by MessageHelper
                    receiverId = characterId
                )

                val success = messageHelper.pushMessage(characterId, userMessage)

                if (success) {
                    // Trigger AI response if requested
                    if (triggerAiResponse) {
                        generateAiResponse(text, characterId)
                    }
                } else {
                    _error.value = "Failed to send message"
                }
            } catch (e: Exception) {
                _error.value = "Error sending message: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --------------------------------------------------------
    // AI RESPONSE GENERATION
    // --------------------------------------------------------

    /**
     * Generate AI response based on user message and conversation history
     */
    private fun generateAiResponse(userMessage: String, characterId: String) {
        viewModelScope.launch {
            // Build context from recent messages
            val conversationContext = buildConversationContext(userMessage)

            // Generate AI response using Flow
            geminiHandler.generateContentFlow(conversationContext)
                .collect { result ->
                    _aiResponseState.value = result

                    // If successful, save AI message to database
                    if (result is GeminiApiHandler.GeminiResult.Success) {
                        saveAiMessage(result.text, characterId)
                    } else if (result is GeminiApiHandler.GeminiResult.Error) {
                        _error.value = "AI Error: ${result.message}"
                    }
                }
        }
    }

    /**
     * Generate streaming AI response (for real-time text display)
     */
    fun generateStreamingAiResponse(userMessage: String, characterId: String) {
        viewModelScope.launch {
            val conversationContext = buildConversationContext(userMessage)

            geminiHandler.generateContentStream(conversationContext)
                .collect { result ->
                    _aiResponseState.value = result

                    // Save final response when streaming completes
                    if (result is GeminiApiHandler.GeminiResult.Success) {
                        // Check if this is the final chunk (you may need additional logic)
                        saveAiMessage(result.text, characterId)
                    }
                }
        }
    }

    /**
     * Build conversation context for AI prompt
     * Includes recent message history for better context
     */
    /**
     * Build conversation context using PromptManager
     */
    private fun buildConversationContext(currentMessage: String): String {
        // 1. Get recent messages (e.g., last 10)
        // Note: Assuming _messages is ordered newest-first, we take 10 then reverse to get chronological order
        val recentMessages = _messages.value.take(10).reversed()

        // 2. Convert your Message objects to the String format PromptManager expects
        val formattedHistory = recentMessages.map { msg ->
            val prefix = if (msg.sentByUser) "User" else "Char" // or use actual character name
            "$prefix: ${msg.text}"
        }

        // 4. Construct the System Instruction (Base + Persona)
        val fullSystemInstruction = PromptManager.buildSystemInstruction(persona)

        // 5. Initialize PromptManager
        val promptManager = PromptManager(
            systemInstruction = fullSystemInstruction,
            currentQuery = currentMessage,
            chatHistory = formattedHistory,
        )

        return promptManager.buildPrompt()
    }

    /**
     * Save AI-generated message to database
     */
    private suspend fun saveAiMessage(text: String, characterId: String) {
        try {
            val aiMessage = Message.createReceivedMessage(
                text = text,
                senderId = characterId,
                receiverId = "" // Will be set by MessageHelper
            )

            val success = messageHelper.pushMessage(characterId, aiMessage)

            if (!success) {
                _error.value = "Failed to save AI response"
            } else {
                // Clear AI response state after saving
                _aiResponseState.value = null
            }
        } catch (e: Exception) {
            _error.value = "Error saving AI message: ${e.message}"
        }
    }

    // --------------------------------------------------------
    // MESSAGE OPERATIONS
    // --------------------------------------------------------

    /**
     * Delete a message
     */
    fun deleteMessage(characterId: String, messageId: String, deleteForEveryone: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val success = messageHelper.removeMessage(characterId, messageId, deleteForEveryone)

                if (!success) {
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
     * Send a quick reply (emoji or pre-defined text)
     */
    fun sendQuickReply(emoji: String, characterId: String) {
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

    // --------------------------------------------------------
    // STATE MANAGEMENT
    // --------------------------------------------------------

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clear AI response state
     */
    fun clearAiResponse() {
        _aiResponseState.value = null
    }

    // --------------------------------------------------------
    // LIFECYCLE
    // --------------------------------------------------------

    override fun onCleared() {
        super.onCleared()
        messageListenerJob?.cancel()
    }
}