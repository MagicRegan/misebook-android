package com.misebook.app.domain.scaling

import com.misebook.app.domain.model.Ingredient
import com.misebook.app.domain.model.MeasurementUnit
import com.misebook.app.domain.model.Recipe
import com.misebook.app.domain.model.UnitDimension
import kotlin.math.abs
import kotlin.math.round

/**
 * Core scaling engine.
 *
 * Contract:
 *  - [scaleByServings] multiplies all quantities by targetServings / currentServings.
 *  - [scaleByIngredient] asks "I only have X of ingredient Y — scale the whole recipe
 *    so that ingredient Y matches X, and every other ingredient adjusts proportionally".
 *    Both the available amount and the recipe amount must share a [UnitDimension]
 *    (we never cross MASS ↔ VOLUME ↔ COUNT).
 *  - Conversions happen only within the same dimension using canonical factors:
 *      g ↔ kg, oz ↔ g, lb ↔ kg, ml ↔ l (and all metric ↔ imperial combos of the same
 *      dimension).
 *  - Results are annotated with a [ScaledIngredient] that preserves the original value
 *    for UI comparison.
 */
object ScalingEngine {

    /** Round to chef-friendly precision. Small values keep 2 decimals, larger values
     *  snap to whole units. */
    fun friendlyRound(q: Double): Double {
        val a = abs(q)
        return when {
            a == 0.0 -> 0.0
            a < 1.0 -> round(q * 100.0) / 100.0
            a < 10.0 -> round(q * 10.0) / 10.0
            a < 100.0 -> round(q * 2.0) / 2.0  // nearest 0.5
            else -> round(q)
        }
    }

    fun canConvert(from: MeasurementUnit, to: MeasurementUnit): Boolean =
        from.dimension == to.dimension && from.dimension != UnitDimension.OTHER

    /** Convert [amount] from [from] unit to [to] unit. Throws if incompatible. */
    fun convert(amount: Double, from: MeasurementUnit, to: MeasurementUnit): Double {
        require(canConvert(from, to)) { "Cannot convert ${from.name} to ${to.name}" }
        // to_amount = amount * from.canonicalFactor / to.canonicalFactor
        return amount * from.canonicalFactor / to.canonicalFactor
    }

    fun scaleByServings(recipe: Recipe, targetServings: Int): ScaledRecipe {
        val current = (recipe.servings ?: 1).coerceAtLeast(1)
        val factor = targetServings.toDouble() / current.toDouble()
        return apply(recipe, factor, ScaleReason.Servings(current, targetServings))
    }

    /**
     * Scale the recipe so that [anchorIngredientId] has exactly [availableQuantity]
     * of [availableUnit]. Returns null if the anchor ingredient cannot be scaled
     * (e.g. it has no quantity, or the unit dimensions are incompatible).
     */
    fun scaleByIngredient(
        recipe: Recipe,
        anchorIngredientId: String,
        availableQuantity: Double,
        availableUnit: MeasurementUnit
    ): ScaledRecipe? {
        if (availableQuantity <= 0.0) return null
        val anchor = recipe.ingredients.firstOrNull { it.id == anchorIngredientId } ?: return null
        val originalQty = anchor.quantity ?: return null
        if (originalQty <= 0.0) return null
        val originalUnit = anchor.unit
        // Convert the user's available quantity into the recipe's original unit for the anchor.
        val availableInRecipeUnit = when {
            originalUnit == MeasurementUnit.NONE && availableUnit == MeasurementUnit.NONE ->
                availableQuantity
            originalUnit == MeasurementUnit.NONE || availableUnit == MeasurementUnit.NONE -> return null
            canConvert(availableUnit, originalUnit) -> convert(availableQuantity, availableUnit, originalUnit)
            else -> return null
        }
        val factor = availableInRecipeUnit / originalQty
        return apply(recipe, factor, ScaleReason.Ingredient(anchor.name, originalQty, originalUnit, availableQuantity, availableUnit))
    }

