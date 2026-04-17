package com.misebook.app.data.repository

import com.misebook.app.data.db.dao.CategoryDao
import com.misebook.app.data.db.entity.CategoryEntity
import com.misebook.app.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val dao: CategoryDao
) {
    fun observeForWorkspace(workspaceId: String): Flow<List<Category>> =
        dao.observeForWorkspace(workspaceId).map { list ->
            list.map { Category(it.id, it.workspaceId, it.name, it.parentId) }
        }

    fun observeTopLevel(workspaceId: String): Flow<List<Category>> =
        dao.observeTopLevel(workspaceId).map { list ->
            list.map { Category(it.id, it.workspaceId, it.name, it.parentId) }
        }

    fun observeChildren(parentId: String): Flow<List<Category>> =
        dao.observeChildren(parentId).map { list ->
            list.map { Category(it.id, it.workspaceId, it.name, it.parentId) }
        }

    suspend fun upsert(category: Category) {
        dao.upsert(
            CategoryEntity(
                id = category.id,
                workspaceId = category.workspaceId,
                name = category.name,
                parentId = category.parentId
            )
        )
    }

    suspend fun delete(id: String) = dao.delete(id)
}
