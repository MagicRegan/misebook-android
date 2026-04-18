package com.misebook.app.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.misebook.app.data.db.entity.DirectionEntity
import com.misebook.app.data.db.entity.IngredientEntity
import com.misebook.app.data.db.entity.RecipeEntity
import kotlinx.coroutines.flow.Flow

data class RecipeWithChildren(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<IngredientEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val directions: List<DirectionEntity>
)

@Dao
interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observe(id: String): Flow<RecipeWithChildren?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun get(id: String): RecipeWithChildren?

    @Transaction
    @Query("SELECT * FROM recipes WHERE workspaceId = :workspaceId ORDER BY updatedAt DESC")
    fun observeForWorkspace(workspaceId: String): Flow<List<RecipeWithChildren>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE workspaceId = :workspaceId ORDER BY COALESCE(lastOpenedAt, updatedAt) DESC LIMIT :limit")
    fun observeRecent(workspaceId: String, limit: Int): Flow<List<RecipeWithChildren>>

    @Transaction
    @Query(
        """
        SELECT * FROM recipes
        WHERE workspaceId = :workspaceId
          AND (:query = '' OR LOWER(name) LIKE '%' || LOWER(:query) || '%')
          AND (:categoryId IS NULL OR categoryId = :categoryId)
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun search(workspaceId: String, query: String, categoryId: String?): Flow<List<RecipeWithChildren>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecipe(r: RecipeEntity)

    @Update
    suspend fun updateRecipe(r: RecipeEntity)

    @Query("UPDATE recipes SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun markOpened(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun clearIngredients(recipeId: String)

    @Query("DELETE FROM directions WHERE recipeId = :recipeId")
    suspend fun clearDirections(recipeId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ings: List<IngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirections(dirs: List<DirectionEntity>)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM recipes WHERE workspaceId = :workspaceId")
    suspend fun deleteForWorkspace(workspaceId: String)

    @Transaction
    suspend fun saveFull(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        directions: List<DirectionEntity>
    ) {
        upsertRecipe(recipe)
        clearIngredients(recipe.id)
        clearDirections(recipe.id)
        if (ingredients.isNotEmpty()) insertIngredients(ingredients)
        if (directions.isNotEmpty()) insertDirections(directions)
    }
}
