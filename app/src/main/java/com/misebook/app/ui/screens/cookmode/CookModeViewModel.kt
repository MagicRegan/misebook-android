package com.misebook.app.ui.screens.cookmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misebook.app.data.repository.RecipeRepository
import com.misebook.app.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CookModeViewModel @Inject constructor(
    private val repo: RecipeRepository
) : ViewModel() {
    private val _recipe = MutableStateFlow<Recipe?>(null)
    val recipe: StateFlow<Recipe?> = _recipe

    fun load(id: String) {
        viewModelScope.launch {
            _recipe.value = repo.get(id)
        }
    }
}
