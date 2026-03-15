package com.ahmedprojects.flow

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TaskCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskCacheEntity)

    @Query("SELECT * FROM task_cache WHERE taskId = :taskId LIMIT 1")
    suspend fun getTask(taskId: Int): TaskCacheEntity?
}