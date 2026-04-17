package com.misebook.app.di

import android.content.Context
import androidx.room.Room
import com.misebook.app.data.db.MiseBookDatabase
import com.misebook.app.data.db.dao.CategoryDao
import com.misebook.app.data.db.dao.RecipeDao
import com.misebook.app.data.db.dao.WorkspaceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): MiseBookDatabase =
        Room.databaseBuilder(ctx, MiseBookDatabase::class.java, MiseBookDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideWorkspaceDao(db: MiseBookDatabase): WorkspaceDao = db.workspaceDao()
    @Provides fun provideRecipeDao(db: MiseBookDatabase): RecipeDao = db.recipeDao()
    @Provides fun provideCategoryDao(db: MiseBookDatabase): CategoryDao = db.categoryDao()

    @Provides @Singleton
    fun provideAppContext(@ApplicationContext ctx: Context): Context = ctx
}
