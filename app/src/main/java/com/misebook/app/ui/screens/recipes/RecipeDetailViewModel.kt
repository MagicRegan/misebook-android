package com.misebook.app.ui.screens.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misebook.app.data.repository.RecipeRepository
import com.misebook.app.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val repo: RecipeRepository
) : ViewModel() {
    private val _recipe = MutableStateFlow<Recipe?>(null)
    val recipe: StateFlow<Recipe?> = _recipe
    private var currentId: String? = null
    private var collectJob: Job? = null

    fun load(id: String) {
        if (currentId == id && collectJob?.isActive == true) return
        currentId = id
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            repo.markOpened(id)
            repo.observe(id).collect { _recipe.value = it }
        }
    }

    suspend fun duplicate(): String? {
        val id = currentId ?: return null
        return repo.duplicate(id)?.id
    }

    suspend fun delete() {
        currentId?.let { repo.delete(it) }
    }
}
