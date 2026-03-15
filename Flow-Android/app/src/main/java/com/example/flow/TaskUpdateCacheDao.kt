package com.ahmedprojects.flow

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TaskUpdateCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdates(updates: List<TaskUpdateCacheEntity>)

    @Query("SELECT * FROM task_update_cache WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getUpdates(taskId: Int): List<TaskUpdateCacheEntity>
}