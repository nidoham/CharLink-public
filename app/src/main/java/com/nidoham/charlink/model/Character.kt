package com.nidoham.charlink.model

import android.os.Parcelable
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

/**
 * Character Data Model.
 * Optimized for Firebase (No-arg constructor support via defaults)
 * and Jetpack Compose (Immutability).
 */
@IgnoreExtraProperties
@Parcelize
data class Character(
    // ==================== Identifiers ====================
    val cid: String = "",
    val uid: String = "", // Creator's UID

    // ==================== Basic Info ====================
    val name: String = "",
    val persona: String = "",
    val photoUrl: String = "", // Main profile image
    val age: Int = 0,
    val gender: String = "",
    val greeting: String = "",

    // ==================== Related Data ====================
    // Defaulting to emptyList() prevents nulls from Firebase
    val categories: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val backgroundImages: List<String> = emptyList(),
    val bannerImages: List<String> = emptyList(),

    // ==================== Status & Access ====================
    val diamonds: Long = 0L,
    val privateCharacter: Boolean = false,
    val premium: Boolean = false,
    val verified: Boolean = false,
    val banned: Boolean = false,

    // ==================== Timestamps ====================
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // ==================== Statistics ====================
    val characterLikes: Long = 0L,
    val characterViews: Long = 0L,
    val characterChats: Long = 0L

) : Parcelable {

    // ==================== Logic / Helper Methods ====================

    /**
     * Checks if the character data is valid for saving.
     */
    @Exclude
    fun isValid(): Boolean {
        return cid.isNotEmpty() && uid.isNotEmpty() && name.isNotEmpty()
    }

    /**
     * Checks if the character is generally accessible (not banned, valid data).
     */
    @Exclude
    fun isAccessible(): Boolean {
        return !banned && isValid()
    }

    /**
     * Determines if a specific viewer can see this character.
     * Handles private character logic.
     */
    @Exclude
    fun canView(viewerUid: String): Boolean {
        if (!isAccessible()) return false
        if (!privateCharacter) return true
        // If private, only the creator can view it
        return uid == viewerUid
    }

    /**
     * Since this is an immutable data class, we don't use a 'touch()' method inside setters.
     * Instead, when updating, use this helper to get a fresh copy with updated time.
     * Example: val updatedChar = oldChar.updateTimestamp()
     */
    fun updateTimestamp(): Character {
        return this.copy(updatedAt = System.currentTimeMillis())
    }
}