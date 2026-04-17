package com.misebook.app.domain.model

import com.misebook.app.data.db.dao.RecipeWithChildren

/** UI-facing models. Separated from Room entities so layers can evolve independently. */

data class Workspace(
    val id: String,
    val name: String,
    val accent: String = "copper"
)

data class Category(
    val id: String,
    val workspaceId: String,
    val name: String,
    val parentId: String? = null
)

data class Ingredient(
    val id: String,
    val position: Int,
    val quantity: Double?,
    val unit: MeasurementUnit,
    val name: String,
    val note: String? = null
) {
    val displayQuantity: String get() = formatQuantity(quantity)
    val displayLine: String get() = buildString {
        if (quantity != null) {
            append(formatQuantity(quantity))
            if (unit != MeasurementUnit.NONE) {
                append(' ').append(unit.displayShort)
            }
            append(' ')
        }
        append(name)
        if (!note.isNullOrBlank()) append(", ").append(note)
    }
}

data class Direction(
    val id: String,
    val position: Int,
    val text: String
)

data class Recipe(
    val id: String,
    val workspaceId: String,
    val name: String,
    val categoryId: String? = null,
    val subcategoryId: String? = null,
    val prepTimeMin: Int? = null,
    val cookTimeMin: Int? = null,
    val yieldAmount: Double? = null,
    val yieldUnit: String? = null,
    val servings: Int? = null,
    val notes: String? = null,
    val imagePath: String? = null,
    val sourceUrl: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val directions: List<Direction> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val isSaved: Boolean get() = createdAt > 0L
    val totalTimeMin: Int? get() = listOfNotNull(prepTimeMin, cookTimeMin).sum().takeIf { it > 0 }
}

fun RecipeWithChildren.toDomain(): Recipe = Recipe(
    id = recipe.id,
    workspaceId = recipe.workspaceId,
    name = recipe.name,
    categoryId = recipe.categoryId,
    subcategoryId = recipe.subcategoryId,
    prepTimeMin = recipe.prepTimeMin,
    cookTimeMin = recipe.cookTimeMin,
    yieldAmount = recipe.yieldAmount,
    yieldUnit = recipe.yieldUnit,
    servings = recipe.servings,
    notes = recipe.notes,
    imagePath = recipe.imagePath,
    sourceUrl = recipe.sourceUrl,
    ingredients = ingredients
        .sortedBy { it.position }
        .map { Ingredient(it.id, it.position, it.quantity, it.unit, it.name, it.note) },
    directions = directions
        .sortedBy { it.position }
        .map { Direction(it.id, it.position, it.text) },
    createdAt = recipe.createdAt,
    updatedAt = recipe.updatedAt
)

private fun formatQuantity(q: Double?): String {
    if (q == null) return ""
    if (q == q.toLong().toDouble()) return q.toLong().toString()
    val rounded = Math.round(q * 100.0) / 100.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
    else rounded.toString().trimEnd('0').trimEnd('.')
}
