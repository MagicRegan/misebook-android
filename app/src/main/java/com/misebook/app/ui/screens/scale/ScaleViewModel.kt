package com.misebook.app.ui.screens.scale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misebook.app.data.repository.RecipeRepository
import com.misebook.app.domain.model.MeasurementUnit
import com.misebook.app.domain.model.Recipe
import com.misebook.app.domain.scaling.ScaleReason
import com.misebook.app.domain.scaling.ScaledRecipe
import com.misebook.app.domain.scaling.ScalingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ScaleMode { SERVINGS, INGREDIENT }

data class ScaleUiState(
    val recipe: Recipe? = null,
    val mode: ScaleMode = ScaleMode.SERVINGS,
    val targetServings: Int = 1,
    val anchorIngredientId: String? = null,
    val anchorAvailableQty: String = "",
    val anchorAvailableUnit: MeasurementUnit = MeasurementUnit.NONE,
    val scaled: ScaledRecipe? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ScaleViewModel @Inject constructor(
    private val repo: RecipeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScaleUiState())
    val state: StateFlow<ScaleUiState> = _state

    fun load(id: String) {
        viewModelScope.launch {
            val r = repo.get(id) ?: return@launch
            val firstMeasurable = r.ingredients.firstOrNull { it.quantity != null && it.unit != MeasurementUnit.NONE }
            _state.value = ScaleUiState(
                recipe = r,
                targetServings = r.servings ?: 1,
                anchorIngredientId = firstMeasurable?.id,
                anchorAvailableUnit = firstMeasurable?.unit ?: MeasurementUnit.NONE,
                anchorAvailableQty = firstMeasurable?.quantity?.toString().orEmpty()
            )
            recompute()
        }
    }

    fun setMode(mode: ScaleMode) {
        _state.value = _state.value.copy(mode = mode)
        recompute()
    }

    fun setTargetServings(v: Int) {
        _state.value = _state.value.copy(targetServings = v.coerceAtLeast(1))
        if (_state.value.mode == ScaleMode.SERVINGS) recompute()
    }

    fun setAnchor(ingredientId: String) {
        val ing = _state.value.recipe?.ingredients?.firstOrNull { it.id == ingredientId }
        _state.value = _state.value.copy(
            anchorIngredientId = ingredientId,
            anchorAvailableUnit = ing?.unit ?: MeasurementUnit.NONE,
            anchorAvailableQty = ing?.quantity?.toString().orEmpty()
        )
        if (_state.value.mode == ScaleMode.INGREDIENT) recompute()
    }

    fun setAvailableQty(v: String) {
        _state.value = _state.value.copy(anchorAvailableQty = v)
        if (_state.value.mode == ScaleMode.INGREDIENT) recompute()
    }

    fun setAvailableUnit(u: MeasurementUnit) {
        _state.value = _state.value.copy(anchorAvailableUnit = u)
        if (_state.value.mode == ScaleMode.INGREDIENT) recompute()
    }

    private fun recompute() {
        val s = _state.value
        val r = s.recipe ?: return
        val result: ScaledRecipe? = when (s.mode) {
            ScaleMode.SERVINGS -> ScalingEngine.scaleByServings(r, s.targetServings).takeIf { true }
            ScaleMode.INGREDIENT -> {
                val id = s.anchorIngredientId ?: return
                val qty = s.anchorAvailableQty.replace(',', '.').toDoubleOrNull()
                if (qty == null) null
                else ScalingEngine.scaleByIngredient(r, id, qty, s.anchorAvailableUnit)
            }
        }
        _state.value = s.copy(
            scaled = result,
            errorMessage = when {
                s.mode == ScaleMode.INGREDIENT && result == null -> "Units don't match or value is invalid."
                else -> null
            }
        )
    }

    fun reasonLabel(): String = when (val reason = _state.value.scaled?.reason) {
        is ScaleReason.Servings -> "Scaled from ${reason.from} to ${reason.to} servings"
        is ScaleReason.Ingredient -> "Scaled around ${reason.name}: ${reason.originalQty} ${reason.originalUnit.displayShort} → ${reason.availableQty} ${reason.availableUnit.displayShort}"
        is ScaleReason.Identity, null -> ""
    }
}
