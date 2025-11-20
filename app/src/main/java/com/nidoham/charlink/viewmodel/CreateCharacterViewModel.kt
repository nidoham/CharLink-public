package com.nidoham.charlink.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.nidoham.charlink.imgbb.ImgBBUploader
import com.nidoham.charlink.model.Character
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class CreateCharState {
    data object Idle : CreateCharState()
    data object Loading : CreateCharState()
    data object Success : CreateCharState()
    data class Error(val message: String) : CreateCharState()
}

class CreateCharacterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<CreateCharState>(CreateCharState.Idle)
    val uiState: StateFlow<CreateCharState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("characters")

    fun createCharacter(
        context: Context,
        imageUri: Uri?,
        name: String,
        age: String,
        traits: List<String>,
        backgroundStory: String,
        addressAs: String
    ) {
        // 1. Validation
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = CreateCharState.Error("You must be logged in.")
            return
        }
        if (name.isBlank() || age.isBlank()) {
            _uiState.value = CreateCharState.Error("Name and Age are required.")
            return
        }
        if (imageUri == null) {
            _uiState.value = CreateCharState.Error("Please select an image.")
            return
        }

        // 2. Process
        viewModelScope.launch {
            _uiState.value = CreateCharState.Loading

            // Step A: Upload Image to ImgBB
            val uploadResult = ImgBBUploader.uploadImage(context, imageUri)

            uploadResult.onSuccess { imageUrl ->
                // Step B: Create Database Key
                val newCharRef = database.push()
                val cid = newCharRef.key

                if (cid != null) {
                    // Step C: Create Character Object
                    val character = Character(
                        cid = cid,
                        uid = currentUser.uid,
                        name = name,
                        age = age.toIntOrNull() ?: 0,
                        persona = backgroundStory,
                        greeting = addressAs,
                        photoUrl = imageUrl,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    // Step D: Save to Firebase Realtime Database
                    try {
                        newCharRef.setValue(character).await()
                        _uiState.value = CreateCharState.Success
                    } catch (e: Exception) {
                        _uiState.value = CreateCharState.Error("Database Error: ${e.message}")
                    }
                } else {
                    _uiState.value = CreateCharState.Error("Failed to generate ID")
                }
            }

            uploadResult.onFailure { error ->
                _uiState.value = CreateCharState.Error("Image Upload Failed: ${error.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = CreateCharState.Idle
    }
}