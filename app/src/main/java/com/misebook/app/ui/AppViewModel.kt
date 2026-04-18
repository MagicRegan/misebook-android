package com.misebook.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misebook.app.data.repository.PreferencesRepository
import com.misebook.app.data.repository.WorkspaceRepository
import com.misebook.app.domain.model.Workspace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppState(
    val onboarded: Boolean = false,
    val workspaces: List<Workspace> = emptyList(),
    val currentWorkspace: Workspace? = null,
    val loading: Boolean = true
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val workspaceRepo: WorkspaceRepository,
    private val prefs: PreferencesRepository
) : ViewModel() {

    val state: StateFlow<AppState> = combine(
        workspaceRepo.observeAll(),
        prefs.currentWorkspaceId,
        prefs.onboarded
    ) { workspaces, currentId, onboarded ->
        val current = workspaces.firstOrNull { it.id == currentId } ?: workspaces.firstOrNull()
        if (current != null && currentId != current.id) {
            viewModelScope.launch { prefs.setCurrentWorkspace(current.id) }
        }
        AppState(onboarded = onboarded, workspaces = workspaces, currentWorkspace = current, loading = false)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    fun createWorkspace(name: String, setCurrent: Boolean = true) {
        viewModelScope.launch {
            val ws = workspaceRepo.create(name)
            if (setCurrent) prefs.setCurrentWorkspace(ws.id)
            prefs.setOnboarded(true)
        }
    }

    fun selectWorkspace(id: String) {
        viewModelScope.launch { prefs.setCurrentWorkspace(id) }
    }

    fun renameWorkspace(id: String, newName: String) {
        viewModelScope.launch { workspaceRepo.rename(id, newName) }
    }

    fun deleteWorkspace(id: String) {
        viewModelScope.launch { workspaceRepo.delete(id) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { prefs.setOnboarded(true) }
    }
}
