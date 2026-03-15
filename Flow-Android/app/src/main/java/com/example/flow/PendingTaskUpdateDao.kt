package com.ahmedprojects.flow

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface PendingTaskUpdateDao {

    @Insert
    suspend fun insertUpdate(update: PendingTaskUpdateEntity)

    @Query("SELECT * FROM pending_task_updates")
    suspend fun getAllUpdates(): List<PendingTaskUpdateEntity>

    @Delete
    suspend fun deleteUpdate(update: PendingTaskUpdateEntity)
}
