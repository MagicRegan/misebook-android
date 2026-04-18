package com.misebook.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misebook.app.data.repository.RecipeRepository
import com.misebook.app.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recipeRepo: RecipeRepository
) : ViewModel() {

    private val workspaceId = MutableStateFlow<String?>(null)

    val recent: StateFlow<List<Recipe>> = workspaceId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else recipeRepo.observeRecent(id, limit = 6)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setWorkspace(id: String?) { workspaceId.value = id }
}
