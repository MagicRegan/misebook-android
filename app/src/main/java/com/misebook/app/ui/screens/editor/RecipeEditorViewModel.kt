package com.misebook.app.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misebook.app.data.repository.RecipeRepository
import com.misebook.app.domain.model.Direction
import com.misebook.app.domain.model.Ingredient
import com.misebook.app.domain.model.MeasurementUnit
import com.misebook.app.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class EditorState(
    val recipe: Recipe = Recipe(id = "", workspaceId = "", name = ""),
    val isNew: Boolean = true,
    val nameError: String? = null,
    val saving: Boolean = false
)

@HiltViewModel
class RecipeEditorViewModel @Inject constructor(
    private val repo: RecipeRepository
) : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state

    fun loadExisting(id: String) {
        viewModelScope.launch {
            val existing = repo.get(id) ?: return@launch
            _state.value = EditorState(recipe = existing, isNew = false)
        }
    }

    fun initNew(workspaceId: String) {
        _state.value = EditorState(
            recipe = Recipe(
                id = UUID.randomUUID().toString(),
                workspaceId = workspaceId,
                name = "",
                ingredients = listOf(emptyIngredient(0)),
                directions = listOf(emptyDirection(0))
            ),
            isNew = true
        )
    }

    fun initFromParsed(workspaceId: String, parsed: Recipe) {
        _state.value = EditorState(
            recipe = parsed.copy(workspaceId = workspaceId, id = UUID.randomUUID().toString()),
            isNew = true
        )
    }

    fun updateName(v: String) = update { it.copy(name = v) }
    fun updatePrep(v: Int?) = update { it.copy(prepTimeMin = v) }
    fun updateCook(v: Int?) = update { it.copy(cookTimeMin = v) }
    fun updateServings(v: Int?) = update { it.copy(servings = v) }
    fun updateYield(amount: Double?, unit: String?) = update { it.copy(yieldAmount = amount, yieldUnit = unit) }
    fun updateNotes(v: String) = update { it.copy(notes = v) }
    fun updateImage(v: String?) = update { it.copy(imagePath = v) }
    fun updateSourceUrl(v: String?) = update { it.copy(sourceUrl = v) }
    fun updateCategory(cat: String?, sub: String?) = update { it.copy(categoryId = cat, subcategoryId = sub) }

    fun addIngredient() = update { it.copy(ingredients = it.ingredients + emptyIngredient(it.ingredients.size)) }
    fun removeIngredient(id: String) = update { it.copy(ingredients = it.ingredients.filter { ing -> ing.id != id }.reindexIng()) }
    fun updateIngredient(id: String, transform: (Ingredient) -> Ingredient) = update {
        it.copy(ingredients = it.ingredients.map { ing -> if (ing.id == id) transform(ing) else ing })
    }
    fun moveIngredient(from: Int, to: Int) = update {
        val list = it.ingredients.toMutableList()
        if (from !in list.indices || to !in list.indices) return@update it
        val item = list.removeAt(from)
        list.add(to, item)
        it.copy(ingredients = list.reindexIng())
    }

    fun addDirection() = update { it.copy(directions = it.directions + emptyDirection(it.directions.size)) }
    fun removeDirection(id: String) = update { it.copy(directions = it.directions.filter { d -> d.id != id }.reindexDir()) }
    fun updateDirection(id: String, text: String) = update {
        it.copy(directions = it.directions.map { d -> if (d.id == id) d.copy(text = text) else d })
    }

    fun save(onSuccess: (String) -> Unit) {
        val current = _state.value.recipe
        if (current.name.isBlank()) {
            _state.value = _state.value.copy(nameError = "Please add a recipe name")
            return
        }
        _state.value = _state.value.copy(saving = true, nameError = null)
        viewModelScope.launch {
            val cleaned = current.copy(
                ingredients = current.ingredients.filter { it.name.isNotBlank() || (it.quantity ?: 0.0) > 0.0 },
                directions = current.directions.filter { it.text.isNotBlank() }
            )
            val saved = repo.save(cleaned)
            _state.value = _state.value.copy(saving = false, recipe = saved, isNew = false)
            onSuccess(saved.id)
        }
    }

    private fun update(transform: (Recipe) -> Recipe) {
        _state.value = _state.value.copy(recipe = transform(_state.value.recipe))
    }

    private fun emptyIngredient(position: Int) = Ingredient(
        id = UUID.randomUUID().toString(),
        position = position,
        quantity = null,
        unit = MeasurementUnit.NONE,
        name = ""
    )

    private fun emptyDirection(position: Int) = Direction(
        id = UUID.randomUUID().toString(),
        position = position,
        text = ""
    )

    private fun List<Ingredient>.reindexIng() = mapIndexed { idx, it -> it.copy(position = idx) }
    private fun List<Direction>.reindexDir() = mapIndexed { idx, it -> it.copy(position = idx) }
}
