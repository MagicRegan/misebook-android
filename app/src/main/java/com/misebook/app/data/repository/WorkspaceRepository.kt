package com.misebook.app.data.repository

import com.misebook.app.data.db.dao.WorkspaceDao
import com.misebook.app.data.db.entity.WorkspaceEntity
import com.misebook.app.domain.model.Workspace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepository @Inject constructor(
    private val dao: WorkspaceDao
) {
    fun observeAll(): Flow<List<Workspace>> = dao.observeAll().map { list ->
        list.map { Workspace(it.id, it.name, it.accent) }
    }

    suspend fun create(name: String, accent: String = "copper"): Workspace {
        val trimmed = name.trim().ifBlank { "My Kitchen" }
        val entity = WorkspaceEntity(name = trimmed, accent = accent)
        dao.upsert(entity)
        return Workspace(entity.id, entity.name, entity.accent)
    }

    suspend fun rename(id: String, newName: String) {
        val existing = dao.get(id) ?: return
        dao.update(existing.copy(name = newName.trim().ifBlank { existing.name }, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun count(): Int = dao.count()
}
