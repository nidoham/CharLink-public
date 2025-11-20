package com.nidoham.charlink.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nidoham.charlink.model.Character
import kotlinx.coroutines.tasks.await

class CharacterHelper {

    // Reference path: /characters/
    private val db = FirebaseDatabase.getInstance()
    private val charactersRef = db.getReference("characters")

    // ================================================================================
    // 1. FETCH SINGLE CHARACTER
    // Path: characters/{charId}
    // ================================================================================

    suspend fun getCharacter(cid: String): Result<Character> {
        return try {
            // Direct path to the character ID
            val snapshot = charactersRef.child(cid).get().await()
            val character = snapshot.getValue(Character::class.java)

            if (character != null) {
                // Ensure the CID in the object matches the database Key
                Result.success(character.copy(cid = snapshot.key ?: cid))
            } else {
                Result.failure(Exception("Character not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ================================================================================
    // 2. FEED LISTS
    // Path: characters/ (Ordered by children)
    // ================================================================================

    suspend fun getCharactersByFilter(filterType: FilterType, limit: Int = 20): Result<List<Character>> {
        return try {
            // 1. Determine sorting key
            val query = when (filterType) {
                FilterType.NEW -> charactersRef.orderByChild("createdAt")
                FilterType.TRENDING,
                FilterType.POPULAR -> charactersRef.orderByChild("characterChats") // Ensure index exists in rules
                FilterType.ALL -> charactersRef.orderByChild("createdAt")
            }

            // 2. Fetch LAST items (Highest values = Newest/Most Popular)
            val snapshot = query.limitToLast(limit).get().await()

            // 3. Parse, Filter, and Reverse (to show Newest first)
            val characters = snapshot.children.mapNotNull { doc ->
                val char = doc.getValue(Character::class.java)
                // FIX: Map the Database Key to the Object CID
                char?.copy(cid = doc.key ?: "")
            }.filter { char ->
                // Logic: Must not be banned and not private (unless handled elsewhere)
                !char.banned && !char.privateCharacter
            }.reversed()

            Result.success(characters)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ================================================================================
    // 3. SEARCH
    // Path: characters/ (Ordered by name or date)
    // ================================================================================

    suspend fun searchCharacters(
        queryText: String,
        category: String? = null
    ): Result<List<Character>> {
        return try {
            val query = if (queryText.isNotEmpty()) {
                charactersRef.orderByChild("name")
                    .startAt(queryText)
                    .endAt(queryText + "\uf8ff")
                    .limitToFirst(50)
            } else {
                charactersRef.orderByChild("createdAt").limitToLast(50)
            }

            val snapshot = query.get().await()

            var resultList = snapshot.children.mapNotNull { doc ->
                val char = doc.getValue(Character::class.java)
                // FIX: Map the Database Key to the Object CID
                char?.copy(cid = doc.key ?: "")
            }

            // If NOT searching by name, reverse to show newest first
            if (queryText.isEmpty()) {
                resultList = resultList.reversed()
            }

            // Client-side Filtering
            val filteredList = resultList.filter { char ->
                val isNotBanned = !char.banned && !char.privateCharacter
                val matchesCategory = if (category.isNullOrEmpty() || category == "All") {
                    true
                } else {
                    char.categories.contains(category)
                }
                isNotBanned && matchesCategory
            }

            Result.success(filteredList)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ================================================================================
    // 4. SAVE (Create or Update)
    // Path: characters/{charId}
    // ================================================================================

    suspend fun saveCharacter(character: Character): Result<Unit> {
        return try {
            // FIX: Generate ID if empty (Create new), otherwise use existing (Update)
            val charId = if (character.cid.isEmpty()) {
                charactersRef.push().key ?: return Result.failure(Exception("Failed to generate ID"))
            } else {
                character.cid
            }

            // Update the object with the correct ID before saving
            val characterToSave = character.copy(cid = charId)

            // Save to characters/charId/*
            charactersRef.child(charId).setValue(characterToSave).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ================================================================================
    // 5. OBSERVE (Realtime)
    // Path: characters/{charId}
    // ================================================================================

    fun observeCharacter(cid: String, onUpdate: (Character?) -> Unit) {
        charactersRef.child(cid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val char = snapshot.getValue(Character::class.java)
                // FIX: Ensure CID matches key
                onUpdate(char?.copy(cid = snapshot.key ?: cid))
            }
            override fun onCancelled(error: DatabaseError) {
                // Optional: Log error or handle it
            }
        })
    }

    enum class FilterType {
        ALL, NEW, TRENDING, POPULAR
    }
}