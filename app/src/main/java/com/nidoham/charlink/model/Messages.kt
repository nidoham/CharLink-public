package com.nidoham.charlink.model

import java.text.SimpleDateFormat
import java.util.*

/**
 * Chat message data model.
 * This class holds information for each message.
 */
data class Messages(
    var id: String? = null,
    var text: String? = null,
    var timestamp: Long = System.currentTimeMillis(),
    var senderId: String? = null,
    var receiverId: String? = null,
    var sentByUser: Boolean = false,
    var status: MessageStatus = MessageStatus.SENT,
    var reactions: List<String>? = null
) {
    /**
     * Enum for tracking message status.
     */
    enum class MessageStatus {
        SENT,      // Message sent
        DELIVERED, // Delivered to recipient
        READ       // Read by recipient
    }

    // ==================== Helper Methods ====================

    /**
     * Checks if the message is from AI/bot.
     * @return true if message is from AI.
     */
    fun isFromAI(): Boolean = !sentByUser

    /**
     * Checks if the message is empty.
     * @return true if text is null or empty.
     */
    fun isEmpty(): Boolean = text.isNullOrBlank()

    /**
     * Checks if the message has reactions.
     * @return true if reactions list has items.
     */
    fun hasReactions(): Boolean = !reactions.isNullOrEmpty()

    /**
     * Provides formatted time string for display.
     * @return Formatted time (e.g., "HH:mm").
     */
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Provides formatted date string for display.
     * @return Formatted date (e.g., "dd/MM/yyyy").
     */
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Provides full formatted datetime string.
     * @return Formatted datetime (e.g., "dd/MM/yyyy HH:mm").
     */
    fun getFormattedDateTime(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Checks if message was sent today.
     * @return true if message timestamp is from today.
     */
    fun isToday(): Boolean {
        val messageCalendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val todayCalendar = Calendar.getInstance()

        return messageCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                messageCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Checks if message was sent yesterday.
     * @return true if message timestamp is from yesterday.
     */
    fun isYesterday(): Boolean {
        val messageCalendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val yesterdayCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }

        return messageCalendar.get(Calendar.YEAR) == yesterdayCalendar.get(Calendar.YEAR) &&
                messageCalendar.get(Calendar.DAY_OF_YEAR) == yesterdayCalendar.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Provides smart date display (Today, Yesterday, or date).
     * @return Smart formatted date string.
     */
    fun getSmartDateDisplay(): String {
        return when {
            isToday() -> "Today"
            isYesterday() -> "Yesterday"
            else -> getFormattedDate()
        }
    }

    // ==================== Builder Pattern ====================

    /**
     * Builder pattern for easily creating Messages objects.
     */
    class Builder {
        private var id: String? = null
        private var text: String? = null
        private var timestamp: Long = System.currentTimeMillis()
        private var sentByUser: Boolean = false
        private var senderId: String? = null
        private var receiverId: String? = null
        private var status: MessageStatus = MessageStatus.SENT
        private var reactions: List<String>? = null

        fun id(id: String) = apply { this.id = id }
        fun text(text: String) = apply { this.text = text }
        fun timestamp(timestamp: Long) = apply { this.timestamp = timestamp }
        fun sentByUser(sentByUser: Boolean) = apply { this.sentByUser = sentByUser }
        fun senderId(senderId: String) = apply { this.senderId = senderId }
        fun receiverId(receiverId: String) = apply { this.receiverId = receiverId }
        fun status(status: MessageStatus) = apply { this.status = status }
        fun reactions(reactions: List<String>) = apply { this.reactions = reactions }

        fun build(): Messages {
            return Messages(
                id = id,
                text = text,
                timestamp = timestamp,
                senderId = senderId,
                receiverId = receiverId,
                sentByUser = sentByUser,
                status = status,
                reactions = reactions
            )
        }
    }

    companion object {
        /**
         * Creates a new message sent by the user.
         */
        fun createUserMessage(
            text: String,
            senderId: String,
            receiverId: String
        ): Messages {
            return Builder()
                .id(System.currentTimeMillis().toString())
                .text(text)
                .sentByUser(true)
                .senderId(senderId)
                .receiverId(receiverId)
                .build()
        }

        /**
         * Creates a new message from AI/other user.
         */
        fun createReceivedMessage(
            text: String,
            senderId: String,
            receiverId: String
        ): Messages {
            return Builder()
                .id(System.currentTimeMillis().toString())
                .text(text)
                .sentByUser(false)
                .senderId(senderId)
                .receiverId(receiverId)
                .build()
        }

        /**
         * Creates a quick reply message (like thumbs up).
         */
        fun createQuickReply(
            emoji: String,
            senderId: String,
            receiverId: String
        ): Messages {
            return Builder()
                .id(System.currentTimeMillis().toString())
                .text(emoji)
                .sentByUser(true)
                .senderId(senderId)
                .receiverId(receiverId)
                .build()
        }
    }
}

// ==================== Extension Functions ====================

/**
 * Extension function to group messages by date.
 */
fun List<Messages>.groupByDate(): Map<String, List<Messages>> {
    return this.groupBy { it.getSmartDateDisplay() }
}

/**
 * Extension function to get unread messages count.
 */
fun List<Messages>.getUnreadCount(): Int {
    return this.count { !it.sentByUser && it.status != Messages.MessageStatus.READ }
}

/**
 * Extension function to mark all messages as read.
 */
fun List<Messages>.markAllAsRead(): List<Messages> {
    return this.map { message ->
        if (!message.sentByUser) {
            message.copy(status = Messages.MessageStatus.READ)
        } else {
            message
        }
    }
}

/**
 * Extension function to filter messages by sender.
 */
fun List<Messages>.fromSender(senderId: String): List<Messages> {
    return this.filter { it.senderId == senderId }
}

/**
 * Extension function to get last message.
 */
fun List<Messages>.lastMessage(): Messages? = this.maxByOrNull { it.timestamp }