    private fun apply(recipe: Recipe, factor: Double, reason: ScaleReason): ScaledRecipe {
        val scaled = recipe.ingredients.map { ing -> scaleIngredient(ing, factor) }
        val scaledServings = recipe.servings?.let { (it * factor).let { v -> maxOf(1, round(v).toInt()) } }
        val scaledYield = recipe.yieldAmount?.let { friendlyRound(it * factor) }
        return ScaledRecipe(
            source = recipe,
            factor = factor,
            reason = reason,
            ingredients = scaled,
            scaledServings = scaledServings,
            scaledYieldAmount = scaledYield
        )
    }

    private fun scaleIngredient(ing: Ingredient, factor: Double): ScaledIngredient {
        val originalQty = ing.quantity
        if (originalQty == null) {
            return ScaledIngredient(ing, scaledQuantity = null, scaledUnit = ing.unit)
        }
        val raw = originalQty * factor
        val (normalizedQty, normalizedUnit) = normalize(raw, ing.unit)
        return ScaledIngredient(ing, scaledQuantity = friendlyRound(normalizedQty), scaledUnit = normalizedUnit)
    }

    /** Promote/demote units for readability: 1500 g → 1.5 kg, 0.4 kg → 400 g, 1200 ml → 1.2 l. */
    private fun normalize(amount: Double, unit: MeasurementUnit): Pair<Double, MeasurementUnit> {
        return when (unit) {
            MeasurementUnit.GRAM -> if (amount >= 1000.0) amount / 1000.0 to MeasurementUnit.KILOGRAM else amount to unit
            MeasurementUnit.KILOGRAM -> if (amount < 1.0) amount * 1000.0 to MeasurementUnit.GRAM else amount to unit
            MeasurementUnit.MILLILITER -> if (amount >= 1000.0) amount / 1000.0 to MeasurementUnit.LITER else amount to unit
            MeasurementUnit.LITER -> if (amount < 1.0) amount * 1000.0 to MeasurementUnit.MILLILITER else amount to unit
            MeasurementUnit.POUND -> if (amount < 1.0) amount * 16.0 to MeasurementUnit.OUNCE else amount to unit
            MeasurementUnit.OUNCE -> if (amount >= 32.0) amount / 16.0 to MeasurementUnit.POUND else amount to unit
            else -> amount to unit
        }
    }

    /** Convert units within a single measurement (user-initiated). */
    fun convertIngredient(ing: Ingredient, targetSystem: TargetSystem): Ingredient {
        val qty = ing.quantity ?: return ing
        val target = pickTargetUnit(ing.unit, targetSystem) ?: return ing
        if (!canConvert(ing.unit, target)) return ing
        val converted = friendlyRound(convert(qty, ing.unit, target))
        val (normQty, normUnit) = normalize(converted, target)
        return ing.copy(quantity = friendlyRound(normQty), unit = normUnit)
    }

    private fun pickTargetUnit(from: MeasurementUnit, target: TargetSystem): MeasurementUnit? {
        val dim = from.dimension
        return when (target) {
            TargetSystem.METRIC -> when (dim) {
                UnitDimension.MASS -> MeasurementUnit.GRAM
                UnitDimension.VOLUME -> MeasurementUnit.MILLILITER
                else -> null
            }
            TargetSystem.IMPERIAL -> when (dim) {
                UnitDimension.MASS -> MeasurementUnit.OUNCE
                UnitDimension.VOLUME -> MeasurementUnit.FLUID_OUNCE
                else -> null
            }
        }
    }
}

enum class TargetSystem { METRIC, IMPERIAL }

sealed interface ScaleReason {
    data class Servings(val from: Int, val to: Int) : ScaleReason
    data class Ingredient(
        val name: String,
        val originalQty: Double,
        val originalUnit: MeasurementUnit,
        val availableQty: Double,
        val availableUnit: MeasurementUnit
    ) : ScaleReason
    data object Identity : ScaleReason
}

data class ScaledIngredient(
    val original: Ingredient,
    val scaledQuantity: Double?,
    val scaledUnit: MeasurementUnit
) {
    val wasChanged: Boolean get() =
        scaledQuantity != original.quantity || scaledUnit != original.unit
}

data class ScaledRecipe(
    val source: Recipe,
    val factor: Double,
    val reason: ScaleReason,
    val ingredients: List<ScaledIngredient>,
    val scaledServings: Int?,
    val scaledYieldAmount: Double?
)
