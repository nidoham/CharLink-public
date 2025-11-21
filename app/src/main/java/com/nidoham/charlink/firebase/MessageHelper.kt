package com.nidoham.charlink.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.nidoham.charlink.model.Message // Updated to Singular 'Message'
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MessageHelper {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference

    /**
     * Pushes a new message to the database.
     * Path: /chats/{uid}/{characterId}/messages/{messageId}
     */
    suspend fun pushMessage(characterId: String, message: Message): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false

        // 1. Generate a Firebase-compatible unique key (Time-sortable)
        val messageId = db.child("chats").push().key ?: return false

        // 2. Prepare the final message object
        // We overwrite the ID with the Firebase key and ensure IDs are correct
        val messageToSave = message.copy(
            id = messageId,
            senderId = currentUserId,
            receiverId = characterId
        )

        val updates = HashMap<String, Any>()

        // Path 1: Sender's History (User -> Character)
        // /chats/uid/characterId/messages/msgId
        updates["/chats/$currentUserId/$characterId/messages/$messageId"] = messageToSave

        // Path 2: Receiver's History (Character -> User)
        // /chats/characterId/uid/messages/msgId
        // NOTE: Only needed if the 'Character' is another User or if the Bot backend reads from here.
        updates["/chats/$characterId/$currentUserId/messages/$messageId"] = messageToSave

        return try {
            db.updateChildren(updates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Real-time fetch of messages between Current User and a Character.
     * Returns a Flow that emits the list of messages whenever the database changes.
     */
    fun fetchMessagesFlow(characterId: String): Flow<List<Message>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            close()
            return@callbackFlow
        }

        val query = db.child("chats")
            .child(currentUserId)
            .child(characterId)
            .child("messages")
            .orderByChild("timestamp") // Ensures we get them in chronological order

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messagesList = ArrayList<Message>()

                for (child in snapshot.children) {
                    try {
                        // Deserialize to your Message data class
                        val msg = child.getValue(Message::class.java)
                        if (msg != null) {
                            messagesList.add(msg)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Firebase returns Oldest -> Newest.
                // If your UI uses reverseLayout=true (Standard Chat UI),
                // you usually want the Newest at index 0.
                messagesList.reverse()

                trySend(messagesList)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)

        // cleanup when the scope is cancelled
        awaitClose { query.removeEventListener(listener) }
    }

    /**
     * Removes a message for the current user.
     */
    suspend fun removeMessage(characterId: String, messageId: String, deleteForEveryone: Boolean = false): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false

        val updates = HashMap<String, Any?>()

        // Remove from current user's view
        updates["/chats/$currentUserId/$characterId/messages/$messageId"] = null

        // If "Delete for Everyone", remove from receiver's (Character's) view too
        if (deleteForEveryone) {
            updates["/chats/$characterId/$currentUserId/messages/$messageId"] = null
        }

        return try {
            db.updateChildren(updates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}