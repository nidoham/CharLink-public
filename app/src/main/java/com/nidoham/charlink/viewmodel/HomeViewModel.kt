package com.nidoham.charlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nidoham.charlink.firebase.CharacterHelper
import com.nidoham.charlink.model.Character
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    // Best practice: Inject this via Constructor if using Hilt/Koin.
    // For now, we keep it simple as per your context.
    private val characterHelper = CharacterHelper()

    // UI State: Characters List
    private val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters: StateFlow<List<Character>> = _characters.asStateFlow()

    // UI State: Loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // UI State: Error Message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // UI State: Selected Category
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    init {
        // Initial Load
        loadData("All")
    }

    /**
     * Called when user clicks a category chip/tab
     */
    fun onCategorySelected(category: String) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            loadData(category)
        }
    }

    /**
     * Called when user pulls to refresh
     */
    fun refresh() {
        loadData(_selectedCategory.value)
    }

    private fun loadData(category: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null // Clear previous errors

            // Logic Split:
            // 1. Special Filters (New, Trending, Popular) -> use getCharactersByFilter
            // 2. Content Categories (Anime, RPG, etc.) -> use searchCharacters with category param

            val result = when (category) {
                "All" -> characterHelper.getCharactersByFilter(CharacterHelper.FilterType.ALL)
                "New" -> characterHelper.getCharactersByFilter(CharacterHelper.FilterType.NEW)
                "Trending" -> characterHelper.getCharactersByFilter(CharacterHelper.FilterType.TRENDING)
                "Popular" -> characterHelper.getCharactersByFilter(CharacterHelper.FilterType.POPULAR)
                else -> {
                    // Assuming 'category' is a specific genre (e.g., "Fantasy", "Realistic")
                    // We use searchCharacters because it supports filtering by specific category
                    characterHelper.searchCharacters(queryText = "", category = category)
                }
            }

            result.onSuccess { list ->
                _characters.value = list
            }.onFailure { error ->
                _errorMessage.value = error.localizedMessage ?: "Failed to load characters"
                _characters.value = emptyList()
            }

            _isLoading.value = false
        }
    }
}