package com.lapakhunian.app.core.data.di

import android.content.Context
import androidx.room.Room
import com.lapakhunian.app.core.data.local.AppDatabase
import com.lapakhunian.app.core.data.local.PropertyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "lapakhunian.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun providePropertyDao(db: AppDatabase): PropertyDao = db.propertyDao()
}
