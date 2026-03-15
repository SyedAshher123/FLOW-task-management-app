package com.ahmedprojects.flow

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_task_updates")
data class PendingTaskUpdateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val userId: Int,
    val message: String,
    val imageBase64: String
)
