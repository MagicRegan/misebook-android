package com.misebook.app.ui.screens.importer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misebook.app.data.repository.RecipeRepository
import com.misebook.app.domain.importer.ImageImporter
import com.misebook.app.domain.importer.ImportSource
import com.misebook.app.domain.importer.ParsedRecipe
import com.misebook.app.domain.importer.PdfImporter
import com.misebook.app.domain.importer.RecipeTextParser
import com.misebook.app.domain.importer.UrlImporter
import com.misebook.app.domain.model.Direction
import com.misebook.app.domain.model.Ingredient
import com.misebook.app.domain.model.MeasurementUnit
import com.misebook.app.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class ImportUiState(
    val isImporting: Boolean = false,
    val parsed: ParsedRecipe? = null,
    val source: ImportSource? = null,
    val error: String? = null,
    val urlInput: String = "",
    val freeformText: String = "",
    val recipeDraft: Recipe? = null,
    val saving: Boolean = false
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val imageImporter: ImageImporter,
    private val pdfImporter: PdfImporter,
    private val urlImporter: UrlImporter,
    private val recipeRepo: RecipeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state

    fun onUrlInput(v: String) { _state.value = _state.value.copy(urlInput = v) }
    fun onFreeformInput(v: String) { _state.value = _state.value.copy(freeformText = v) }

    fun importFromImage(uri: Uri, onReady: () -> Unit) {
        launchImport(ImportSource.IMAGE, onReady) { imageImporter.importFromUri(uri) }
    }

    fun importFromPdf(uri: Uri, onReady: () -> Unit) {
        launchImport(ImportSource.PDF, onReady) { pdfImporter.importFromUri(uri) }
    }

    fun importFromUrl(onReady: () -> Unit) {
        val url = _state.value.urlInput.trim()
        if (url.isBlank()) {
            _state.value = _state.value.copy(error = "Please enter a URL")
            return
        }
        launchImport(ImportSource.URL, onReady) { urlImporter.importFromUrl(url) }
    }

    fun importFromFreeform(onReady: () -> Unit) {
        val text = _state.value.freeformText
        if (text.isBlank()) {
            _state.value = _state.value.copy(error = "Please paste some text")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true, error = null, source = ImportSource.TEXT)
            val parsed = withContext(Dispatchers.Default) { RecipeTextParser.parse(text) }
            hydrateDraft(parsed, ImportSource.TEXT)
            _state.value = _state.value.copy(isImporting = false)
            onReady()
        }
    }

    private fun launchImport(source: ImportSource, onReady: () -> Unit, block: suspend () -> ParsedRecipe) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true, error = null, source = source)
            val outcome = runCatching { block() }
            outcome.onSuccess { parsed ->
                hydrateDraft(parsed, source)
                _state.value = _state.value.copy(isImporting = false)
                onReady()
            }.onFailure { t ->
                _state.value = _state.value.copy(isImporting = false, error = "Import failed: ${t.message ?: "unknown"}")
            }
        }
    }

    private fun hydrateDraft(parsed: ParsedRecipe, source: ImportSource) {
        val draft = Recipe(
            id = UUID.randomUUID().toString(),
            workspaceId = "",
            name = parsed.title.ifBlank { "Imported recipe" },
            servings = parsed.servings,
            yieldAmount = null,
            yieldUnit = null,
            sourceUrl = parsed.sourceUrl,
            notes = null,
            ingredients = parsed.ingredients.mapIndexed { idx, pi ->
                Ingredient(
                    id = UUID.randomUUID().toString(),
                    position = idx,
                    quantity = pi.quantity,
                    unit = pi.unit ?: MeasurementUnit.NONE,
                    name = pi.name,
                    note = pi.note
                )
            },
            directions = parsed.directions.mapIndexed { idx, pd ->
                Direction(id = UUID.randomUUID().toString(), position = idx, text = pd.text)
            }
        )
        _state.value = _state.value.copy(parsed = parsed, source = source, recipeDraft = draft)
    }

    fun updateDraft(transform: (Recipe) -> Recipe) {
        val cur = _state.value.recipeDraft ?: return
        _state.value = _state.value.copy(recipeDraft = transform(cur))
    }

    fun save(workspaceId: String, onSaved: (String) -> Unit) {
        val draft = _state.value.recipeDraft ?: return
        if (draft.name.isBlank()) {
            _state.value = _state.value.copy(error = "Recipe needs a name")
            return
        }
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            val saved = recipeRepo.save(draft.copy(workspaceId = workspaceId))
            _state.value = ImportUiState() // reset after save
            onSaved(saved.id)
        }
    }

    fun reset() { _state.value = ImportUiState() }
}
