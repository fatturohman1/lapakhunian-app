package com.lapakhunian.app.core.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PropertyEntity>)

    @Query("SELECT * FROM properties ORDER BY isFeatured DESC, cachedAt DESC LIMIT :limit")
    fun latest(limit: Int = 8): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE isFeatured = 1 ORDER BY cachedAt DESC LIMIT :limit")
    fun featured(limit: Int = 6): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE id = :id")
    suspend fun byId(id: String): PropertyEntity?

    @Query("SELECT COUNT(*) FROM properties")
    suspend fun count(): Int

    @Query("DELETE FROM properties")
    suspend fun clear()
}

@Database(entities = [PropertyEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
}
