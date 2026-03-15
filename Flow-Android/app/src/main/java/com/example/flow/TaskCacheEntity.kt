package com.ahmedprojects.flow

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_cache")
data class TaskCacheEntity(
    @PrimaryKey val taskId: Int,
    val title: String,
    val description: String,
    val assignedBy: Int,
    val assignedTo: Int,
    val assignedByName: String,
    val assignedToName: String,
    val deadline: String,
    val priority: String,
    val status: String
)