package com.nidoham.charlink.firebase.prompt

/**
 * Immutable class for building structured prompts for character AI interactions.
 * Optimized for Kotlin with Data Classes and Named Arguments.
 *
 * @property systemInstruction Complete system instruction including base + persona.
 * @property currentQuery Current user message.
 * @property persona Character's persona description (optional).
 * @property memories Important information to remember (optional).
 * @version 2.0
 */
data class PromptManager(
    val systemInstruction: String,
    val currentQuery: String,
    val persona: String? = null,
    val memories: List<String> = emptyList(),
) {

    companion object {
        /**
         * Creates system instruction by combining base system prompt with character persona.
         *
         * @param persona Character's persona description.
         * @return Complete system instruction.
         */
        fun buildSystemInstruction(persona: String): String {
            return SystemInstruction.current() + "\n\n" +
                    "══════════ CHARACTER PERSONA ══════════\n" + persona
        }
    }

    /**
     * Builds the complete prompt for AI model consumption.
     * Formats all components into a clean, structured prompt.
     *
     * @return Formatted prompt string.
     */
    fun buildPrompt(): String = buildString {
        append(systemInstruction).append("\n\n")

        if (!persona.isNullOrBlank()) {
            append("══════════ CHARACTER PERSONA ══════════\n")
            append(persona).append("\n\n")
        }

        if (memories.isNotEmpty()) {
            append("══════════ MEMORIES ══════════\n")
            append("Important information you remember:\n")
            memories.forEach { append("• ").append(it).append("\n") }
            append("\n")
        }

        append("══════════ CURRENT MESSAGE ══════════\n")
        append(currentQuery)
    }

    /**
     * Builds a user-facing prompt (without system instruction).
     *
     * @return User-facing prompt string.
     */
    fun buildUserPrompt(): String = buildString {
        if (!persona.isNullOrBlank()) {
            append("Character Context:\n").append(persona).append("\n\n")
        }

        if (memories.isNotEmpty()) {
            append("Remembered:\n")
            memories.forEach { append("• ").append(it).append("\n") }
            append("\n")
        }

        append(currentQuery)
    }

    init {
        require(systemInstruction.isNotBlank()) { "System instruction cannot be null or empty" }
        require(currentQuery.isNotBlank()) { "Current query cannot be null or empty" }
    }
}
