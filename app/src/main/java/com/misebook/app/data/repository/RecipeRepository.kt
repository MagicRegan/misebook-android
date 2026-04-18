package com.misebook.app.data.repository

import com.misebook.app.data.db.dao.RecipeDao
import com.misebook.app.data.db.entity.DirectionEntity
import com.misebook.app.data.db.entity.IngredientEntity
import com.misebook.app.data.db.entity.RecipeEntity
import com.misebook.app.domain.model.Direction
import com.misebook.app.domain.model.Recipe
import com.misebook.app.domain.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val dao: RecipeDao
) {
    fun observe(id: String): Flow<Recipe?> = dao.observe(id).map { it?.toDomain() }

    suspend fun get(id: String): Recipe? = dao.get(id)?.toDomain()

    fun observeForWorkspace(workspaceId: String): Flow<List<Recipe>> =
        dao.observeForWorkspace(workspaceId).map { list -> list.map { it.toDomain() } }

    fun observeRecent(workspaceId: String, limit: Int = 6): Flow<List<Recipe>> =
        dao.observeRecent(workspaceId, limit).map { list -> list.map { it.toDomain() } }

    fun search(workspaceId: String, query: String, categoryId: String?): Flow<List<Recipe>> =
        dao.search(workspaceId, query, categoryId).map { list -> list.map { it.toDomain() } }

    suspend fun markOpened(id: String) = dao.markOpened(id)

    suspend fun save(recipe: Recipe): Recipe {
        val now = System.currentTimeMillis()
        val id = recipe.id.ifBlank { UUID.randomUUID().toString() }
        val createdAt = if (recipe.createdAt == 0L) now else recipe.createdAt
        val entity = RecipeEntity(
            id = id,
            workspaceId = recipe.workspaceId,
            name = recipe.name.trim().ifBlank { "Untitled recipe" },
            categoryId = recipe.categoryId,
            subcategoryId = recipe.subcategoryId,
            prepTimeMin = recipe.prepTimeMin,
            cookTimeMin = recipe.cookTimeMin,
            yieldAmount = recipe.yieldAmount,
            yieldUnit = recipe.yieldUnit,
            servings = recipe.servings,
            notes = recipe.notes?.trim(),
            imagePath = recipe.imagePath,
            sourceUrl = recipe.sourceUrl,
            createdAt = createdAt,
            updatedAt = now,
            lastOpenedAt = now
        )
        val ings = recipe.ingredients.mapIndexed { idx, it ->
            IngredientEntity(
                id = it.id.ifBlank { UUID.randomUUID().toString() },
                recipeId = id,
                position = idx,
                quantity = it.quantity,
                unit = it.unit,
                name = it.name.trim(),
                note = it.note?.trim()?.ifBlank { null }
            )
        }
        val dirs = recipe.directions.mapIndexed { idx, it ->
            DirectionEntity(
                id = it.id.ifBlank { UUID.randomUUID().toString() },
                recipeId = id,
                position = idx,
                text = it.text.trim()
            )
        }
        dao.saveFull(entity, ings, dirs)
        return recipe.copy(id = id, createdAt = createdAt, updatedAt = now)
    }

    suspend fun duplicate(id: String, newName: String? = null): Recipe? {
        val existing = dao.get(id)?.toDomain() ?: return null
        val copyName = newName ?: "${existing.name} (copy)"
        val fresh = existing.copy(
            id = UUID.randomUUID().toString(),
            name = copyName,
            ingredients = existing.ingredients.map { it.copy(id = UUID.randomUUID().toString()) },
            directions = existing.directions.map { Direction(UUID.randomUUID().toString(), it.position, it.text) },
            createdAt = 0L,
            updatedAt = 0L
        )
        return save(fresh)
    }

    suspend fun delete(id: String) = dao.delete(id)
}
