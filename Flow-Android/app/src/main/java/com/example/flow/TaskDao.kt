package com.ahmedprojects.flow

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TaskDao {

    // Tasks assigned TO the user
    @Query("SELECT * FROM tasks WHERE assignedTo = :userId")
    suspend fun getTasksForUser(userId: Int): List<TaskEntity>

    // Tasks assigned BY the user
    @Query("SELECT * FROM tasks WHERE assignedBy = :userId")
    suspend fun getTasksByUser(userId: Int): List<TaskEntity>

    // Insert a single task
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    // Insert multiple tasks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    // Delete all tasks assigned TO a user
    @Query("DELETE FROM tasks WHERE assignedTo = :userId")
    suspend fun deleteTasksForUser(userId: Int)

    // Delete all tasks assigned BY a user
    @Query("DELETE FROM tasks WHERE assignedBy = :userId")
    suspend fun deleteTasksByUser(userId: Int)
}
