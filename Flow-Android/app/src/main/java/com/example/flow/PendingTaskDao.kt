package com.ahmedprojects.flow

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface PendingTaskDao {

    @Insert
    suspend fun insertTask(task: PendingTaskEntity)

    @Query("SELECT * FROM pending_tasks")
    suspend fun getAllTasks(): List<PendingTaskEntity>

    @Delete
    suspend fun deleteTask(task: PendingTaskEntity)

    @Query("DELETE FROM pending_tasks")
    suspend fun clearAll()
}
