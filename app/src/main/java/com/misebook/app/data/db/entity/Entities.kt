package com.misebook.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.misebook.app.domain.model.MeasurementUnit
import java.util.UUID

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val accent: String = "copper",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories", indices = [Index(value = ["workspaceId"])])
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val name: String,
    val parentId: String? = null,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "recipes",
    indices = [
        Index(value = ["workspaceId"]),
        Index(value = ["categoryId"]),
        Index(value = ["subcategoryId"])
    ]
)
data class RecipeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null
)

@Entity(
    tableName = "ingredients",
    foreignKeys = [ForeignKey(
        entity = RecipeEntity::class,
        parentColumns = ["id"],
        childColumns = ["recipeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["recipeId"])]
)
data class IngredientEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recipeId: String,
    val position: Int,
    val quantity: Double?,
    val unit: MeasurementUnit = MeasurementUnit.NONE,
    val name: String,
    val note: String? = null
)

@Entity(
    tableName = "directions",
    foreignKeys = [ForeignKey(
        entity = RecipeEntity::class,
        parentColumns = ["id"],
        childColumns = ["recipeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["recipeId"])]
)
data class DirectionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recipeId: String,
    val position: Int,
    val text: String
)
