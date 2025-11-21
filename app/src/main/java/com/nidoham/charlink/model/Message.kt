package com.nidoham.charlink.model

import java.text.SimpleDateFormat
import java.util.*

/**
 * Chat message data model.
 * Renamed from 'Messages' to 'Message' (singular convention).
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val senderId: String? = null,
    val receiverId: String? = null,
    val sentByUser: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT,
    val reactions: List<String> = emptyList() // Better to default to emptyList() than null
) {
    enum class MessageStatus { SENT, DELIVERED, READ }

    // ==================== Logic & Formatting ====================

    val isFromAI: Boolean
        get() = !sentByUser

    /**
     * formattedTime property (e.g., "14:30").
     * Using a property is more idiomatic in Kotlin than getFormattedTime().
     */
    val formattedTime: String
        get() = DateHelper.formatTime(timestamp)

    /**
     * Smart date display (Today, Yesterday, or dd/MM/yyyy).
     */
    val smartDateDisplay: String
        get() = when {
            DateHelper.isToday(timestamp) -> "Today"
            DateHelper.isYesterday(timestamp) -> "Yesterday"
            else -> DateHelper.formatDate(timestamp)
        }

    // ==================== Factory Methods ====================

    companion object {
        fun createUserMessage(text: String, senderId: String, receiverId: String) = Message(
            text = text,
            sentByUser = true,
            senderId = senderId,
            receiverId = receiverId
        )

        fun createReceivedMessage(text: String, senderId: String, receiverId: String) = Message(
            text = text,
            sentByUser = false,
            senderId = senderId,
            receiverId = receiverId
        )

        fun createQuickReply(emoji: String, senderId: String, receiverId: String) = Message(
            text = emoji,
            sentByUser = true,
            senderId = senderId,
            receiverId = receiverId
        )
    }
}

// ==================== Internal Helpers ====================

/**
 * Helper for Date formatting and comparison.
 * Optimized for Thread Safety and Performance.
 */
private object DateHelper {
    // FIX: SimpleDateFormat is NOT thread-safe. We must use ThreadLocal to avoid crashes/corruption.
    private val timeFormat = ThreadLocal.withInitial { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    private val dateFormat = ThreadLocal.withInitial { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    fun formatTime(time: Long): String = timeFormat.get()?.format(Date(time)) ?: ""

    fun formatDate(time: Long): String = dateFormat.get()?.format(Date(time)) ?: ""

    fun isToday(time: Long): Boolean {
        return isSameDay(time, System.currentTimeMillis())
    }

    fun isYesterday(time: Long): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(time, yesterday.timeInMillis)
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

// ==================== Extension Functions ====================

/**
 * Groups messages by their smart date (Today, Yesterday, Date).
 */
fun List<Message>.groupByDate(): Map<String, List<Message>> =
    this.groupBy { it.smartDateDisplay }

/**
 * Counts unread messages sent by others.
 */
fun List<Message>.getUnreadCount(): Int =
    this.count { !it.sentByUser && it.status != Message.MessageStatus.READ }

/**
 * Returns a new list with all received messages marked as READ.
 */
fun List<Message>.markAllAsRead(): List<Message> =
    this.map {
        if (!it.sentByUser && it.status != Message.MessageStatus.READ) {
            it.copy(status = Message.MessageStatus.READ)
        } else {
            it
        }
    }

fun List<Message>.fromSender(senderId: String): List<Message> =
    this.filter { it.senderId == senderId }

/**
 * Gets the last message based on timestamp.
 */
fun List<Message>.lastMessage(): Message? =
    this.maxByOrNull { it.timestamp }