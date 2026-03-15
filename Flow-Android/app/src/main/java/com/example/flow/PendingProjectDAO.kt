package com.ahmedprojects.flow

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingProjectDao {

    @Query("SELECT * FROM pending_projects")
    suspend fun getPending(): List<PendingProjectEntity>

    @Insert
    suspend fun insertPending(project: PendingProjectEntity)

    @Delete
    suspend fun deletePending(project: PendingProjectEntity)
}
