package com.misebook.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.misebook.app.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE workspaceId = :workspaceId ORDER BY sortOrder ASC, name ASC")
    fun observeForWorkspace(workspaceId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE workspaceId = :workspaceId AND parentId IS NULL ORDER BY sortOrder ASC, name ASC")
    fun observeTopLevel(workspaceId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY sortOrder ASC, name ASC")
    fun observeChildren(parentId: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: String)
}
