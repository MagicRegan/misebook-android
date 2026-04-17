package com.misebook.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misebook.app.data.repository.RecipeRepository
import com.misebook.app.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: RecipeRepository
) : ViewModel() {

    private val workspaceId = MutableStateFlow<String?>(null)
    val query = MutableStateFlow("")

    val results: StateFlow<List<Recipe>> =
        combine(workspaceId, query.debounce(200L)) { id, q -> id to q }
            .flatMapLatest { (id, q) ->
                if (id == null) flowOf(emptyList())
                else repo.search(id, q, null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setWorkspace(id: String?) { workspaceId.value = id }
    fun setQuery(v: String) { query.value = v }
}
