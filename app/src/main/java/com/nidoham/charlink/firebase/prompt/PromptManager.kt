package com.nidoham.charlink.firebase.prompt

/**
 * Immutable class for building structured prompts for character AI interactions.
 * Optimized for Kotlin with Data Classes and Named Arguments.
 *
 * @property systemInstruction Complete system instruction including base + persona.
 * @property currentQuery Current user message.
 * @property persona Character's persona description (optional).
 * @property greeting Default greeting template (optional).
 * @property tags Character traits/attributes (optional).
 * @property memories Important information to remember (optional).
 * @property chatHistory Recent conversation context (optional).
 * @version 2.0
 */
data class PromptManager(
    val systemInstruction: String,
    val currentQuery: String,
    val persona: String? = null,
    val greeting: String? = null,
    val tags: List<String> = emptyList(),
    val memories: List<String> = emptyList(),
    val chatHistory: List<String> = emptyList()
) {

    companion object {
        /**
         * Creates system instruction by combining base system prompt with character persona.
         *
         * @param persona Character's persona description
         * @return Complete system instruction
         */
        fun buildSystemInstruction(persona: String): String {
            // Accessing the Singleton Object from the previous file
            return SystemInstruction.getCurrentPrompt() + "\n\n" +
                    "══════════ CHARACTER PERSONA ══════════\n" + persona
        }
    }

    /**
     * Builds the complete prompt for AI model consumption.
     * Formats all components into a clean, structured prompt.
     *
     * @return Formatted prompt string
     */
    fun buildPrompt(): String = buildString {
        // System Instruction (always present)
        append(systemInstruction).append("\n\n")

        // Character Persona (if provided separately)
        if (!persona.isNullOrEmpty()) {
            append("══════════ CHARACTER PERSONA ══════════\n")
            append(persona).append("\n\n")
        }

        // Greeting Template (for first message context)
        if (!greeting.isNullOrEmpty()) {
            append("══════════ DEFAULT GREETING ══════════\n")
            append("When starting a new conversation, use this greeting style:\n")
            append(greeting).append("\n\n")
        }

        // Tags (character traits, attributes)
        if (tags.isNotEmpty()) {
            append("══════════ CHARACTER TAGS ══════════\n")
            append(tags.joinToString(", ")).append("\n\n")
        }

        // Memories (important information to remember)
        if (memories.isNotEmpty()) {
            append("══════════ MEMORIES ══════════\n")
            append("Important information you remember:\n")
            memories.forEach { memory ->
                append("• ").append(memory).append("\n")
            }
            append("\n")
        }

        // Chat History (recent conversation context)
        if (chatHistory.isNotEmpty()) {
            append("══════════ CONVERSATION HISTORY ══════════\n")
            chatHistory.forEach { chat ->
                append(chat).append("\n")
            }
            append("\n")
        }

        // Current User Query (what user just said)
        append("══════════ CURRENT MESSAGE ══════════\n")
        append(currentQuery)
    }

    /**
     * Alternative prompt format optimized for API with system instruction separation.
     * Use this when your API supports separate system instruction parameter.
     *
     * @return User-facing prompt without system instruction
     */
    fun buildUserPrompt(): String = buildString {
        // Character Context
        if (!persona.isNullOrEmpty()) {
            append("Character Context:\n").append(persona).append("\n\n")
        }

        if (!greeting.isNullOrEmpty()) {
            append("Greeting Style: ").append(greeting).append("\n\n")
        }

        if (tags.isNotEmpty()) {
            append("Traits: ").append(tags.joinToString(", ")).append("\n\n")
        }

        // Conversation State
        if (memories.isNotEmpty()) {
            append("Remembered:\n")
            memories.forEach { m -> append("• ").append(m).append("\n") }
            append("\n")
        }

        if (chatHistory.isNotEmpty()) {
            append("Recent Conversation:\n")
            chatHistory.forEach { chat -> append(chat).append("\n") }
            append("\n")
        }

        // Current Input
        append(currentQuery)
    }

    init {
        require(systemInstruction.isNotBlank()) { "System instruction cannot be null or empty" }
        require(currentQuery.isNotBlank()) { "Current query cannot be null or empty" }
    }
}