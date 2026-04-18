package com.misebook.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.misebook.app.data.db.converter.Converters
import com.misebook.app.data.db.dao.CategoryDao
import com.misebook.app.data.db.dao.RecipeDao
import com.misebook.app.data.db.dao.WorkspaceDao
import com.misebook.app.data.db.entity.CategoryEntity
import com.misebook.app.data.db.entity.DirectionEntity
import com.misebook.app.data.db.entity.IngredientEntity
import com.misebook.app.data.db.entity.RecipeEntity
import com.misebook.app.data.db.entity.WorkspaceEntity

@Database(
    entities = [
        WorkspaceEntity::class,
        RecipeEntity::class,
        IngredientEntity::class,
        DirectionEntity::class,
        CategoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MiseBookDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun recipeDao(): RecipeDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val NAME = "misebook.db"
    }
}
