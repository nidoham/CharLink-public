package com.nidoham.charlink.model

import android.os.Parcelable
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.android.parcel.Parcelize

/**
 * অ্যাপ্লিকেশনের ব্যবহারকারীর ডেটা মডেল।
 * Jetpack Compose এবং Firebase এর জন্য অপটিমাইজড।
 */
@IgnoreExtraProperties
@Parcelize // নেভিগেশনের সময় অবজেক্ট পাস করার জন্য (build.gradle এ plugin যোগ করতে হবে)
data class User(
    // ==================== মূল বৈশিষ্ট্য ====================
    val uid: String = "",
    val name: String = "",
    val persona: String = "",
    val photoUrl: String = "",
    val dob: Int = 0,
    val gender: String = "",

    // ==================== অ্যাকাউন্ট স্ট্যাটাস ====================
    val privateAccount: Boolean = false,
    val verified: Boolean = false,
    val banned: Boolean = false,
    val premium: Boolean = false,

    // ==================== আর্থিক তথ্য ====================
    val accountBalance: Long = 0L,
    val diamonds: Long = 0L,

    // ==================== সময় সম্পর্কিত তথ্য ====================
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val loginStreak: Int = 0,
    val lastOnline: Long = 0L,

    // ==================== সামাজিক পরিসংখ্যান ====================
    val followers: Long = 0L,
    val following: Long = 0L,

    // ==================== কন্টেন্ট ক্রিয়েটর পরিসংখ্যান ====================
    val creatorCharacters: Long = 0L,
    val creatorLikes: Long = 0L,
    val creatorViews: Long = 0L,
    val creatorChats: Long = 0L,

    // ==================== প্রোফাইল পরিসংখ্যান ====================
    val profileViews: Long = 0L,
    val profileLikes: Long = 0L,
    val profileChats: Long = 0L
) : Parcelable {

    // ==================== সাহায্যকারী মেথড (Helpers) ====================

    /**
     * ব্যবহারকারীর তথ্য বৈধ কি না তা পরীক্ষা করে।
     */
    @Exclude
    fun isValid(): Boolean {
        return uid.isNotEmpty()
    }

    /**
     * Companion Object: স্ট্যাটিক মেথড বা ফ্যাক্টরি মেথডের জন্য
     */
    companion object {
        /**
         * FirebaseUser অবজেক্ট থেকে User মডেল তৈরি করে।
         */
        fun fromFirebaseUser(firebaseUser: FirebaseUser): User {
            return User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName ?: "",
                photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                // বাকি সব ফিল্ড ডিফল্ট মান নিয়ে নেবে
            )
        }
    }
}